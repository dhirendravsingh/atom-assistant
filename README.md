# Atom

Atom is a personal, Android-first reminder assistant for Dhiren. It accepts natural text or voice-shaped commands, extracts scheduling details, asks once for anything missing, and is designed to deliver reminders reliably from the device.

This repository currently contains two complementary surfaces:

- `android/` — the real Kotlin + Jetpack Compose Android application UI.
- `app/` — the approved interactive web prototype used to iterate on the visual direction.

## Current status

The Android Phase 1 UI is implemented: light/dark themes, time-aware greeting, animated quick capture, 14 Atom marks, capture/review/follow-up flows, reminder editing, recurring reminder presentation, settings, and a full-screen Alarm Mode preview.

The current Android code is deliberately a UI milestone. Durable Room storage, Android alarm scheduling, on-device speech recognition, notification channels, reboot recovery, Railway sync, and the optional OpenAI fallback are not connected yet. The UI labels these boundaries instead of pretending they are live.

## Run the Android app

Open the `android/` directory in Android Studio, allow Gradle sync to finish, and run the `app` configuration on an Android 8.0+ device or emulator.

From a machine with JDK 17 and the Android SDK installed:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs the same tests and debug build whenever Android files change.

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
- `docs/changelog/` — one changelog fragment per pull request

## Repository safety

Signing keys, local Android SDK paths, environment files, and generated build output are excluded from Git. Keep the eventual release keystore and Railway/OpenAI credentials outside the repository.
