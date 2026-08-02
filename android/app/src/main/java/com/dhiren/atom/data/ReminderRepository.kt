package com.dhiren.atom.data

import com.dhiren.atom.data.local.ReminderDao
import com.dhiren.atom.data.local.ReminderEntity
import com.dhiren.atom.notifications.AlarmScheduleCalculator
import com.dhiren.atom.notifications.NoOpReminderAlarmScheduler
import com.dhiren.atom.notifications.ReminderAlarmScheduler
import com.dhiren.atom.ui.ReminderAccent
import com.dhiren.atom.ui.ReminderState
import com.dhiren.atom.ui.ReminderUi
import java.time.Clock
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val alarmScheduler: ReminderAlarmScheduler = NoOpReminderAlarmScheduler,
) {
    val reminders: Flow<List<ReminderUi>> = reminderDao.observeAll().map { entities ->
        val zone = zoneProvider()
        entities.map { it.toUi(clock, zone) }
    }

    suspend fun save(reminder: ReminderUi) {
        val existing = reminder.id.takeIf { it != 0L }?.let { reminderDao.getById(it) }
        val saveZone = runCatching { ZoneId.of(reminder.timezone) }.getOrNull()
            ?: existing?.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: zoneProvider()
        val entity = reminder.toEntity(
            existing = existing,
            clock = clock,
            zone = saveZone,
        )
        if (existing != null) alarmScheduler.cancel(existing.id)
        val rowId = reminderDao.upsert(entity)
        val persisted = entity.copy(id = entity.id.takeIf { it != 0L } ?: rowId)
        scheduleIfEligible(persisted)
    }

    suspend fun delete(id: Long) {
        alarmScheduler.cancel(id)
        reminderDao.deleteById(id)
    }

    suspend fun complete(id: Long) {
        val existing = reminderDao.getById(id) ?: return
        if (existing.recurrenceRule != null) return
        alarmScheduler.cancel(id)
        reminderDao.upsert(
            existing.copy(
                scheduledAtUtc = null,
                state = ReminderState.Completed.name,
                updatedAtUtc = Instant.now(clock).toString(),
            ),
        )
    }

    suspend fun snooze(id: Long, duration: Duration) {
        require(!duration.isNegative && !duration.isZero) { "Snooze duration must be positive" }
        val existing = reminderDao.getById(id) ?: return
        val trigger = Instant.now(clock).plus(duration)
        val zone = runCatching { ZoneId.of(existing.timezone) }.getOrDefault(zoneProvider())
        val localTrigger = trigger.atZone(zone)
        val updated = existing.copy(
            scheduledAtUtc = trigger.toString(),
            localDate = localTrigger.toLocalDate().toString(),
            localTime = localTrigger.toLocalTime().withSecond(0).withNano(0).format(DatabaseTimeFormatter),
            state = ReminderState.Scheduled.name,
            updatedAtUtc = Instant.now(clock).toString(),
        )
        alarmScheduler.cancel(id)
        reminderDao.upsert(updated)
        alarmScheduler.schedule(id, updated.title, trigger)
    }

    suspend fun remindAgain(id: Long, delay: Duration): Long? {
        require(!delay.isNegative && !delay.isZero) { "Remind-again delay must be positive" }
        val existing = reminderDao.getById(id) ?: return null
        val trigger = Instant.now(clock).plus(delay)
        val zone = runCatching { ZoneId.of(existing.timezone) }.getOrDefault(zoneProvider())
        val localTrigger = trigger.atZone(zone)
        val duplicate = existing.copy(
            id = 0L,
            scheduledAtUtc = trigger.toString(),
            localDate = localTrigger.toLocalDate().toString(),
            localTime = localTrigger.toLocalTime().withSecond(0).withNano(0).format(DatabaseTimeFormatter),
            recurrenceRule = null,
            state = ReminderState.Scheduled.name,
            createdAtUtc = Instant.now(clock).toString(),
            updatedAtUtc = Instant.now(clock).toString(),
        )
        val newId = reminderDao.upsert(duplicate)
        alarmScheduler.schedule(newId, duplicate.title, trigger)
        if (existing.recurrenceRule == null) complete(id)
        return newId
    }

    suspend fun advanceRecurringAfterDelivery(id: Long) {
        val existing = reminderDao.getById(id) ?: return
        if (existing.recurrenceRule == null) return
        val now = Instant.now(clock)
        val nextTrigger = AlarmScheduleCalculator.nextTrigger(
            existing.copy(scheduledAtUtc = null),
            now.plusSeconds(1),
        ) ?: return
        val zone = runCatching { ZoneId.of(existing.timezone) }.getOrDefault(zoneProvider())
        val localTrigger = nextTrigger.atZone(zone)
        val updated = existing.copy(
            scheduledAtUtc = nextTrigger.toString(),
            localDate = localTrigger.toLocalDate().toString(),
            localTime = localTrigger.toLocalTime().withSecond(0).withNano(0).format(DatabaseTimeFormatter),
            updatedAtUtc = now.toString(),
        )
        reminderDao.upsert(updated)
        alarmScheduler.schedule(id, updated.title, nextTrigger)
    }

    suspend fun reconcileAlarms() {
        reminderDao.getScheduled().forEach { reminder ->
            alarmScheduler.cancel(reminder.id)
            scheduleIfEligible(reminder)
        }
    }

    private fun scheduleIfEligible(reminder: ReminderEntity) {
        AlarmScheduleCalculator.nextTrigger(reminder, Instant.now(clock))?.let { trigger ->
            alarmScheduler.schedule(reminder.id, reminder.title, trigger)
        }
    }
}

internal fun ReminderUi.toEntity(
    existing: ReminderEntity? = null,
    clock: Clock = Clock.systemUTC(),
    zone: ZoneId = ZoneId.systemDefault(),
): ReminderEntity {
    val now = Instant.now(clock)
    val relativeSchedule = date?.resolveRelative(now, zone)
    val normalizedDate = relativeSchedule?.toLocalDate()
        ?: localDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: date.toLocalDate(now, zone)
    val normalizedTime = relativeSchedule?.toLocalTime()?.withSecond(0)?.withNano(0)
        ?: localTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        ?: time.toLocalTime()
    val normalizedRecurrenceRule = recurrenceRule ?: recurrence.toRecurrenceRule()
    val scheduledAtUtc = when {
        state != ReminderState.Scheduled -> null
        relativeSchedule != null -> relativeSchedule.toInstant().toString()
        normalizedDate != null && normalizedTime != null ->
            ZonedDateTime.of(normalizedDate, normalizedTime, zone).toInstant().toString()
        else -> null
    }
    val nowText = now.toString()

    return ReminderEntity(
        id = id,
        title = title.trim(),
        sourceText = sourceText.ifBlank { title.trim() },
        scheduledAtUtc = scheduledAtUtc,
        localDate = normalizedDate?.toString(),
        localTime = normalizedTime?.format(DatabaseTimeFormatter),
        timezone = zone.id,
        recurrenceRule = normalizedRecurrenceRule,
        source = source,
        state = state.name,
        accent = accent.name,
        createdAtUtc = existing?.createdAtUtc ?: nowText,
        updatedAtUtc = nowText,
    )
}

internal fun ReminderEntity.toUi(
    clock: Clock = Clock.systemUTC(),
    displayZone: ZoneId = ZoneId.systemDefault(),
): ReminderUi {
    val dateValue = localDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val timeValue = localTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    return ReminderUi(
        id = id,
        title = title,
        date = recurrenceRule.toRecurrenceLabel() ?: dateValue?.toDisplayLabel(LocalDate.now(clock.withZone(displayZone))),
        time = timeValue?.format(DisplayTimeFormatter),
        source = source,
        state = ReminderState.entries.firstOrNull { it.name.equals(state, ignoreCase = true) }
            ?: ReminderState.Unscheduled,
        accent = ReminderAccent.entries.firstOrNull { it.name.equals(accent, ignoreCase = true) }
            ?: ReminderAccent.Mint,
        recurrence = recurrenceRule.toRecurrenceLabel(),
        sourceText = sourceText,
        scheduledAtUtc = scheduledAtUtc,
        localDate = localDate,
        localTime = localTime,
        timezone = timezone,
        recurrenceRule = recurrenceRule,
    )
}

private val DatabaseTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val DisplayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val SpokenTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("h:mm a")
    .toFormatter(Locale.US)

private fun String?.toLocalTime(): LocalTime? {
    if (this.isNullOrBlank()) return null
    val normalized = trim().uppercase(Locale.US).replace(Regex("\\s+"), " ")
    return runCatching { LocalTime.parse(normalized, SpokenTimeFormatter) }
        .recoverCatching { LocalTime.parse(normalized, DatabaseTimeFormatter) }
        .getOrNull()
}

private fun String?.toLocalDate(now: Instant, zone: ZoneId): LocalDate? {
    if (this.isNullOrBlank() || startsWith("Every", ignoreCase = true)) return null
    val today = now.atZone(zone).toLocalDate()
    val normalized = substringBefore("·").trim()
    when {
        normalized.equals("today", ignoreCase = true) -> return today
        normalized.equals("tomorrow", ignoreCase = true) -> return today.plusDays(1)
    }

    DayOfWeek.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }?.let { weekday ->
        return today.with(TemporalAdjusters.nextOrSame(weekday))
    }

    val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        monthDayFormatter("MMMM d", today.year),
        monthDayFormatter("MMM d", today.year),
        DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US),
        monthDayFormatter("M/d", today.year),
    )
    return formatters.firstNotNullOfOrNull { formatter ->
        try {
            LocalDate.parse(normalized, formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private fun monthDayFormatter(pattern: String, year: Int): DateTimeFormatter =
    DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern(pattern)
        .parseDefaulting(ChronoField.YEAR, year.toLong())
        .toFormatter(Locale.US)

private fun String.resolveRelative(now: Instant, zone: ZoneId): ZonedDateTime? {
    val match = Regex(
        "^in\\s+(\\d+)\\s+(minute|minutes|hour|hours|day|days)$",
        RegexOption.IGNORE_CASE,
    ).matchEntire(trim()) ?: return null
    val amount = match.groupValues[1].toLong()
    val base = now.atZone(zone)
    return when (match.groupValues[2].lowercase(Locale.US)) {
        "minute", "minutes" -> base.plusMinutes(amount)
        "hour", "hours" -> base.plusHours(amount)
        "day", "days" -> base.plusDays(amount)
        else -> null
    }
}

private fun String?.toRecurrenceRule(): String? = when (this?.trim()?.lowercase(Locale.US)) {
    "every day", "daily" -> "FREQ=DAILY"
    "every weekday", "weekdays" -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
    "every week", "weekly" -> "FREQ=WEEKLY"
    "every month", "monthly" -> "FREQ=MONTHLY"
    else -> this?.takeIf { it.startsWith("FREQ=", ignoreCase = true) }
}

private fun String?.toRecurrenceLabel(): String? = when (this) {
    "FREQ=DAILY" -> "Every day"
    "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" -> "Every weekday"
    "FREQ=WEEKLY" -> "Every week"
    "FREQ=MONTHLY" -> "Every month"
    else -> DayOfWeek.entries.firstNotNullOfOrNull { day ->
        if (this == "FREQ=WEEKLY;BYDAY=${day.name.take(2)}") {
            "Every ${day.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}"
        } else {
            null
        }
    }
}

private fun LocalDate.toDisplayLabel(today: LocalDate): String {
    val calendarLabel = format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    return when (this) {
        today -> "Today · $calendarLabel"
        today.plusDays(1) -> "Tomorrow · $calendarLabel"
        else -> "${format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))} · $calendarLabel"
    }
}
