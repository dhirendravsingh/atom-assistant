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
    val id: Int,
    val title: String,
    val date: String?,
    val time: String?,
    val source: String,
    val state: ReminderState,
    val accent: ReminderAccent,
    val recurrence: String? = null,
)

val sampleReminders = listOf(
    ReminderUi(
        id = 1,
        title = "Send product brief to Aisha",
        date = "Today · Jul 31",
        time = "6:30 PM",
        source = "Voice",
        state = ReminderState.Scheduled,
        accent = ReminderAccent.Mint,
    ),
    ReminderUi(
        id = 2,
        title = "Renew car insurance",
        date = "Tomorrow · Aug 1",
        time = "10:00 AM",
        source = "Text",
        state = ReminderState.Scheduled,
        accent = ReminderAccent.Coral,
    ),
    ReminderUi(
        id = 3,
        title = "Call Rhea about the launch",
        date = "Friday · Aug 7",
        time = null,
        source = "Voice",
        state = ReminderState.NeedsTime,
        accent = ReminderAccent.Lime,
    ),
    ReminderUi(
        id = 4,
        title = "Review today’s priorities",
        date = "Every weekday",
        time = "9:00 AM",
        source = "Voice",
        state = ReminderState.Scheduled,
        accent = ReminderAccent.Mint,
        recurrence = "Every weekday",
    ),
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
