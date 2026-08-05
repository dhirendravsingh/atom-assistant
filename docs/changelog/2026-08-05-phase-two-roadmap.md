# Defer Railway synchronization to Phase 2

Date: 2026-08-05

## Added

- Added a product roadmap separating the local-only Android release from future
  cloud synchronization work.
- Defined the optional Phase 2 Railway/PostgreSQL database, migration,
  single-owner API, offline outbox, incremental synchronization, conflict
  handling, recovery, privacy, and test scope.

## Changed

- Made Room the only Phase 1 source of truth and removed Railway from the list
  of current Android implementation milestones.
- Updated notification and privacy policies to identify Railway as Phase 2-only
  infrastructure.
- Clarified that Phase 2 synchronization remains optional and cannot become a
  dependency for local reminder creation or alarm delivery.
