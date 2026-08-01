# Privacy

## Scope

Atom is designed for one owner, Dhiren Sir. Phase 1 stores only the information
needed to operate reminders: owner profile, reminder text, schedule, recurrence,
delivery state, timezone, locale, and minimal diagnostic metadata.

## Local-first data

The Android Room database is the primary store. The app remains functional
without Railway. If sync is enabled, Railway PostgreSQL receives the minimum
structured reminder data required for backup and cross-install recovery.

Voice is transcribed through Android's on-device speech capability whenever the
device supports it. Raw audio is temporary and is deleted immediately after a
successful or abandoned transcription. Atom does not build a voiceprint.

## Optional OpenAI fallback

OpenAI fallback is off by default. Enabling it may send the spoken transcript or
short audio clip to a paid API and consumes API credits. Before first use, Atom
must explain the data sent, cost implication, and how to turn the fallback off.
The API key belongs on the Railway backend, never in the APK.

Model output is treated as an untrusted suggestion. The owner sees the parsed
result and confirms it before any reminder mutation.

## Security and retention

- Encrypt sensitive local storage using Android Keystore-backed keys.
- Use TLS for all Railway traffic.
- Authenticate the sole installation with revocable device credentials.
- Never place reminder text, audio, tokens, or API keys in analytics or crash
  logs.
- Redact reminder content from routine server logs.
- Support delete reminder, delete cloud backup, and reset all Atom data.
- Keep operational logs short-lived and document the retention period before
  production launch.

