package com.dhiren.atom.nlp

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class CommandIntent {
    Create,
    Reschedule,
    Rename,
    Cancel,
    Snooze,
    Complete,
    RemindAgain,
    Repeat,
}

enum class MissingField {
    Task,
    Date,
    Time,
    AmPm,
    Context,
}

sealed interface RecurrenceUpdate {
    data object Unchanged : RecurrenceUpdate
    data object Clear : RecurrenceUpdate
    data class Set(val rule: String, val label: String) : RecurrenceUpdate
}

data class ReminderContext(
    val id: Long,
    val title: String,
    val localDate: LocalDate?,
    val localTime: LocalTime?,
    val timezone: ZoneId,
    val recurrenceRule: String?,
)

data class ParsedCommand(
    val intent: CommandIntent,
    val task: String?,
    val localDate: LocalDate?,
    val localTime: LocalTime?,
    val scheduledAtUtc: Instant?,
    val timezone: ZoneId,
    val recurrence: RecurrenceUpdate,
    val recurrenceRule: String?,
    val recurrenceLabel: String?,
    val sourceText: String,
    val relativeLabel: String? = null,
    val dateWasSpecified: Boolean = false,
    val timeWasSpecified: Boolean = false,
    val scheduleWasSpecified: Boolean = false,
    val missingFields: Set<MissingField> = emptySet(),
    val conflicts: List<String> = emptyList(),
    val confidence: Double = 1.0,
) {
    val needsConfirmation: Boolean
        get() = missingFields.isNotEmpty() || conflicts.isNotEmpty()

    val isActionable: Boolean
        get() = !needsConfirmation
}
