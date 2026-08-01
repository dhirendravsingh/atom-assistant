# Room persistence

## Added

- Added Room 2.8.4 with KSP processing and versioned schema export.
- Added `owner_profile` and `reminders` database entities, DAOs and a singleton
  `AtomDatabase` backed by the private `atom.db` file.
- Added a repository that normalizes local date, local time, timezone, UTC
  schedule and recurrence values before persistence.
- Added unit coverage for absolute, relative and recurring schedule storage.
- Added local database architecture and lifecycle documentation.

## Changed

- Replaced the in-memory sample reminder list with a Room-backed reactive Flow.
- Routed reminder creation, editing and deletion through durable storage.
- Preserved the original capture text when reopening a reminder for editing.

## Validation

- Verified the Room schema and repository through the Android unit and debug APK
  build workflow.
