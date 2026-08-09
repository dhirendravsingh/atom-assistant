# Lock-screen Alarm Mode reliability

Date: 2026-08-09

## Fixed

- Added Android 14+ full-screen alarm access to Atom's first-launch permission
  sequence so ringing reminders can appear above the lock screen.
- Changed special-access setup to wait for the owner to return from the exact
  alarm screen before opening the full-screen alarm screen.
- Added a one-time onboarding version bump so existing installs receive the new
  lock-screen setup once after updating.
- Enabling Alarm Mode now opens Android's repair screen when full-screen access
  is missing.

## Confirmed

- `AlarmActivity` declares and applies show-when-locked, turn-screen-on, and
  keep-screen-on behavior.
- Reminder notifications remain public on the lock screen and use a high
  importance alarm channel with a full-screen intent.

## Verification required before commit and push

- `./gradlew lintDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebugAndroidTest`
- `npm run lint`
- `npm run typecheck`
- `npm test`
