# Native Android UI

## Added

- Added a Kotlin and Jetpack Compose Android application under `android/`.
- Added the approved Today, capture, reminders, settings, logo gallery, follow-up, and Alarm Mode preview experiences.
- Added explicit light and dark palettes, a time-aware “Dhiren Sir” greeting, quick-capture motion, and the waving Atom doodle.
- Added all 14 approved Atom logo directions with the original preserved as the default.
- Added GitHub Actions verification and debug APK artifact generation.

## Changed

- Replaced the starter README with Atom-specific setup, architecture, safety, and project-status guidance.
- Extended `.gitignore` for Android build output, SDK configuration, and signing keys.

## Validation

- Validated shell and XML project files locally.
- Added unit coverage for greeting boundaries and the complete logo option set.
- Added a CI build because the current development machine does not have a local JDK or Android SDK installed.
