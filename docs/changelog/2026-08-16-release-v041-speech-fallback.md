# Atom V0.4.1 speech fallback

Date: 2026-08-16

## Fixed

- Kept voice input available when Android has a standard speech-recognition
  service but no dedicated on-device recognizer.
- Added a fallback offer when the on-device service cannot handle the selected
  language.
- Added an explicit per-recording confirmation before Android's standard
  recognition service may receive microphone audio over the internet.
- Preserved the existing transcript when online speech is declined or fails.
- Updated recovery messages and privacy documentation for both recognition
  modes.

## Release

- Advanced the Android package to version `0.4.1` (`versionCode` 5).
- Named the installable Android artifact `atom.v0.4.1.apk`.

## Validation

- Android: 91 unit tests, lint with zero errors, debug compilation, and APK
  assembly passed.
- APK: package metadata confirms version `0.4.1` / code `5`; APK Signature
  Scheme v2 verification passed with the Android debug signer used for the
  personal installable build.
- Repository ESLint, TypeScript, production web build, and rendered HTML test
  passed.
- Production npm dependency audit found zero known vulnerabilities.
