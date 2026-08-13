# Natural-language rules

## Parse contract

Every command is converted into a structured candidate before anything is
scheduled:

```text
intent                  create | reschedule | rename | cancel | snooze |
                        complete | remind_again | repeat
task                    required for create
scheduled_at_utc        nullable
local_date              nullable
local_time              nullable
timezone                required (IANA name)
recurrence_rule         nullable (RFC 5545 RRULE body)
source_text             required
confidence              0.0–1.0
needs_confirmation      boolean
```

`scheduled_at_utc` is derived only when local date, local time, AM/PM, and
timezone are all known. The original local values remain stored so timezone and
daylight-saving changes can be handled intentionally.

## Processing order

1. Normalize punctuation, whitespace, spoken AM/PM, and common transcription
   variants.
2. Strip an optional conversational prefix.
3. Detect the intent.
4. Extract relative schedule, recurrence, date, and time.
5. Treat the remaining meaningful phrase as the task.
6. Validate the schedule and ask once for missing information.
7. Show a confirmation card before writing or changing an alarm.

Relative phrases take precedence over absolute date fragments. A valid explicit
recurrence takes precedence over a one-off weekday date.

## Optional prefixes

The owner never needs to say “Atom” after pressing the microphone. These
openers are ignored when they appear at the start:

- Atom; Hey Atom; Hi Atom; Hello Atom; Okay Atom; OK Atom; Atom, please
- Remind me; Remind me again; Please remind me; Can/Could/Would you remind me
- Could/Would you please remind me
- I want you to remind me; I would like a reminder; I need a reminder
- Set/Give/Create/Schedule a reminder; Set an alert for; Schedule this for
- Help me remember; I must remember to; I have to remember to
- Don’t let me forget; Don’t forget to remind me
- Make sure I remember; Make sure I don’t forget
- Ping/Alert/Notify/Nudge me; Give me a heads-up
- Add this to my reminders; Put this on my reminder list
- Make a note to remind me; Keep this on my radar
- Remember that I need to; When it’s time; At that time remind me
- Let/Tell me when it’s time to; Wake me up to
- Do me a favor and remind me

Prefix stripping may run repeatedly up to three times to handle combinations
such as “Hey Atom, could you please remind me to…”.

## Date and time

- Visible and spoken times use 12-hour format.
- `12 AM` means midnight; `12 PM` means noon.
- A numeric time without AM/PM is incomplete unless the phrase contains an
  unambiguous word such as noon or midnight.
- Supported dates include today, tomorrow, day after tomorrow, weekdays, and
  explicit calendar dates.
- “In 20 minutes”, “in 2 hours”, and “in 3 days” are complete relative
  schedules and are resolved from the device clock at confirmation time.
- Past one-off times require confirmation or a new date; Atom does not silently
  roll them forward.

## Recurrence

Supported Phase 1 recurrence patterns:

| Phrase | Stored `recurrence_rule` |
| --- | --- |
| every day / daily | `FREQ=DAILY` |
| every 2 hours / every 2 hours a day | `FREQ=HOURLY;INTERVAL=2` |
| every weekday / weekdays | `FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR` |
| every Monday | `FREQ=WEEKLY;BYDAY=MO` |
| every week / weekly | `FREQ=WEEKLY` |
| every month / monthly | `FREQ=MONTHLY` |

Calendar recurrence still needs a local time. If it is missing, Atom marks the
reminder `needs_time` and asks once. An hourly interval recurrence starts one
interval from confirmation when no first time is provided, then saves and
schedules only its nearest occurrence. Intervals from 1 to 24 hours are
supported. “Stop repeating”, “don’t repeat”, “one time”, and “once” clear the
recurrence during an edit.

The common typed/transcription variant “evrey 2 hours” is normalized to the
same interval rule.

## Edit and action examples

- “Hey Atom, move this to tomorrow at 12 PM.”
- “Change the title to send the final proposal.”
- “Make this repeat every weekday at 9 AM.”
- “Stop repeating this.”
- “Cancel this reminder.”
- “Remind me again at 12 PM.”

If a command contains conflicting dates, times, or actions, Atom makes no
mutation and asks the owner to choose one interpretation.

## Deterministic action semantics

The Phase 1 parser runs entirely on-device and never sends command text to an
AI service. An action is written to Room only when it has no conflicts and no
required fields are missing.

| Intent | Context required | Room effect |
| --- | --- | --- |
| create | no | insert a new reminder |
| reschedule | yes | update the selected reminder's structured schedule |
| rename | yes | update its task while preserving its schedule |
| cancel | yes | retain the record and mark it `Canceled` |
| snooze | yes | move the selected reminder to the resolved future schedule |
| complete | yes | retain the record and mark it `Completed` |
| remind_again | yes | insert another reminder with the same task |
| repeat | yes | set or clear its recurrence rule |

An omitted date or time is preserved during an explicit edit. A newly created
reminder still asks once for either missing part. “Remind me again” can reuse
the selected reminder's date when a replacement time is given; relative forms
such as “remind me again in 20 minutes” resolve both fields from the device
clock.

The command parser does not schedule Android alarms itself. It produces the
validated local and UTC schedule that the alarm layer will consume. That alarm
layer must cancel an existing alarm before applying a reschedule or snooze.

## Conflict policy

Atom blocks the write and explains the issue when it finds:

- two distinct dates, times, relative schedules, recurrence rules, or actions;
- a relative schedule combined with an absolute schedule or recurrence;
- a one-off date combined with recurrence;
- a 24-hour time, an invalid 12-hour time, or a numeric time without AM/PM;
- a one-off schedule that is not in the future.

No ambiguous command is silently guessed or rolled forward.
