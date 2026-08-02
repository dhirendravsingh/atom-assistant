# Android speech recognition

Date: 2026-08-02

## Added

- Connected the capture microphone to Android `SpeechRecognizer` with partial
  and final transcripts.
- Added runtime microphone-permission requests and a direct app-settings repair
  action when permission is denied.
- Added Android 12+ on-device recognizer detection and creation.
- Added offline-preferred recognition for Android 8–11, with explicit language
  pack and offline-availability errors.
- Added working microphone actions to missing-date and missing-time follow-up
  fields.
- Added speech error-message unit tests and Android package visibility for the
  installed recognition service.

## Changed

- The microphone no longer inserts sample text; recognized speech becomes the
  editable command.
- Voice capture works while creating or editing a reminder.
- Typed text and follow-up values are restored when recognition fails after a
  partial result.
- Reminder source metadata records voice use for either the main command or a
  spoken date/time follow-up.
- Privacy and failure documentation now describe the real Android recognition
  behavior and recovery paths.

## Verification required before commit

- `./gradlew lintDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
