# Notification history and navigation polish

## Added

- Notification history detail popups showing the scheduled time, ring time,
  selected outcome, action time, and replacement schedule when present.
- Functional reminder search with focused keyboard input and live filtering on
  Android, iOS, and the interactive web prototype.
- Tapping the homepage microphone now opens Quick Capture with voice listening
  already active; opening Capture from the add button or a text/card action
  continues to start in editable text mode.
- Android system-back handling that returns from secondary screens to the
  appropriate previous screen instead of exiting immediately.

## Changed

- Removed the “Call Rhea” unscheduled demo reminder and replaced remaining
  visible Rhea sample prompts with a neutral example.
- Ignored notification entries now use a soft red full-card background.
- Added more breathing room between the theme, notification, and profile
  controls in the Android and web headers.
- Corrected the Android Settings profile-card colors in dark mode.
- Corrected the Android adaptive-icon orbit geometry so the atom mark is
  vertically centered in launcher thumbnails.
- Ring history now retains the intended alarm time for notification details.

## Validation

- Web lint, typecheck, production build, rendered HTML tests, and browser
  interaction QA passed.
- iOS core checks passed (10/10). Full iOS tests require Xcode; this machine has
  Command Line Tools only and its Swift CLI does not provide the `Testing`
  module used by the package tests.
- Android lint, unit tests, and debug APK assembly passed using Android SDK 36.
