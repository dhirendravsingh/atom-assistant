# Natural-language parser

Date: 2026-08-01

## Added

- Added an on-device, deterministic command parser with typed intents,
  structured schedules, confidence, missing-field reporting, and conflict
  reporting.
- Added create, reschedule, rename, cancel, snooze, complete, remind-again, and
  recurrence-change actions backed by the Room reminder record.
- Added relative minute, hour, day, and week schedules; today/tomorrow/day-after
  dates; weekdays; explicit dates; noon; midnight; and validated AM/PM times.
- Added daily, weekday, named-weekday, weekly, and monthly recurrence parsing,
  including commands that clear recurrence.
- Added a fixed-clock parser test corpus covering successful commands, partial
  commands, context-aware edits, action conflicts, schedule conflicts, invalid
  time formats, and past schedules.

## Changed

- The capture confirmation now shows the detected action and blocks ambiguous
  commands before they can mutate Room.
- Follow-up date/time answers are parsed through the same command engine instead
  of being copied into storage as unvalidated display strings.
- Reminder UI models now retain normalized local date, local time, timezone,
  UTC instant, and recurrence rule values supplied by the parser.
- Completed and canceled reminders are retained as explicit states; completed
  reminders have their own list filter and no longer count as missing details.

## Verification

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
