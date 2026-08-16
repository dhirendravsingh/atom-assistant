# Android release signing

Atom release APKs use a private signing key that must never be committed to Git
or copied into the Android package. The canonical key is stored outside the
repository with owner-only filesystem permissions. Its password is stored in
the macOS login Keychain under service `com.dhiren.atom.release-keystore` and
account `atom-release`.

The release build reads these environment variables:

- `ATOM_RELEASE_KEYSTORE`
- `ATOM_RELEASE_STORE_PASSWORD`
- `ATOM_RELEASE_KEY_PASSWORD`
- `ATOM_RELEASE_KEY_ALIAS` (defaults to `atom-release`)

Before publishing Atom, make an encrypted offline backup of the keystore and
its password. Losing this key prevents future APKs from updating an installed
release. Never upload the key, password, or a signing-properties file to GitHub.

## Verification gate

Run all repository checks before producing a release:

```bash
npm run lint
npm run typecheck
npm test
npm audit
cd android
./gradlew lintDebug testDebugUnitTest assembleDebugAndroidTest
./gradlew --no-configuration-cache assembleRelease
```

Release signing deliberately disables Gradle's configuration cache so signing
credentials are not serialized into build cache state.

Verify the APK signature with Android SDK `apksigner verify --verbose
--print-certs app/build/outputs/apk/release/atom.v0.4.1.apk`. Installation and
real-device instrumentation tests require a USB-debugging-authorized Android
phone or emulator.
