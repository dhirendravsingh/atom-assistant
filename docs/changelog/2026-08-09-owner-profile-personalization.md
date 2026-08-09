# Owner profile personalization

Date: 2026-08-09

## Added

- Added an editable local profile in Settings with name, gender, and pronoun
  options.
- Added a live greeting preview and dynamic profile initials.
- Added gender-aware greetings while keeping inclusive choices free of an
  inferred honorific.
- Added Room persistence and tests for profile updates and the version 1 to 2
  migration.

## Changed

- Replaced the hardcoded `Dhiren Sir` UI value with the saved owner profile.
- Advanced the Room database to version 2 without clearing reminders.
- Kept profile data entirely on the device with automatically detected locale
  and timezone metadata.
