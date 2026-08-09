# Android device reliability

Date: 2026-08-03

## Added

- Added a serialized device-reconciliation coordinator for application launch,
  reboot, application update, clock changes, timezone changes, locale changes,
  and exact-alarm permission changes.
- Added persisted reconciliation health including the last successful time,
  reason, restored alarm count, and detected missed-reminder count.
- Added missed-reminder recovery with a 24-hour relevance window, persistent
  occurrence deduplication, and Done, Snooze, and Remind Again notification
  actions.
- Added a bounded on-device missed-occurrence backlog so notification denial
  cannot silently discard a recurring occurrence while its next alarm advances.
- Added a Missed reminder state, list filter, and reminder badge for one-off
  occurrences that became due while Atom was unavailable.
- Added device health detection for notification access, exact-alarm access,
  reminder-channel availability, full-screen intent access, and
  battery-optimization status.
- Added direct repair routes to Android notification, exact-alarm, full-screen,
  and battery-optimization settings.
- Added Settings visibility for the last successful alarm restoration event.

## Changed

- Reboot and application-update handling now restores future alarms and surfaces
  relevant missed reminders from Room.
- Clock and timezone changes now explicitly recalculate recurring schedules from
  their local time, recurrence rule, and stored IANA timezone.
- Missed recurring occurrences advance to their next valid future alarm without
  losing the missed-occurrence notification.
- Missed one-off reminders older than 24 hours remain visible without creating
  a disruptive late alert.
- Returning from Android reliability settings immediately reconciles alarms and
  retries eligible missed-reminder delivery after access is restored.

## Verification required before commit and push

- `./gradlew lintDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
