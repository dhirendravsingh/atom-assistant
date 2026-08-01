package com.dhiren.atom.nlp

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class AtomCommandParser(
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) {
    fun parse(
        input: String,
        context: ReminderContext? = null,
        timezone: ZoneId = context?.timezone ?: zoneProvider(),
    ): ParsedCommand {
        val source = input.trim().replace(Regex("\\s+"), " ")
        val normalized = normalize(source)
        val now = Instant.now(clock)
        val today = now.atZone(timezone).toLocalDate()
        val conflicts = mutableListOf<String>()

        val intents = detectIntents(normalized)
        val detectedIntent = intents.singleOrNull() ?: intents.firstOrNull() ?: CommandIntent.Create
        if (intents.size > 1) {
            conflicts += "This command contains conflicting actions: ${intents.joinToString { it.displayName() }}."
        }

        val recurrence = parseRecurrence(normalized, conflicts)
        val intent = if (detectedIntent == CommandIntent.Create && context != null) {
            if (recurrence !is RecurrenceUpdate.Unchanged) CommandIntent.Repeat else CommandIntent.Reschedule
        } else {
            detectedIntent
        }
        val relativeCandidates = RelativePattern.findAll(normalized).mapNotNull { match ->
            val amount = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            if (amount <= 0L) return@mapNotNull null
            val unit = match.groupValues[2].lowercase(Locale.US)
            val resolved = when {
                unit.startsWith("minute") -> now.atZone(timezone).plusMinutes(amount)
                unit.startsWith("hour") -> now.atZone(timezone).plusHours(amount)
                unit.startsWith("day") -> now.atZone(timezone).plusDays(amount)
                unit.startsWith("week") -> now.atZone(timezone).plusWeeks(amount)
                else -> return@mapNotNull null
            }
            RelativeCandidate(match.value, resolved.withSecond(0).withNano(0))
        }.toList()
        val distinctRelative = relativeCandidates.distinctBy { it.value.toInstant() }
        if (distinctRelative.size > 1) {
            conflicts += "More than one relative schedule was found."
        }

        val timeResult = parseTimes(normalized, conflicts)
        val dateCandidates = if (recurrence is RecurrenceUpdate.Set) {
            parseDates(normalized, today, includeWeekdays = false)
        } else {
            parseDates(normalized, today, includeWeekdays = true)
        }
        val distinctDates = dateCandidates.distinctBy { it.value }
        if (distinctDates.size > 1) {
            conflicts += "More than one date was found: ${distinctDates.joinToString { it.raw }}."
        }

        val relative = distinctRelative.singleOrNull()
        if (relative != null && (distinctDates.isNotEmpty() || timeResult.times.isNotEmpty())) {
            conflicts += "A relative schedule cannot be combined with another date or time."
        }
        if (relative != null && recurrence is RecurrenceUpdate.Set) {
            conflicts += "A relative schedule cannot be combined with recurrence."
        }
        if (recurrence is RecurrenceUpdate.Set && distinctDates.isNotEmpty()) {
            conflicts += "A recurring reminder cannot also use a one-off date."
        }

        val dateWasSpecified = relative != null || distinctDates.isNotEmpty()
        val timeWasSpecified = relative != null || timeResult.times.isNotEmpty()
        val scheduleWasSpecified = dateWasSpecified || timeWasSpecified || recurrence !is RecurrenceUpdate.Unchanged

        val explicitDate = relative?.value?.toLocalDate() ?: distinctDates.singleOrNull()?.value
        val explicitTime = relative?.value?.toLocalTime() ?: timeResult.times.singleOrNull()?.value
        val finalDate = when {
            relative != null -> explicitDate
            dateWasSpecified -> explicitDate
            context != null -> context.localDate
            else -> null
        }
        val finalTime = when {
            relative != null -> explicitTime
            timeWasSpecified -> explicitTime
            context != null -> context.localTime
            else -> null
        }
        val finalRecurrenceRule = when (recurrence) {
            RecurrenceUpdate.Clear -> null
            is RecurrenceUpdate.Set -> recurrence.rule
            RecurrenceUpdate.Unchanged -> context?.recurrenceRule
        }
        val finalRecurrenceLabel = recurrenceLabel(finalRecurrenceRule)

        val task = extractTask(normalized, intent, context)
        val missing = linkedSetOf<MissingField>()
        if (intent.requiresContext() && context == null) missing += MissingField.Context

        when (intent) {
            CommandIntent.Create -> {
                if (task.isNullOrBlank()) missing += MissingField.Task
                if (finalRecurrenceRule != null) {
                    if (finalTime == null) missing += MissingField.Time
                } else {
                    if (finalDate == null) missing += MissingField.Date
                    if (finalTime == null) missing += MissingField.Time
                }
            }

            CommandIntent.Reschedule -> {
                if (!scheduleWasSpecified) conflicts += "Say the new date, time, or recurrence."
                if (finalRecurrenceRule != null) {
                    if (finalTime == null) missing += MissingField.Time
                } else {
                    if (finalDate == null) missing += MissingField.Date
                    if (finalTime == null) missing += MissingField.Time
                }
            }

            CommandIntent.Rename -> if (task.isNullOrBlank() || task == context?.title) {
                missing += MissingField.Task
            }

            CommandIntent.Snooze,
            CommandIntent.RemindAgain -> {
                if (!scheduleWasSpecified) conflicts += "Say when Atom should remind you again."
                if (finalDate == null) missing += MissingField.Date
                if (finalTime == null) missing += MissingField.Time
            }

            CommandIntent.Repeat -> {
                if (recurrence is RecurrenceUpdate.Unchanged) {
                    conflicts += "Say how this reminder should repeat, or say stop repeating."
                }
                if (recurrence is RecurrenceUpdate.Set && finalTime == null) missing += MissingField.Time
            }

            CommandIntent.Cancel,
            CommandIntent.Complete -> Unit
        }

        if (timeResult.requiresAmPm) missing += MissingField.AmPm

        val scheduled = if (finalDate != null && finalTime != null) {
            ZonedDateTime.of(finalDate, finalTime, timezone).toInstant()
        } else {
            null
        }
        val mutatesSchedule = intent in setOf(
            CommandIntent.Create,
            CommandIntent.Reschedule,
            CommandIntent.Snooze,
            CommandIntent.RemindAgain,
        )
        if (
            mutatesSchedule &&
            finalRecurrenceRule == null &&
            scheduled != null &&
            !scheduled.isAfter(now) &&
            conflicts.none { it.contains("relative", ignoreCase = true) }
        ) {
            conflicts += "The requested one-off schedule is in the past."
        }

        val distinctConflicts = conflicts.distinct()
        val confidence = when {
            distinctConflicts.isNotEmpty() -> 0.35
            missing.isNotEmpty() -> 0.72
            else -> 0.98
        }
        return ParsedCommand(
            intent = intent,
            task = task,
            localDate = finalDate,
            localTime = finalTime,
            scheduledAtUtc = scheduled,
            timezone = timezone,
            recurrence = recurrence,
            recurrenceRule = finalRecurrenceRule,
            recurrenceLabel = finalRecurrenceLabel,
            sourceText = source,
            relativeLabel = relative?.raw?.replaceFirstChar { it.uppercase() },
            dateWasSpecified = dateWasSpecified,
            timeWasSpecified = timeWasSpecified,
            scheduleWasSpecified = scheduleWasSpecified,
            missingFields = missing,
            conflicts = distinctConflicts,
            confidence = confidence,
        )
    }

    private fun detectIntents(text: String): List<CommandIntent> {
        val found = linkedSetOf<CommandIntent>()
        if (CancelPattern.containsMatchIn(text)) found += CommandIntent.Cancel
        if (CompletePattern.containsMatchIn(text)) found += CommandIntent.Complete
        if (SnoozePattern.containsMatchIn(text)) found += CommandIntent.Snooze
        if (RemindAgainPattern.containsMatchIn(text)) found += CommandIntent.RemindAgain
        if (RenamePattern.containsMatchIn(text)) found += CommandIntent.Rename
        if (ReschedulePattern.containsMatchIn(text)) found += CommandIntent.Reschedule
        if (RepeatActionPattern.containsMatchIn(text)) found += CommandIntent.Repeat
        return found.toList().ifEmpty { listOf(CommandIntent.Create) }
    }

    private fun parseRecurrence(text: String, conflicts: MutableList<String>): RecurrenceUpdate {
        val clears = ClearRecurrencePattern.containsMatchIn(text)
        val candidates = mutableListOf<Pair<String, String>>()
        if (EveryWeekdayPattern.containsMatchIn(text)) {
            candidates += "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" to "Every weekday"
        }
        EveryWeekdayNamePattern.findAll(text).forEach { match ->
            val day = parseDayOfWeek(match.groupValues[1]) ?: return@forEach
            candidates += "FREQ=WEEKLY;BYDAY=${day.rruleCode()}" to "Every ${day.displayName()}"
        }
        if (DailyPattern.containsMatchIn(text)) candidates += "FREQ=DAILY" to "Every day"
        if (WeeklyPattern.containsMatchIn(text)) candidates += "FREQ=WEEKLY" to "Every week"
        if (MonthlyPattern.containsMatchIn(text)) candidates += "FREQ=MONTHLY" to "Every month"

        val distinct = candidates.distinctBy { it.first }
        if (clears && distinct.isNotEmpty()) {
            conflicts += "The command both adds and removes recurrence."
        }
        if (distinct.size > 1) {
            conflicts += "More than one recurrence pattern was found."
        }
        return when {
            clears && distinct.isEmpty() -> RecurrenceUpdate.Clear
            distinct.size == 1 && !clears -> RecurrenceUpdate.Set(distinct[0].first, distinct[0].second)
            else -> RecurrenceUpdate.Unchanged
        }
    }

    private fun parseTimes(text: String, conflicts: MutableList<String>): TimeResult {
        val candidates = mutableListOf<TimeCandidate>()
        SpokenTimePattern.findAll(text).forEach { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].ifBlank { "0" }.toInt()
            val meridiem = match.groupValues[3].lowercase(Locale.US)
            val hour24 = when {
                meridiem == "am" && hour == 12 -> 0
                meridiem == "pm" && hour != 12 -> hour + 12
                else -> hour
            }
            candidates += TimeCandidate(match.value, LocalTime.of(hour24, minute))
        }
        NoonPattern.findAll(text).forEach { candidates += TimeCandidate(it.value, LocalTime.NOON) }
        MidnightPattern.findAll(text).forEach { candidates += TimeCandidate(it.value, LocalTime.MIDNIGHT) }

        InvalidSpokenTimePattern.findAll(text).forEach { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@forEach
            val minute = match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: return@forEach
            if (hour !in 1..12 || minute !in 0..59) {
                conflicts += "${match.value.trim()} is not a valid 12-hour time."
            }
        }
        TwentyFourHourPattern.findAll(text).forEach { match ->
            val hour = match.groupValues[1].toInt()
            if (hour > 12) conflicts += "Use a 12-hour time with AM or PM instead of ${match.value.trim()}."
        }

        val distinct = candidates.distinctBy { it.value }
        if (distinct.size > 1) {
            conflicts += "More than one time was found: ${distinct.joinToString { it.raw.trim() }}."
        }
        val requiresAmPm = BareTimePattern.containsMatchIn(text) && candidates.isEmpty()
        return TimeResult(distinct, requiresAmPm)
    }

    private fun parseDates(text: String, today: LocalDate, includeWeekdays: Boolean): List<DateCandidate> {
        val candidates = mutableListOf<DateCandidate>()
        DayAfterTomorrowPattern.findAll(text).forEach {
            candidates += DateCandidate(it.value, today.plusDays(2))
        }
        val withoutDayAfterTomorrow = text.replace(DayAfterTomorrowPattern, " ")
        TomorrowPattern.findAll(withoutDayAfterTomorrow).forEach {
            candidates += DateCandidate(it.value, today.plusDays(1))
        }
        TodayPattern.findAll(text).forEach { candidates += DateCandidate(it.value, today) }

        IsoDatePattern.findAll(text).forEach { match ->
            runCatching { LocalDate.parse(match.value) }.getOrNull()?.let {
                candidates += DateCandidate(match.value, it)
            }
        }
        NumericDatePattern.findAll(text).forEach { match ->
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val year = match.groupValues[3].ifBlank { today.year.toString() }.toInt()
            runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let {
                candidates += DateCandidate(match.value, it)
            }
        }
        MonthDatePattern.findAll(text).forEach { match ->
            val month = monthNumber(match.groupValues[1]) ?: return@forEach
            val day = match.groupValues[2].toInt()
            val year = match.groupValues[3].ifBlank { today.year.toString() }.toInt()
            runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let {
                candidates += DateCandidate(match.value, it)
            }
        }
        if (includeWeekdays) {
            WeekdayPattern.findAll(text).forEach { match ->
                val day = parseDayOfWeek(match.groupValues[2]) ?: return@forEach
                val next = if (match.groupValues[1].isNotBlank()) {
                    today.with(TemporalAdjusters.next(day))
                } else {
                    today.with(TemporalAdjusters.nextOrSame(day))
                }
                candidates += DateCandidate(match.value, next)
            }
        }
        return candidates.distinctBy { it.value to it.raw.lowercase(Locale.US) }
    }

    private fun extractTask(text: String, intent: CommandIntent, context: ReminderContext?): String? {
        if (intent in setOf(CommandIntent.Cancel, CommandIntent.Complete, CommandIntent.Snooze, CommandIntent.RemindAgain, CommandIntent.Reschedule, CommandIntent.Repeat)) {
            return context?.title
        }
        if (intent == CommandIntent.Rename) {
            val renamed = RenameTargetPattern.find(text)?.groupValues?.get(1)
            return cleanTask(renamed.orEmpty()).takeIf { it.isNotBlank() }?.sentenceCase()
        }

        var candidate = text
        repeat(3) {
            val stripped = stripOnePrefix(candidate)
            if (stripped == candidate) return@repeat
            candidate = stripped
        }
        candidate = cleanTask(candidate)
        repeat(2) { candidate = stripOnePrefix(candidate) }
        candidate = candidate
            .replace(LeadingTaskConnectorPattern, "")
            .replace(TrailingReminderNounPattern, "")
            .trim(' ', ',', '.', '-', ':')
        return candidate.takeIf { it.isNotBlank() }?.sentenceCase()
    }

    private fun cleanTask(value: String): String = value
        .replace(RelativePattern, " ")
        .replace(SpokenTimePattern, " ")
        .replace(NoonPattern, " ")
        .replace(MidnightPattern, " ")
        .replace(BareTimePattern, " ")
        .replace(DayAfterTomorrowPattern, " ")
        .replace(TomorrowPattern, " ")
        .replace(TodayPattern, " ")
        .replace(IsoDatePattern, " ")
        .replace(NumericDatePattern, " ")
        .replace(MonthDatePattern, " ")
        .replace(WeekdayPattern, " ")
        .replace(EveryWeekdayPattern, " ")
        .replace(EveryWeekdayNamePattern, " ")
        .replace(DailyPattern, " ")
        .replace(WeeklyPattern, " ")
        .replace(MonthlyPattern, " ")
        .replace(ClearRecurrencePattern, " ")
        .replace(OrphanSchedulePrepositionPattern, " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', ',', '.', '-', ':')

    private fun stripOnePrefix(value: String): String {
        val trimmed = value.trimStart(' ', ',', '.', '-', ':')
        val prefix = acceptedPrefixes
            .sortedByDescending { it.length }
            .firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            ?: return value
        val boundary = trimmed.getOrNull(prefix.length)
        if (boundary != null && boundary.isLetterOrDigit()) return value
        return trimmed.drop(prefix.length).trimStart(' ', ',', '.', '-', ':')
    }

    private fun normalize(value: String): String = value
        .replace('’', '\'')
        .replace(Regex("(?i)a\\.?\\s*m\\.?"), "AM")
        .replace(Regex("(?i)p\\.?\\s*m\\.?"), "PM")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun monthNumber(value: String): Int? = Month.entries.firstOrNull {
        it.name.startsWith(value.take(3), ignoreCase = true)
    }?.value

    private fun parseDayOfWeek(value: String): DayOfWeek? = DayOfWeek.entries.firstOrNull {
        it.name.startsWith(value.take(3), ignoreCase = true)
    }

    private fun recurrenceLabel(rule: String?): String? = when (rule) {
        null -> null
        "FREQ=DAILY" -> "Every day"
        "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" -> "Every weekday"
        "FREQ=WEEKLY" -> "Every week"
        "FREQ=MONTHLY" -> "Every month"
        else -> DayOfWeek.entries.firstNotNullOfOrNull { day ->
            if (rule == "FREQ=WEEKLY;BYDAY=${day.rruleCode()}") "Every ${day.displayName()}" else null
        }
    }

    private fun CommandIntent.requiresContext(): Boolean = this != CommandIntent.Create

    private fun CommandIntent.displayName(): String = name.replaceFirstChar { it.lowercase() }

    private fun DayOfWeek.rruleCode(): String = name.take(2)

    private fun DayOfWeek.displayName(): String = name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }

    private fun String.sentenceCase(): String = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    private data class RelativeCandidate(val raw: String, val value: ZonedDateTime)
    private data class TimeCandidate(val raw: String, val value: LocalTime)
    private data class DateCandidate(val raw: String, val value: LocalDate)
    private data class TimeResult(val times: List<TimeCandidate>, val requiresAmPm: Boolean)

    companion object {
        val acceptedPrefixes = listOf(
            "Atom, please",
            "Hey Atom",
            "Hi Atom",
            "Hello Atom",
            "Okay Atom",
            "OK Atom",
            "Atom",
            "Could you please remind me",
            "Would you please remind me",
            "Please remind me",
            "Can you remind me",
            "Could you remind me",
            "Would you remind me",
            "Remind me again",
            "Remind me",
            "I want you to remind me",
            "I would like a reminder",
            "I need a reminder",
            "Set a reminder",
            "Give me a reminder",
            "Create a reminder",
            "Schedule a reminder",
            "Set an alert for",
            "Schedule this for",
            "Help me remember",
            "I must remember to",
            "I have to remember to",
            "Don't let me forget",
            "Don't forget to remind me",
            "Make sure I remember",
            "Make sure I don't forget",
            "Ping me",
            "Alert me",
            "Notify me",
            "Nudge me",
            "Give me a heads-up",
            "Add this to my reminders",
            "Put this on my reminder list",
            "Make a note to remind me",
            "Keep this on my radar",
            "Remember that I need to",
            "When it's time",
            "At that time remind me",
            "Let me know when it's time to",
            "Tell me when it's time to",
            "Wake me up to",
            "Do me a favor and remind me",
        )

        private val CancelPattern = Regex("(?i)\\b(?:cancel|delete|remove)\\s+(?:(?:this|the)\\s+)?reminder\\b")
        private val CompletePattern = Regex("(?i)\\b(?:complete|finish|mark)\\s+(?:(?:this|the)\\s+)?reminder(?:\\s+as)?(?:\\s+(?:done|complete))?\\b|\\bmark\\s+(?:this|it)\\s+(?:done|complete)\\b")
        private val SnoozePattern = Regex("(?i)\\bsnooze\\b")
        private val RemindAgainPattern = Regex("(?i)\\bremind\\s+(?:me\\s+)?again\\b")
        private val RenamePattern = Regex("(?i)\\b(?:rename\\s+(?:this|it|the reminder)|change\\s+(?:the\\s+)?(?:title|name|task)|call\\s+this)\\b")
        private val ReschedulePattern = Regex("(?i)\\b(?:reschedule\\s+(?:this|it|the reminder)|move\\s+(?:this|it|the reminder)|postpone\\s+(?:this|it|the reminder)|change\\s+(?:the\\s+)?(?:date|time|schedule)|change\\s+this\\s+to)\\b")
        private val RepeatActionPattern = Regex("(?i)\\b(?:make\\s+(?:this|it)\\s+repeat|stop\\s+repeating|don'?t\\s+repeat|no\\s+longer\\s+repeat)\\b")
        private val RenameTargetPattern = Regex("(?i)\\b(?:rename\\s+(?:this|it|the reminder)|change\\s+(?:the\\s+)?(?:title|name|task)|call\\s+this)\\s+(?:to|as)\\s+(.+)$")

        private val RelativePattern = Regex("(?i)\\b(?:in|after|for)\\s+(\\d+)\\s+(minutes?|hours?|days?|weeks?)\\b")
        private val SpokenTimePattern = Regex("(?i)\\b(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(AM|PM)\\b")
        private val InvalidSpokenTimePattern = Regex("(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(AM|PM)\\b")
        private val NoonPattern = Regex("(?i)\\bnoon\\b")
        private val MidnightPattern = Regex("(?i)\\bmidnight\\b")
        private val TwentyFourHourPattern = Regex("(?i)\\b(?:at|until|to)\\s+([01]?\\d|2[0-3]):[0-5]\\d\\b(?!\\s*(?:AM|PM))")
        private val BareTimePattern = Regex("(?i)\\b(?:at|until|to)\\s+(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\b(?!\\s*(?:AM|PM))")

        private val DayAfterTomorrowPattern = Regex("(?i)\\b(?:the\\s+)?day\\s+after\\s+tomorrow\\b")
        private val TomorrowPattern = Regex("(?i)\\btomorrow\\b")
        private val TodayPattern = Regex("(?i)\\btoday\\b")
        private val IsoDatePattern = Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b")
        private val NumericDatePattern = Regex("\\b(0?[1-9]|1[0-2])/(0?[1-9]|[12]\\d|3[01])(?:/(\\d{4}))?\\b")
        private val MonthDatePattern = Regex("(?i)\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?\\b")
        private val WeekdayPattern = Regex("(?i)\\b(?:(next)\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")

        private val ClearRecurrencePattern = Regex("(?i)\\b(?:stop\\s+repeating|don'?t\\s+repeat|do\\s+not\\s+repeat|no\\s+longer\\s+repeat|one[ -]time|once)\\b")
        private val EveryWeekdayPattern = Regex("(?i)\\b(?:every\\s+weekday|weekdays)\\b")
        private val EveryWeekdayNamePattern = Regex("(?i)\\bevery\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")
        private val DailyPattern = Regex("(?i)\\b(?:every\\s+day|daily)\\b")
        private val WeeklyPattern = Regex("(?i)\\b(?:every\\s+week|weekly)\\b")
        private val MonthlyPattern = Regex("(?i)\\b(?:every\\s+month|monthly)\\b")

        private val LeadingTaskConnectorPattern = Regex("(?i)^(?:to|about|for|that)\\s+")
        private val TrailingReminderNounPattern = Regex("(?i)\\s+(?:reminder|alert)$")
        private val OrphanSchedulePrepositionPattern = Regex("(?i)\\b(?:at|on)\\s*(?=(?:to|about|for|that|remind|please|can|could|would)\\b|$)")
    }
}
