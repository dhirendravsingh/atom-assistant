# Notification policy

## Reliability model

The Android application is offline-first. Room is the source of truth on the
phone; Railway PostgreSQL is an optional encrypted sync and backup copy. A
network connection is not required to create, edit, ring, snooze, complete, or
cancel a reminder.

For each complete schedule Atom creates a local Android alarm:

- Use `AlarmManager.setExactAndAllowWhileIdle` when exact-alarm permission is
  available.
- Use a unique stable alarm identifier derived from the reminder occurrence.
- Show a notification channel with sound, vibration, and lock-screen visibility.
- Alarm Mode may launch a full-screen ringing screen until dismissed or snoozed.
- A background push can be used as a backup signal, never as the primary timer.

## Scheduling rules

- Unscheduled, `needs_date`, and `needs_time` reminders never create alarms.
- Editing cancels the previous alarm before scheduling the replacement.
- Cancelling removes the alarm and any pending backup push.
- Snoozing creates a new one-off occurrence linked to the original reminder.
- “Remind me again” creates a new requested occurrence after the current alarm.
- For recurrence, schedule only the nearest future occurrence; after it fires,
  calculate and register the next one from the RRULE.
- Dedupe by occurrence ID so a local alarm and backup push cannot notify twice.

## Device lifecycle

Atom registers for device reboot, app upgrade, timezone change, clock change,
and locale change. On these events it reloads future reminders from Room and
reconciles Android alarms. Recurrences are recalculated in their stored IANA
timezone.

The Settings screen reports notification permission, exact-alarm permission,
battery optimization risk, and the time of the last successful reconciliation.

## Delivery actions

Every scheduled notification offers Done and Snooze. Alarm Mode additionally
offers Remind me again. Actions must write their state locally before attempting
network sync.

Android cannot guarantee a ring after the owner force-stops the app, revokes
permission, powers off the device, or applies aggressive vendor battery
restrictions. Atom must surface these conditions honestly and provide a repair
action instead of claiming everything is healthy.

