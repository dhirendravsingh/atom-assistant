# Reliable Android notifications

Date: 2026-08-02

## Added

- Added `AlarmManager` scheduling with exact idle alarms when authorized and an
  idle-safe fallback when exact-alarm access is unavailable.
- Added a high-importance reminder channel with alarm sound, vibration, public
  lock-screen visibility, and full-screen intent support.
- Added real Done, Snooze 10 minutes, and Remind in 1 hour actions to both the
  notification and Alarm Mode.
- Notification-shade actions now also stop and close an active full-screen
  Alarm Mode session for the same reminder.
- Added the real lock-screen `AlarmActivity`, including a looping alarm sound,
  repeating vibration, screen wake behavior, and the approved premium UI.
- Added reboot, app-upgrade, time-change, timezone-change, and locale-change
  reconciliation from Room.
- Added recurrence scheduling for daily, weekday, named-weekday, weekly, and
  monthly rules.
- Added notification, exact-alarm, and full-screen-access health reporting with
  direct Android settings repair actions.
- Added unit tests for recurrence calculation, PendingIntent identity, alarm
  replacement ordering, deletion, snooze, and remind-again behavior.

## Changed

- Every scheduled Room save now cancels its previous alarm before registering
  the replacement.
- Deleting and completing one-off reminders cancel their outstanding alarms.
- Alarm Mode settings now persist and control whether reminder notifications
  launch the full-screen ringing activity.
- The existing Alarm Mode preview now renders the same Compose screen used by
  real alarms.

## Verification required before commit and push

- `./gradlew lintDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
