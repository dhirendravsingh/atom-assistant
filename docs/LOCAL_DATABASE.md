# Local database

Atom uses Android Room as its offline source of truth. Room creates a private
SQLite file named `atom.db` automatically when the application first accesses
the database. The owner does not need to provision a service, create
credentials, or initialize a database manually.

## Version 3 schema

### `owner_profile`

The single row stores the editable display name, gender option, pronoun option,
detected IANA timezone, and device locale. The row is inserted only when a new
database is created.

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

### `notification_history`

Each Android alarm event stores the reminder identifier and title, the event
type, a short status detail, its UTC occurrence time, an optional resulting
schedule, and whether the event has been read. Rows deliberately do not use a
foreign key so history remains understandable after a reminder is deleted.
The matching iOS application stores the same activity in private SwiftData.
Both notification views order the newest event first.

## Lifecycle

- Creating, editing and deleting reminders writes through `ReminderRepository`.
- Compose observes the DAO `Flow`, so screens update after committed database
  changes.
- Closing or restarting Atom does not remove reminders.
- Clearing application data or uninstalling Atom removes the local database.
- Future schema changes must increment the database version and include a
  tested Room migration. Exported schemas live under `android/app/schemas/`.

Migration 1 → 2 adds gender and pronouns without deleting reminders. It also
normalizes the original `Dhiren Sir` value to `Dhiren`, because the greeting now
adds `Sir` dynamically from the selected gender.

Migration 2 → 3 adds the notification history table without changing or
deleting profiles or reminders.

Phase 1 has no external database. Railway PostgreSQL is deferred to the optional
Phase 2 backup and synchronization work described in `ROADMAP.md`. Even then,
it must not replace Room or affect offline reminder creation and delivery.
