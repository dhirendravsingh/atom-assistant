package com.dhiren.atom.ui

import com.dhiren.atom.nlp.AtomCommandParser
import java.time.ZoneId

enum class AtomScreen {
    Today,
    Capture,
    Reminders,
    Settings,
}

enum class ReminderState {
    Scheduled,
    NeedsTime,
    NeedsDate,
    Unscheduled,
    Completed,
    Canceled,
}

enum class ReminderAccent {
    Mint,
    Coral,
    Lime,
}

data class ReminderUi(
    val id: Long,
    val title: String,
    val date: String?,
    val time: String?,
    val source: String,
    val state: ReminderState,
    val accent: ReminderAccent,
    val recurrence: String? = null,
    val sourceText: String = "",
    val scheduledAtUtc: String? = null,
    val localDate: String? = null,
    val localTime: String? = null,
    val timezone: String = ZoneId.systemDefault().id,
    val recurrenceRule: String? = null,
)

val naturalLanguagePrefixes = AtomCommandParser.acceptedPrefixes
