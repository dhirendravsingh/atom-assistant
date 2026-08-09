# atom-ios

`atom-ios` is Atom's separate native SwiftUI application for iPhone and iPad.
It does not replace or wrap the Android application. Both applications keep
their own offline reminder store and use the operating system's native speech,
permission, and reminder-delivery APIs.

## Platform behavior

- SwiftUI interface with the same calm Atom visual language.
- SwiftData stores the owner profile and reminders in Atom's private app
  container. No file, photo, media, or storage permission is requested.
- User Notifications schedules local reminders without a server or APNs.
- Notifications, microphone, and speech recognition are requested through an
  iOS-specific setup flow.
- The microphone listens only after the owner taps it.
- Notification actions include Done, Snooze 10 minutes, and Remind in 1 hour.
- iOS 17 is the minimum supported release.

Standard local notifications are the compatible delivery mechanism on iOS
17–25. An iOS 26 AlarmKit adapter is the next release-hardening step because it
requires the iOS 26 SDK and full Xcode for compilation and device validation.

## Generate and open the Xcode project

Install the full Xcode application and XcodeGen 2.46 or newer, then run:

```bash
cd atom-ios
xcodegen generate
open atom-ios.xcodeproj
```

Select your Apple development team in **Signing & Capabilities**, choose an
iPhone or simulator, and run the `atom-ios` scheme.

## Checks

The platform-neutral parser and permission plan can be tested without Xcode:

```bash
cd atom-ios
swift run AtomCoreChecks
swift format lint --recursive Sources Tests Checks AtomIOS AtomIOSTests
```

With full Xcode installed, also run the XCTest/Swift Testing suite using
`swift test` before compiling the application target.

The SwiftUI application itself requires the full Xcode iOS SDK:

```bash
xcodebuild \
  -project atom-ios.xcodeproj \
  -scheme atom-ios \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  test
```

A free Personal Team can install the app for short personal testing. Persistent
TestFlight or App Store distribution requires Apple Developer Program signing.
