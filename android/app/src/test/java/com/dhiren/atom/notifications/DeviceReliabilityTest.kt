package com.dhiren.atom.notifications

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceReliabilityTest {
    private val now = Instant.parse("2026-08-03T12:00:00Z")

    @Test
    fun `missed reminders inside the relevance window notify`() {
        assertTrue(
            MissedReminderPolicy.shouldNotify(
                scheduledAt = Instant.parse("2026-08-02T12:00:00Z"),
                now = now,
            ),
        )
    }

    @Test
    fun `old missed reminders do not create a late noisy alert`() {
        assertFalse(
            MissedReminderPolicy.shouldNotify(
                scheduledAt = Instant.parse("2026-08-02T11:59:59Z"),
                now = now,
            ),
        )
    }

    @Test
    fun `a future reminder is not classified as missed`() {
        assertFalse(
            MissedReminderPolicy.shouldNotify(
                scheduledAt = Instant.parse("2026-08-03T12:01:00Z"),
                now = now,
            ),
        )
    }

    @Test
    fun `health snapshot lists only missing delivery requirements`() {
        val snapshot = DeviceReliabilitySnapshot(
            notificationPermissionGranted = false,
            exactAlarmAccessGranted = false,
            fullScreenAccessGranted = true,
            batteryOptimizationExempt = false,
            lastSuccessfulReconciliation = null,
            lastReconciliationReason = null,
            lastScheduledAlarmCount = 0,
            lastMissedReminderCount = 0,
        )

        assertEquals(
            listOf("notification permission", "exact alarm access"),
            snapshot.missingAlarmRequirements(alarmModeEnabled = true),
        )
    }

    @Test
    fun `full screen access is required only while alarm mode is enabled`() {
        val snapshot = DeviceReliabilitySnapshot(
            notificationPermissionGranted = true,
            exactAlarmAccessGranted = true,
            fullScreenAccessGranted = false,
            batteryOptimizationExempt = true,
            lastSuccessfulReconciliation = now,
            lastReconciliationReason = AlarmReconciliationReason.AppStart,
            lastScheduledAlarmCount = 2,
            lastMissedReminderCount = 0,
        )

        assertEquals(listOf("full-screen access"), snapshot.missingAlarmRequirements(alarmModeEnabled = true))
        assertTrue(snapshot.missingAlarmRequirements(alarmModeEnabled = false).isEmpty())
    }

    @Test
    fun `pending missed occurrence codec preserves unicode titles and identity`() {
        val occurrence = MissedReminderOccurrence(
            reminderId = 91L,
            title = "Call माँ about the tickets | urgent",
            scheduledAt = Instant.parse("2026-08-03T11:45:00Z"),
            recurring = true,
        )

        assertEquals(
            occurrence,
            MissedReminderOccurrenceCodec.decode(MissedReminderOccurrenceCodec.encode(occurrence)),
        )
    }
}
