# Notification and alarm history

Date: 2026-08-10

## Added

- Added an Android Room `notification_history` table and a non-destructive
  version 2 → 3 migration.
- Added a matching SwiftData notification-history model to `atom-ios`.
- Changed the notification bell to open a focused alarm activity screen on both
  platforms.
- Displayed alarm events newest-first with event type, reminder title, action
  detail, time, and any resulting snooze/remind-again schedule.
- Added unread indicators that clear after the history screen is opened.
- Recorded rang, missed, opened, snoozed, reminded-again, completed, and ignored
  states where each operating system exposes them.
- Added Ignore to the Android full-screen alarm and to Android/iOS notification
  actions.

## Reliability and privacy

- History remains on-device and does not require network, storage, or any new
  runtime permission.
- Android records alarm delivery directly in its receiver.
- iOS records foreground delivery immediately and reconciles delivered local
  notifications when Atom becomes active.
- Notification history deliberately survives reminder deletion so the event
  remains understandable.

## Tests

- Added Android notification-history repository coverage.
- Added Android one-off and recurring ignore behavior coverage.
- Added an Android instrumented Room version 2 → 3 migration test.
- Added iOS notification-history model coverage to the native test target.
