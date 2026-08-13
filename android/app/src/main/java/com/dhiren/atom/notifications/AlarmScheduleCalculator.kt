package com.dhiren.atom.notifications

import com.dhiren.atom.data.local.ReminderEntity
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object AlarmScheduleCalculator {
    fun nextTrigger(reminder: ReminderEntity, now: Instant): Instant? {
        if (!reminder.state.equals("Scheduled", ignoreCase = true)) return null
        val stored = reminder.scheduledAtUtc?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (stored != null && stored.isAfter(now)) return stored

        val rule = reminder.recurrenceRule ?: return null
        rule.hourlyIntervalHours()?.let { intervalHours ->
            return nextHourly(reminder, stored, now, intervalHours)
        }
        val time = reminder.localTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val zone = runCatching { ZoneId.of(reminder.timezone) }.getOrNull() ?: return null
        val localNow = now.atZone(zone)
        return when {
            rule == "FREQ=DAILY" -> nextDaily(localNow, time)
            rule.startsWith("FREQ=WEEKLY;BYDAY=") -> nextNamedWeekday(localNow, time, rule.substringAfter("BYDAY="))
            rule == "FREQ=WEEKLY" -> nextWeekly(reminder, localNow, time, zone)
            rule == "FREQ=MONTHLY" -> nextMonthly(reminder, localNow, time, zone)
            else -> null
        }?.toInstant()
    }

    private fun nextHourly(
        reminder: ReminderEntity,
        stored: Instant?,
        now: Instant,
        intervalHours: Long,
    ): Instant? {
        val zone = runCatching { ZoneId.of(reminder.timezone) }.getOrNull() ?: return null
        val localAnchor = reminder.localDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { date ->
                reminder.localTime
                    ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    ?.let { time -> ZonedDateTime.of(date, time, zone).toInstant() }
            }
        val createdAnchor = runCatching { Instant.parse(reminder.createdAtUtc) }
            .getOrNull()
            ?.plus(Duration.ofHours(intervalHours))
        val anchor = stored ?: localAnchor ?: createdAnchor ?: return null
        if (anchor.isAfter(now)) return anchor

        val intervalSeconds = Duration.ofHours(intervalHours).seconds
        val elapsedSeconds = Duration.between(anchor, now).seconds.coerceAtLeast(0)
        val intervalsToAdvance = (elapsedSeconds / intervalSeconds) + 1
        return anchor.plusSeconds(intervalSeconds * intervalsToAdvance)
    }

    private fun nextDaily(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun nextNamedWeekday(
        now: ZonedDateTime,
        time: LocalTime,
        byDay: String,
    ): ZonedDateTime? {
        val days = byDay.split(',').mapNotNull(::rruleDay)
        return (0L..7L)
            .map { offset -> ZonedDateTime.of(now.toLocalDate().plusDays(offset), time, now.zone) }
            .firstOrNull { candidate -> candidate.dayOfWeek in days && candidate.isAfter(now) }
    }

    private fun nextWeekly(
        reminder: ReminderEntity,
        now: ZonedDateTime,
        time: LocalTime,
        zone: ZoneId,
    ): ZonedDateTime {
        val anchorDay = reminder.anchorDate(zone).dayOfWeek
        val date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(anchorDay))
        val candidate = ZonedDateTime.of(date, time, zone)
        return if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
    }

    private fun nextMonthly(
        reminder: ReminderEntity,
        now: ZonedDateTime,
        time: LocalTime,
        zone: ZoneId,
    ): ZonedDateTime {
        val anchorDay = reminder.anchorDate(zone).dayOfMonth
        fun candidate(month: YearMonth): ZonedDateTime = ZonedDateTime.of(
            month.atDay(anchorDay.coerceAtMost(month.lengthOfMonth())),
            time,
            zone,
        )
        val thisMonth = candidate(YearMonth.from(now))
        return if (thisMonth.isAfter(now)) thisMonth else candidate(YearMonth.from(now).plusMonths(1))
    }

    private fun ReminderEntity.anchorDate(zone: ZoneId): LocalDate = localDate
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: runCatching { Instant.parse(createdAtUtc).atZone(zone).toLocalDate() }.getOrDefault(LocalDate.now(zone))

    private fun rruleDay(code: String): DayOfWeek? = when (code) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
    }
}

internal fun isElapsedIntervalRecurrence(rule: String?): Boolean =
    rule.hourlyIntervalHours() != null

private fun String?.hourlyIntervalHours(): Long? =
    Regex("FREQ=HOURLY;INTERVAL=(\\d+)", RegexOption.IGNORE_CASE)
        .matchEntire(this.orEmpty())
        ?.groupValues
        ?.get(1)
        ?.toLongOrNull()
        ?.takeIf { it in 1..24 }
