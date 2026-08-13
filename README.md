# Atom

Atom is a personal, offline-first reminder assistant for Dhiren. It accepts natural text or voice-shaped commands, extracts scheduling details, asks once for anything missing, and is designed to deliver reminders reliably from the device.

This repository currently contains two complementary surfaces:

- `android/` — the real Kotlin + Jetpack Compose Android application UI.
- `atom-ios/` — the separate native SwiftUI iPhone and iPad application.
- `app/` — the approved interactive web prototype used to iterate on the visual direction.

## Current status

The Android Phase 1 experience is implemented: light/dark themes, time-aware greeting, animated quick capture, 14 Atom marks, capture/review/follow-up flows, reminder editing, recurrence, settings, and full-screen Alarm Mode.

Durable Room storage, deterministic parsing, Android speech recognition, local alarm scheduling, notification actions, and device lifecycle recovery are connected. Phase 1 is intentionally local-only: no Railway service, PostgreSQL database, account, or synchronization queue is required. Optional Railway backup and synchronization are deferred to Phase 2, and the optional OpenAI fallback is not connected.

## Run the Android app

Open the `android/` directory in Android Studio, allow Gradle sync to finish, and run the `app` configuration on an Android 8.0+ device or emulator.

From a machine with JDK 17 and the Android SDK installed:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to:

```text
android/app/build/outputs/apk/debug/atom.v3.apk
```

The V3 APK artifact is named `atom.v3.apk` (version `0.3.0`, code `3`).

## Run the iOS app

The iOS application requires the full Xcode application and iOS SDK. After
installing Xcode and XcodeGen 2.46 or newer:

```bash
cd atom-ios
xcodegen generate
open atom-ios.xcodeproj
```

The iOS target and installed app are named `atom-ios`. Its platform-neutral
parser and permission checks can also be run with `swift run AtomCoreChecks`; see
`atom-ios/README.md` for current platform limitations.

Separate GitHub Actions workflows validate Android and iOS changes.

## Run the web prototype

The prototype needs Node.js 22.13 or later.

```bash
npm install
npm run dev
```

Then open `http://localhost:3000`.

## Product documentation

- `docs/ATOM_BEHAVIOR.md`
- `docs/NATURAL_LANGUAGE_RULES.md`
- `docs/NOTIFICATION_POLICY.md`
- `docs/PRIVACY.md`
- `docs/FAILURE_SCENARIOS.md`
- `docs/LOCAL_DATABASE.md`
- `docs/ROADMAP.md`
- `docs/changelog/` — one changelog fragment per pull request

## Repository safety

Signing keys, local Android SDK paths, environment files, and generated build output are excluded from Git. Keep the release keystore and any future Phase 2 Railway/OpenAI credentials outside the repository.
