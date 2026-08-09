# Separate iOS application foundation

Date: 2026-08-09

## Added

- Added a separate native SwiftUI application named `atom-ios`, targeting iOS
  17 and newer without changing the Android application architecture.
- Added SwiftData reminder persistence, local notification scheduling, Done,
  Snooze, and Remind Again actions, typed and voice capture, missing-schedule
  follow-up, reminders, settings, profile fields, and light/dark appearance.
- Added platform-specific notification, microphone, and speech-recognition
  setup with no storage permission.
- Added an Atom iOS app icon, XcodeGen project definition, Swift Package core,
  parser and permission-plan tests, and an iOS GitHub Actions build workflow.
- Documented iOS delivery guarantees and the iOS 26 AlarmKit hardening step.

## Changed

- Android APK outputs are now named `atom-android-debug.apk` and
  `atom-android-release.apk`.

## Local verification available without full Xcode

- Swift source-format parsing
- Swift Package manifest and core tests when the installed Command Line Tools
  toolchain matches its macOS SDK
- XcodeGen project generation and project-file validation
- Android lint, unit tests, and APK assembly
- Web lint, typecheck, build, and render test

The SwiftUI application target, iOS analyzer, simulator tests, and signed IPA
remain unverified until full Xcode and the iPhoneOS SDK are installed.
