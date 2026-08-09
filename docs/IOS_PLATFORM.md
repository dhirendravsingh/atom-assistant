# atom-ios platform policy

`atom-ios` is a separate native SwiftUI application. It does not run the APK,
embed the web prototype, or request Android-only permissions.

## Local data

SwiftData stores reminders in the application's private container. No photo,
media, file, or storage permission is needed. Phase 1 has no backend and does
not require Railway, PostgreSQL, APNs, or another service to schedule local
notifications.

## Permission flow

After a short Atom explanation, iOS presents its own system prompts for:

1. notifications, sound, badges, Lock Screen delivery, and notification actions
2. microphone access, used only after the owner taps the microphone
3. speech recognition, used to turn dictation into editable reminder text

Denied access is never treated as granted. Text capture remains available, and
Settings includes a repair link. The application checks authorization again
after returning from iOS Settings.

## Delivery levels

- iOS 17–25: schedule `UNCalendarNotificationTrigger` local notifications with
  time-sensitive interruption level and Done, Snooze, and Remind Again actions.
- iOS 26+: AlarmKit is the required release-hardening path for prominent alarms
  that can break through silent mode and Focus. It must be compiled and tested
  with the iOS 26 SDK before being enabled.

Local User Notifications do not guarantee Android-style full-screen ringing.
The UI must state this honestly until AlarmKit is integrated and validated.

## Release requirements

Full Xcode is required to compile the SwiftUI target, run iOS static analysis,
execute simulator/device tests, sign the app, and produce an IPA. Command Line
Tools alone are insufficient because they do not include the iPhoneOS SDK.
