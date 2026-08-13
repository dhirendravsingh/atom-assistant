package com.dhiren.atom.notifications

import com.dhiren.atom.data.local.ReminderEntity
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmScheduleCalculatorTest {
    private val now = Instant.parse("2026-08-01T12:00:00Z")

    @Test
    fun `uses a future one off instant`() {
        val reminder = reminder(scheduledAtUtc = "2026-08-01T12:20:00Z")

        assertEquals(Instant.parse("2026-08-01T12:20:00Z"), AlarmScheduleCalculator.nextTrigger(reminder, now))
    }

    @Test
    fun `does not reschedule a past one off reminder`() {
        val reminder = reminder(scheduledAtUtc = "2026-08-01T11:59:00Z")

        assertNull(AlarmScheduleCalculator.nextTrigger(reminder, now))
    }

    @Test
    fun `calculates the next daily occurrence in local time`() {
        val reminder = reminder(
            scheduledAtUtc = null,
            localTime = "09:00",
            recurrenceRule = "FREQ=DAILY",
        )

        assertEquals(Instant.parse("2026-08-02T03:30:00Z"), AlarmScheduleCalculator.nextTrigger(reminder, now))
    }

    @Test
    fun `weekday recurrence skips the weekend`() {
        val reminder = reminder(
            scheduledAtUtc = null,
            localTime = "09:00",
            recurrenceRule = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
        )

        assertEquals(Instant.parse("2026-08-03T03:30:00Z"), AlarmScheduleCalculator.nextTrigger(reminder, now))
    }

    @Test
    fun `named weekday recurrence uses its rrule day`() {
        val reminder = reminder(
            scheduledAtUtc = null,
            localTime = "08:15",
            recurrenceRule = "FREQ=WEEKLY;BYDAY=MO",
        )

        assertEquals(Instant.parse("2026-08-03T02:45:00Z"), AlarmScheduleCalculator.nextTrigger(reminder, now))
    }

    @Test
    fun `monthly recurrence clamps to the final day`() {
        val reminder = reminder(
            scheduledAtUtc = null,
            localDate = "2026-01-31",
            localTime = "09:00",
            recurrenceRule = "FREQ=MONTHLY",
        )
        val februaryNow = Instant.parse("2026-02-01T00:00:00Z")

        assertEquals(Instant.parse("2026-02-28T03:30:00Z"), AlarmScheduleCalculator.nextTrigger(reminder, februaryNow))
    }

    @Test
    fun `completed reminders never produce an alarm`() {
        val reminder = reminder(
            scheduledAtUtc = "2026-08-01T12:20:00Z",
            state = "Completed",
        )

        assertNull(AlarmScheduleCalculator.nextTrigger(reminder, now))
    }

    @Test
    fun `daily recurrence keeps its wall time across spring daylight saving`() {
        val beforeDst = Instant.parse("2026-03-07T15:00:00Z")
        val reminder = reminder(
            scheduledAtUtc = null,
            localDate = "2026-03-07",
            localTime = "09:00",
            recurrenceRule = "FREQ=DAILY",
            timezone = "America/New_York",
        )

        assertEquals(
            Instant.parse("2026-03-08T13:00:00Z"),
            AlarmScheduleCalculator.nextTrigger(reminder, beforeDst),
        )
    }

    @Test
    fun `hourly interval advances from the stored occurrence without drift`() {
        val reminder = reminder(
            scheduledAtUtc = "2026-08-01T12:00:00Z",
            localDate = "2026-08-01",
            localTime = "17:30",
            recurrenceRule = "FREQ=HOURLY;INTERVAL=2",
        )

        assertEquals(
            Instant.parse("2026-08-01T14:00:00Z"),
            AlarmScheduleCalculator.nextTrigger(reminder, now),
        )
    }

    @Test
    fun `hourly interval skips elapsed occurrences after downtime`() {
        val reminder = reminder(
            scheduledAtUtc = "2026-08-01T09:00:00Z",
            localDate = "2026-08-01",
            localTime = "14:30",
            recurrenceRule = "FREQ=HOURLY;INTERVAL=2",
        )
        val restoredAt = Instant.parse("2026-08-01T12:10:00Z")

        assertEquals(
            Instant.parse("2026-08-01T13:00:00Z"),
            AlarmScheduleCalculator.nextTrigger(reminder, restoredAt),
        )
    }

    private fun reminder(
        scheduledAtUtc: String?,
        localDate: String? = "2026-08-01",
        localTime: String? = "17:30",
        recurrenceRule: String? = null,
        state: String = "Scheduled",
        timezone: String = "Asia/Kolkata",
    ) = ReminderEntity(
        id = 8L,
        title = "Review priorities",
        sourceText = "Remind me to review priorities",
        scheduledAtUtc = scheduledAtUtc,
        localDate = localDate,
        localTime = localTime,
        timezone = timezone,
        recurrenceRule = recurrenceRule,
        source = "Text",
        state = state,
        accent = "Mint",
        createdAtUtc = "2026-01-31T03:30:00Z",
        updatedAtUtc = "2026-08-01T12:00:00Z",
    )
}
