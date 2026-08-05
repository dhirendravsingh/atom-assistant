# Atom Android

This is the native Android UI for Atom, built with Kotlin and Jetpack Compose.

## Requirements

- Android Studio with Android SDK 36
- JDK 17
- Android 8.0 (API 26) or newer device/emulator

## Build

```bash
./gradlew testDebugUnitTest assembleDebug
```

The first build downloads Gradle and Android dependencies. The checked-in Gradle wrapper keeps command-line and CI builds aligned.

## Package

`com.dhiren.atom`

## Implemented in this UI milestone

- premium responsive Today screen
- explicit light and dark color systems
- time-aware greeting for “Dhiren Sir”
- animated quick-capture accent lines and waving Atom doodle
- text and microphone-shaped capture interactions
- deterministic preview for 12-hour time, relative time, dates, and recurrence phrases
- one-time missing-date/time follow-up dialog
- Room-backed editable reminders and filters
- 14 selectable Atom logo directions
- full-screen Alarm Mode preview

## Local persistence

Room creates the private `atom.db` database automatically on first use. The
database contains the single owner profile and normalized reminder records.
No external database initialization or credentials are required.

## Deferred work

Phase 1 uses Room and Android device services only. Railway/PostgreSQL backup,
the synchronization API, offline network outbox, and conflict resolution are
deferred to Phase 2. An opt-in OpenAI parser fallback is also deferred. No API
key belongs in the Android package.
