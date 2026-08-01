package com.dhiren.atom.ui

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
)

val naturalLanguagePrefixes = listOf(
    "Atom",
    "Hey Atom",
    "Hi Atom",
    "Hello Atom",
    "Okay Atom",
    "Atom, please",
    "Please remind me",
    "Can you remind me",
    "Could you remind me",
    "Would you remind me",
    "Remind me",
    "Remind me again",
    "I need a reminder",
    "Set a reminder",
    "Create a reminder",
    "Schedule a reminder",
    "Help me remember",
    "Don’t let me forget",
    "Make sure I remember",
    "Ping me",
    "Alert me",
    "Notify me",
    "Nudge me",
    "Add this to my reminders",
    "Keep this on my radar",
    "Give me a heads-up",
)
