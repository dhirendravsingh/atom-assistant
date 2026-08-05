# Testing, release signing, and security hardening

Date: 2026-08-05

## Added

- Added Room instrumentation coverage for reminder persistence, editing, and
  deletion on a real Android runtime.
- Added schedule regression tests for noon, midnight, daylight-saving gaps and
  overlaps, recurrence across DST, reboot restoration, and alarm replacement.
- Added environment-only release signing configuration and release build
  documentation.
- Release builds disable Gradle configuration caching so signing credentials do
  not enter cached configuration state.
- Added explicit backup and device-transfer exclusion rules for private reminder
  data.

## Changed

- Enabled release code and resource shrinking.
- Disabled cleartext Android traffic and application-data backup.
- Removed the unused Drizzle/D1 backend scaffold from the local-only Phase 1
  prototype.
- Upgraded vulnerable web build dependencies and added an explicit TypeScript
  typecheck command.

## Verification required before commit

- `npm run lint`
- `npm run typecheck`
- `npm test`
- `npm audit`
- `./gradlew lintDebug testDebugUnitTest assembleDebugAndroidTest`
- `./gradlew assembleRelease`
- `apksigner verify --verbose --print-certs app-release.apk`
