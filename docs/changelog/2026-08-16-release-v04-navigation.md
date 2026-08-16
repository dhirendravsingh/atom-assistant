# Atom V0.4 navigation

Date: 2026-08-16

## Changed

- Made the owner-profile initial in the home header open Settings.
- Removed Settings from the bottom navigation.
- Reordered the three evenly spaced bottom actions to Today, Add, and
  Reminders.
- Advanced the Android package to version `0.4.0` (`versionCode` 4).
- Named the installable Android artifact `atom.v.04.apk`.

## Validation

- Android: 88 unit tests, lint with zero errors, debug compilation, and APK
  assembly passed.
- APK: package metadata confirms version `0.4.0` / code `4`; APK Signature
  Scheme v2 verification passed with the Android debug signer used for the
  personal installable build.
- Repository ESLint, TypeScript, production web build, and rendered HTML test
  passed.
- Production npm dependency audit found zero known vulnerabilities.
