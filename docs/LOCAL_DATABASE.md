# Local database

Atom uses Android Room as its offline source of truth. Room creates a private
SQLite file named `atom.db` automatically when the application first accesses
the database. The owner does not need to provision a service, create
credentials, or initialize a database manually.

## Version 1 schema

### `owner_profile`

The single row stores the Phase 1 owner identity, detected IANA timezone and
device locale. The row is inserted only when a new database is created.

### `reminders`

Each reminder stores:

- task title and original source text
- nullable `scheduled_at_utc`
- nullable ISO `local_date` and 24-hour storage `local_time`
- required IANA `timezone`
- nullable RFC 5545 `recurrence_rule`
- source, state and visual accent
- creation and update timestamps

The database stores time in a normalized machine-readable form. Atom continues
to display all times in 12-hour format with AM or PM.

## Lifecycle

- Creating, editing and deleting reminders writes through `ReminderRepository`.
- Compose observes the DAO `Flow`, so screens update after committed database
  changes.
- Closing or restarting Atom does not remove reminders.
- Clearing application data or uninstalling Atom removes the local database.
- Future schema changes must increment the database version and include a
  tested Room migration. Exported schemas live under `android/app/schemas/`.

Railway PostgreSQL remains an optional future synchronization and backup layer;
it does not replace Room or affect offline reminder creation.
