package com.dhiren.atom.data

import com.dhiren.atom.ui.ReminderAccent
import com.dhiren.atom.ui.ReminderState
import com.dhiren.atom.ui.ReminderUi
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderRepositoryTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-08-01T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val timezone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `absolute local schedule is persisted with utc instant`() {
        val entity = reminder(
            date = "Tomorrow",
            time = "12:00 AM",
        ).toEntity(clock = clock, zone = timezone)

        assertEquals("2026-08-02", entity.localDate)
        assertEquals("00:00", entity.localTime)
        assertEquals("2026-08-01T18:30:00Z", entity.scheduledAtUtc)
        assertEquals("Asia/Kolkata", entity.timezone)
    }

    @Test
    fun `noon is persisted as twelve pm rather than midnight`() {
        val entity = reminder(
            date = "Tomorrow",
            time = "12:00 PM",
        ).toEntity(clock = clock, zone = timezone)

        assertEquals("12:00", entity.localTime)
        assertEquals("2026-08-02T06:30:00Z", entity.scheduledAtUtc)
    }

    @Test
    fun `spring daylight saving gap resolves to the next valid instant`() {
        val newYork = ZoneId.of("America/New_York")
        val entity = reminder(
            date = null,
            time = null,
        ).copy(
            localDate = "2026-03-08",
            localTime = "02:30",
            timezone = newYork.id,
        ).toEntity(clock = clock, zone = newYork)

        assertEquals("2026-03-08T07:30:00Z", entity.scheduledAtUtc)
    }

    @Test
    fun `autumn daylight saving overlap chooses the earlier occurrence`() {
        val newYork = ZoneId.of("America/New_York")
        val entity = reminder(
            date = null,
            time = null,
        ).copy(
            localDate = "2026-11-01",
            localTime = "01:30",
            timezone = newYork.id,
        ).toEntity(clock = clock, zone = newYork)

        assertEquals("2026-11-01T05:30:00Z", entity.scheduledAtUtc)
    }

    @Test
    fun `relative schedule is resolved when owner confirms`() {
        val entity = reminder(
            date = "In 20 minutes",
            time = null,
        ).toEntity(clock = clock, zone = timezone)

        assertEquals("2026-08-01", entity.localDate)
        assertEquals("17:50", entity.localTime)
        assertEquals("2026-08-01T12:20:00Z", entity.scheduledAtUtc)
    }

    @Test
    fun `recurrence is stored as rrule body`() {
        val entity = reminder(
            date = "Every weekday",
            time = "9:00 AM",
            recurrence = "Every weekday",
        ).toEntity(clock = clock, zone = timezone)

        assertNull(entity.localDate)
        assertEquals("09:00", entity.localTime)
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", entity.recurrenceRule)
        assertEquals("Every weekday", entity.toUi(clock, timezone).recurrence)
    }

    private fun reminder(
        date: String?,
        time: String?,
        recurrence: String? = null,
    ) = ReminderUi(
        id = 0L,
        title = "Review priorities",
        date = date,
        time = time,
        source = "Text",
        state = ReminderState.Scheduled,
        accent = ReminderAccent.Mint,
        recurrence = recurrence,
        sourceText = "Remind me to review priorities",
    )
}
