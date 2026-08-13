# Atom V3 hourly recurrence

Date: 2026-08-13

## Added

- Deterministic parsing for hourly recurrence commands such as “remind me to
  drink water every 2 hours a day.”
- `FREQ=HOURLY;INTERVAL=n` persistence for intervals from 1 through 24 hours.
- A first occurrence one interval after confirmation when the owner does not
  provide an explicit starting time.
- Alarm advancement that stores and schedules the nearest future occurrence
  after each delivery.
- Recovery logic that skips elapsed interval occurrences after downtime while
  preserving the original cadence.
- Parser, recurrence calculator, persistence, delivery, and reboot regression
  coverage for interval reminders.

## Release

- Advanced the Android package to version `0.3.0` (`versionCode` 3).
- Named the installable Android artifact `atom.v3.apk`.
- Includes the approved notification history, navigation, search, dark-mode,
  launcher alignment, and homepage voice-entry polish from the V3 branch.

## Validation

- Android: 88 unit tests, lint, debug compilation, and APK assembly passed.
- APK: package metadata confirms version `0.3.0` / code `3`; APK Signature
  Scheme v2 verification passed with the Android debug signer used for the
  personal installable build.
- Web prototype: TypeScript, ESLint, production build, and rendered HTML test
  passed.
- iOS shared core checks passed 10/10 for the cross-platform files included in
  the same change set.
- Production npm dependencies audit with zero known vulnerabilities. The full
  development-tree audit still reports the upstream `vinext -> image-size`
  advisory; npm offers only a breaking vinext downgrade, so it was not applied.
