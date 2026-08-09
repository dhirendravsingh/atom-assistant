# First-launch permission setup

Date: 2026-08-09

## Added

- Added a first-launch explanation followed by notification and microphone
  runtime permission prompts.
- Added a direct transition to Android's Alarms & reminders special-access
  screen when exact-alarm access is missing.
- Added tests for permission-request ordering and already-granted permissions.

## Changed

- Kept full-screen access scoped to optional Alarm Mode.
- Clarified that Room uses private internal app storage and requires no storage,
  photo, media, or file permission.
- Reclassified battery optimization as optional troubleshooting rather than an
  application requirement or health failure.
