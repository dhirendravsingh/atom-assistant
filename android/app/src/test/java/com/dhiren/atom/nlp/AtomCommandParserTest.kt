package com.dhiren.atom.nlp

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomCommandParserTest {
    private val timezone = ZoneId.of("Asia/Kolkata")
    private val parser = AtomCommandParser(
        clock = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC),
        zoneProvider = { timezone },
    )
    private val context = ReminderContext(
        id = 42L,
        title = "Send the product brief",
        localDate = LocalDate.of(2026, 8, 2),
        localTime = LocalTime.of(18, 30),
        timezone = timezone,
        recurrenceRule = null,
    )

    @Test
    fun `creates an absolute reminder and strips a combined prefix`() {
        val result = parser.parse("Hey Atom, could you please remind me to call Rhea tomorrow at 6:30 PM")

        assertEquals(CommandIntent.Create, result.intent)
        assertEquals("Call Rhea", result.task)
        assertEquals(LocalDate.of(2026, 8, 2), result.localDate)
        assertEquals(LocalTime.of(18, 30), result.localTime)
        assertEquals(Instant.parse("2026-08-02T13:00:00Z"), result.scheduledAtUtc)
        assertTrue(result.isActionable)
    }

    @Test
    fun `parses schedule before the reminder phrase`() {
        val result = parser.parse("At 4:30 PM remind me to call home tomorrow")

        assertEquals("Call home", result.task)
        assertEquals(LocalTime.of(16, 30), result.localTime)
        assertTrue(result.isActionable)
    }

    @Test
    fun `resolves minutes relative to the injected clock`() {
        val result = parser.parse("Remind me in 20 minutes to check the oven")

        assertEquals("Check the oven", result.task)
        assertEquals("In 20 minutes", result.relativeLabel)
        assertEquals(LocalDate.of(2026, 8, 1), result.localDate)
        assertEquals(LocalTime.of(17, 50), result.localTime)
        assertEquals(Instant.parse("2026-08-01T12:20:00Z"), result.scheduledAtUtc)
        assertTrue(result.isActionable)
    }

    @Test
    fun `resolves hours days and weeks`() {
        assertEquals(Instant.parse("2026-08-01T14:00:00Z"), parser.parse("In 2 hours remind me to stretch").scheduledAtUtc)
        assertEquals(LocalDate.of(2026, 8, 4), parser.parse("In 3 days remind me to renew the pass").localDate)
        assertEquals(LocalDate.of(2026, 8, 15), parser.parse("In 2 weeks remind me to review goals").localDate)
    }

    @Test
    fun `snooze for a relative duration uses the current reminder`() {
        val result = parser.parse("Snooze this reminder for 20 minutes", context)

        assertEquals(CommandIntent.Snooze, result.intent)
        assertEquals(context.title, result.task)
        assertEquals(Instant.parse("2026-08-01T12:20:00Z"), result.scheduledAtUtc)
        assertTrue(result.isActionable)
    }

    @Test
    fun `understands noon and midnight`() {
        assertEquals(LocalTime.NOON, parser.parse("Remind me tomorrow at noon to have lunch").localTime)
        assertEquals(LocalTime.MIDNIGHT, parser.parse("Remind me tomorrow at midnight to deploy").localTime)
    }

    @Test
    fun `maps twelve am and twelve pm correctly`() {
        assertEquals(LocalTime.MIDNIGHT, parser.parse("Remind me tomorrow at 12 AM to deploy").localTime)
        assertEquals(LocalTime.NOON, parser.parse("Remind me tomorrow at 12 PM to have lunch").localTime)
    }

    @Test
    fun `numeric time without am or pm remains incomplete`() {
        val result = parser.parse("Remind me tomorrow at 6:30 to call home")

        assertTrue(MissingField.AmPm in result.missingFields)
        assertTrue(MissingField.Time in result.missingFields)
        assertFalse(result.isActionable)
    }

    @Test
    fun `rejects a 24 hour time`() {
        val result = parser.parse("Remind me tomorrow at 18:30 to call home")

        assertTrue(result.conflicts.any { "12-hour" in it })
        assertFalse(result.isActionable)
    }

    @Test
    fun `missing date asks only for date`() {
        val result = parser.parse("Remind me at 6 PM to call home")

        assertEquals(setOf(MissingField.Date), result.missingFields)
    }

    @Test
    fun `missing time asks only for time`() {
        val result = parser.parse("Remind me tomorrow to call home")

        assertEquals(setOf(MissingField.Time), result.missingFields)
    }

    @Test
    fun `missing date and time can be saved unscheduled`() {
        val result = parser.parse("Remind me to call home")

        assertEquals(setOf(MissingField.Date, MissingField.Time), result.missingFields)
        assertEquals("Call home", result.task)
    }

    @Test
    fun `parses day after tomorrow`() {
        val result = parser.parse("Remind me the day after tomorrow at 9 AM to pay rent")

        assertEquals(LocalDate.of(2026, 8, 3), result.localDate)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `parses next weekday`() {
        val result = parser.parse("Remind me next Monday at 9 AM to call the bank")

        assertEquals(LocalDate.of(2026, 8, 3), result.localDate)
    }

    @Test
    fun `parses explicit month and iso dates`() {
        assertEquals(LocalDate.of(2026, 8, 8), parser.parse("Remind me August 8 2026 at 9 AM to renew insurance").localDate)
        assertEquals(LocalDate.of(2026, 8, 9), parser.parse("Remind me 2026-08-09 at 9 AM to renew insurance").localDate)
    }

    @Test
    fun `creates daily recurrence without requiring a date`() {
        val result = parser.parse("Every day at 9 AM remind me to review priorities")

        assertEquals("FREQ=DAILY", result.recurrenceRule)
        assertEquals("Every day", result.recurrenceLabel)
        assertFalse(MissingField.Date in result.missingFields)
        assertTrue(result.isActionable)
    }

    @Test
    fun `creates weekday recurrence`() {
        val result = parser.parse("Every weekday at 9 AM remind me to review priorities")

        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result.recurrenceRule)
        assertEquals("Review priorities", result.task)
    }

    @Test
    fun `creates recurrence for a named weekday`() {
        val result = parser.parse("Every Monday at 8:15 AM remind me to submit timesheet")

        assertEquals("FREQ=WEEKLY;BYDAY=MO", result.recurrenceRule)
        assertEquals("Every Monday", result.recurrenceLabel)
        assertTrue(result.isActionable)
    }

    @Test
    fun `recurrence without time asks for time only`() {
        val result = parser.parse("Remind me every month to pay the card")

        assertEquals(setOf(MissingField.Time), result.missingFields)
        assertEquals("FREQ=MONTHLY", result.recurrenceRule)
    }

    @Test
    fun `reschedules while preserving title`() {
        val result = parser.parse("Hey Atom, move this to tomorrow at 12 PM", context)

        assertEquals(CommandIntent.Reschedule, result.intent)
        assertEquals(context.title, result.task)
        assertEquals(LocalTime.NOON, result.localTime)
        assertTrue(result.isActionable)
    }

    @Test
    fun `date only reschedule preserves existing time`() {
        val result = parser.parse("Move this to next Monday", context)

        assertEquals(LocalDate.of(2026, 8, 3), result.localDate)
        assertEquals(context.localTime, result.localTime)
        assertTrue(result.isActionable)
    }

    @Test
    fun `time only reschedule preserves existing date`() {
        val result = parser.parse("Change the time to 9:45 AM", context)

        assertEquals(context.localDate, result.localDate)
        assertEquals(LocalTime.of(9, 45), result.localTime)
        assertTrue(result.isActionable)
    }

    @Test
    fun `renames while preserving the schedule`() {
        val result = parser.parse("Change the title to send the final proposal", context)

        assertEquals(CommandIntent.Rename, result.intent)
        assertEquals("Send the final proposal", result.task)
        assertEquals(context.localDate, result.localDate)
        assertEquals(context.localTime, result.localTime)
        assertTrue(result.isActionable)
    }

    @Test
    fun `cancels a contextual reminder`() {
        val result = parser.parse("Cancel this reminder", context)

        assertEquals(CommandIntent.Cancel, result.intent)
        assertEquals(context.title, result.task)
        assertTrue(result.isActionable)
    }

    @Test
    fun `completes a contextual reminder`() {
        val result = parser.parse("Mark this reminder complete", context)

        assertEquals(CommandIntent.Complete, result.intent)
        assertTrue(result.isActionable)
    }

    @Test
    fun `action without reminder context is rejected`() {
        val result = parser.parse("Cancel this reminder")

        assertTrue(MissingField.Context in result.missingFields)
        assertFalse(result.isActionable)
    }

    @Test
    fun `remind again preserves task and creates a new schedule`() {
        val result = parser.parse("Remind me again at 12 PM", context)

        assertEquals(CommandIntent.RemindAgain, result.intent)
        assertEquals(context.title, result.task)
        assertEquals(context.localDate, result.localDate)
        assertEquals(LocalTime.NOON, result.localTime)
        assertTrue(result.isActionable)
    }

    @Test
    fun `adds recurrence during an edit`() {
        val result = parser.parse("Make this repeat every weekday at 9 AM", context)

        assertEquals(CommandIntent.Repeat, result.intent)
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result.recurrenceRule)
        assertTrue(result.isActionable)
    }

    @Test
    fun `clears recurrence during an edit`() {
        val recurringContext = context.copy(recurrenceRule = "FREQ=DAILY")
        val result = parser.parse("Stop repeating this", recurringContext)

        assertEquals(CommandIntent.Repeat, result.intent)
        assertEquals(RecurrenceUpdate.Clear, result.recurrence)
        assertNull(result.recurrenceRule)
        assertTrue(result.isActionable)
    }

    @Test
    fun `rejects conflicting dates`() {
        val result = parser.parse("Remind me today and tomorrow at 9 PM to call home")

        assertTrue(result.conflicts.any { "More than one date" in it })
        assertFalse(result.isActionable)
    }

    @Test
    fun `rejects conflicting times`() {
        val result = parser.parse("Remind me tomorrow at 9 AM or 10 AM to call home")

        assertTrue(result.conflicts.any { "More than one time" in it })
    }

    @Test
    fun `rejects relative and absolute schedules together`() {
        val result = parser.parse("Remind me in 20 minutes tomorrow at 9 PM to call home")

        assertTrue(result.conflicts.any { "relative schedule" in it })
    }

    @Test
    fun `rejects conflicting actions`() {
        val result = parser.parse("Cancel this reminder and mark this reminder complete", context)

        assertTrue(result.conflicts.any { "conflicting actions" in it })
    }

    @Test
    fun `rejects a past one off schedule`() {
        val result = parser.parse("Remind me today at 9 AM to call home")

        assertTrue(result.conflicts.any { "past" in it })
    }

    @Test
    fun `allows task words that resemble actions`() {
        val result = parser.parse("Remind me tomorrow at 9 AM to cancel my subscription")

        assertEquals(CommandIntent.Create, result.intent)
        assertEquals("Cancel my subscription", result.task)
    }
}
