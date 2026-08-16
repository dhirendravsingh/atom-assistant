# Privacy

## Scope

Atom is designed for one owner. Phase 1 stores only the information needed to
operate and personalize reminders: the chosen name, gender option, pronoun
option, reminder text, schedule, recurrence, delivery state, timezone, locale,
and minimal diagnostic metadata. Gender and pronouns may be set to “Prefer not
to say.”

## Local-first data

The Android Room database is the only data store in Phase 1. No profile or
reminder or notification-history data is sent to Railway or PostgreSQL. If the owner explicitly enables
the optional Phase 2 synchronization feature in the future, Railway PostgreSQL
may receive the minimum structured profile and reminder data required for
backup and recovery while Room remains the operational source of truth.

Room writes `atom.db` inside Atom's private internal application storage. Atom
does not request permission to read photos, shared files, media, or external
storage.

Alarm activity contains the reminder title, delivery/action status, timestamps,
and an optional replacement schedule. Android stores it in Room and iOS stores
it in SwiftData. It stays inside the respective app container and requires no
additional permission.

Voice is transcribed through Android's speech-recognition service. Atom uses a
dedicated on-device recognizer when Android provides one. If that recognizer or
its selected language is unavailable, Atom offers Android's standard speech
service as a fallback. Before each online fallback, Atom explains that the
phone's recognition provider may receive the microphone recording and waits
for the owner to explicitly choose “Use online speech.” Atom does not send this
audio to OpenAI or store it.

Atom never stores raw microphone audio and does not build a voiceprint. It keeps
only the editable transcript after recognition. If online speech is declined,
or neither Android recognizer can complete, typed entry remains available and
no OpenAI fallback is enabled automatically.

## Optional OpenAI fallback

OpenAI fallback is off by default. Enabling it may send the spoken transcript or
short audio clip to a paid API and consumes API credits. Before first use, Atom
must explain the data sent, cost implication, and how to turn the fallback off.
If the fallback is implemented in Phase 2, its API key belongs on the Railway
backend, never in the APK.

Model output is treated as an untrusted suggestion. The owner sees the parsed
result and confirms it before any reminder mutation.

## Security and retention

- Encrypt sensitive local storage using Android Keystore-backed keys.
- Use TLS for all optional Phase 2 Railway traffic.
- Authenticate the sole installation with revocable device credentials.
- Never place reminder text, audio, tokens, or API keys in analytics or crash
  logs.
- Redact reminder content from routine server logs.
- Support delete reminder, delete cloud backup, and reset all Atom data.
- Keep operational logs short-lived and document the retention period before
  production launch.
