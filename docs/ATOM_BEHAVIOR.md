# Atom behavior

## Identity

Atom is a private, single-owner reminder assistant. In Phase 1 the owner name is
hardcoded as **Dhiren Sir**. Atom must not expose multi-user controls or imply
that another account can use this installation.

The home greeting follows the device's local time:

- 05:00–11:59: Good morning
- 12:00–16:59: Good afternoon
- 17:00–21:59: Good evening
- 22:00–04:59: Good night

The device locale and IANA timezone are detected automatically. All visible
times use 12-hour format with AM or PM.

## Core behavior

Atom should be calm, short, and explicit. It may normalize obvious speech
variations, but it must never invent a date, time, AM/PM value, recurrence, or
task.

1. Parse the task, date, time, timezone, and recurrence separately.
2. If both date and time are present, show a review step before scheduling.
3. If either date or time is missing, ask once in a voice-and-text follow-up.
4. If the owner skips the follow-up, save the reminder to Unscheduled.
5. Display the exact schedule Atom understood before the owner confirms.

## Reminder states

| State | Meaning | Atom action |
| --- | --- | --- |
| `scheduled` | Date and time are known | Persist and schedule the local alarm |
| `needs_time` | Date or recurrence is known | Ask for a 12-hour time |
| `needs_date` | Time is known | Ask for a date |
| `unscheduled` | Neither is known | Save safely without an alarm |

Recurring reminders are scheduled one occurrence at a time. After an occurrence
is completed or missed, Atom computes and schedules the next occurrence from
`recurrence_rule`.

## Editing and commands

An edit may change the task, date, time, or recurrence through text, voice, or
direct fields. Before saving an edited scheduled reminder, Atom cancels the
previous Android alarm and then schedules its replacement. The operation is
idempotent so the owner never receives both alarms.

Supported edit intents include reschedule, rename, repeat, stop repeating,
cancel, complete, snooze, and remind again. “Remind me again” creates a new
schedule from an already-fired reminder and does not silently overwrite its
history.

## Intelligence boundary

The deterministic parser is primary. Optional OpenAI fallback is allowed only
when enabled by the owner and only after a failed or low-confidence local parse.
Atom must show the fallback result for confirmation. It must not let a model
directly schedule, edit, or delete a reminder.

