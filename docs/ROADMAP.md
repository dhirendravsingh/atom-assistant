# Atom roadmap

## Phase 1 — dependable single-device reminders

Phase 1 is intentionally local-only. Atom serves one owner on one Android
phone, and the private Room database on that phone is the only source of truth.
Creating, editing, scheduling, ringing, snoozing, repeating, completing, and
recovering reminders must not require an account, server, or internet access.

Phase 1 includes:

- text and Android speech-recognition capture
- deterministic natural-language reminder parsing and follow-up questions
- Room persistence for the owner profile and reminders
- exact local alarms, notification actions, and full-screen Alarm Mode
- recurrence, missed-reminder handling, and device lifecycle recovery
- notification, exact-alarm, and battery-reliability health reporting
- release signing, on-device testing, and a signed Android APK

Phase 1 explicitly excludes Railway, PostgreSQL, login, server APIs, cloud
backup, cross-device synchronization, and an offline network sync queue.

## Phase 2 — optional Railway backup and synchronization

Phase 2 may be started only when cloud backup, installation recovery, a web
dashboard, or use across multiple devices becomes a real requirement. It must
remain optional and must never make local reminder delivery dependent on the
network.

Phase 2 scope:

1. Provision Railway PostgreSQL and add versioned, reversible migrations for
   the owner, registered installations, reminders, recurrence data, mutation
   history, and deletion tombstones.
2. Build a private single-owner API with revocable installation credentials,
   TLS, strict payload validation, idempotency keys, and no public registration
   flow.
3. Add a Room-backed offline outbox. Local mutations commit to Room first and
   are queued for retry with bounded exponential backoff when connectivity is
   available.
4. Add incremental pull synchronization using stable reminder IDs and server
   cursors so reinstall recovery does not require downloading an unbounded
   history.
5. Resolve conflicts without silently overwriting the phone. Owner-confirmed
   local edits remain authoritative; ambiguous concurrent edits are retained
   for explicit review, and deletes use tombstones rather than disappearing.
6. Add encrypted backup, restore, observability with reminder text redacted,
   retention controls, and a complete “delete cloud data” operation.
7. Test offline creation, retry, duplicate delivery, clock skew, reinstall
   recovery, concurrent edits, deletion conflicts, and Railway outages.

Even in Phase 2, Room remains the operational source of truth for the Android
application. The server is an optional backup and synchronization replica;
AlarmManager always schedules from the confirmed local Room state.
