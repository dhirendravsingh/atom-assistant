# Notification policy

## Reliability model

The Android application is offline-first. Room is the only Phase 1 source of
truth on the phone. Railway PostgreSQL is deferred to an optional Phase 2
encrypted synchronization and backup copy. A network connection is not
required to create, edit, ring, snooze, complete, or cancel a reminder.

On first launch, Atom explains and then requests notification and microphone
runtime permissions in sequence. If exact-alarm access is missing, Atom opens
Android's Alarms & reminders special-access screen. The owner must still choose
Allow; Android does not permit Atom to grant these permissions to itself.
On Android 14 and newer, Atom also opens the full-screen alarm access screen so
Alarm Mode can wake the display and appear above the lock screen. Existing
installs receive this one-time setup after updating. Enabling Alarm Mode later
also opens the repair screen when this access is missing.
No storage permission is requested because Room uses Atom's private internal
application storage.

For each complete schedule Atom creates a local Android alarm:

- Use `AlarmManager.setExactAndAllowWhileIdle` when exact-alarm permission is
  available.
- Fall back to `setAndAllowWhileIdle` and show notification health as needing
  repair when exact-alarm access is unavailable.
- Use a unique stable alarm identifier derived from the reminder occurrence.
- Show a notification channel with sound, vibration, and lock-screen visibility.
- Alarm Mode launches a full-screen ringing screen above the lock screen and
  wakes the display until the reminder is dismissed, snoozed, or rescheduled.
- A background push can be used as a backup signal, never as the primary timer.

## Scheduling rules

- Unscheduled, `needs_date`, and `needs_time` reminders never create alarms.
- Editing cancels the previous alarm before scheduling the replacement.
- Cancelling removes the alarm and any pending backup push.
- Snoozing moves the current occurrence ten minutes into the future while
  preserving its recurrence rule.
- One-tap “Remind me again” creates a separate one-off occurrence one hour in
  the future. Natural-language remind-again commands can still choose another
  date or time.
- For recurrence, schedule only the nearest future occurrence; after it fires,
  calculate and register the next one from the RRULE.
- Hourly intervals such as `FREQ=HOURLY;INTERVAL=2` retain their original
  cadence: a late delivery or reboot skips elapsed occurrences and schedules
  the next future interval without drifting from the stored occurrence.
- Dedupe by occurrence ID so a local alarm and backup push cannot notify twice.

## Device lifecycle

Atom registers for device reboot, app upgrade, timezone change, clock change,
and locale change. On these events it reloads future reminders from Room and
reconciles Android alarms. Recurrences are recalculated in their stored IANA
timezone.

If a stored occurrence became due while the device was powered off or Atom was
otherwise unavailable, reconciliation classifies it as missed. Occurrences up
to 24 hours old receive one deduplicated “Missed reminder” notification with
Done, Snooze, and Remind Again actions. Older one-off reminders move to the
Missed list without creating a noisy late alert. Missed recurring reminders
may notify for the missed occurrence while Atom separately schedules their next
future occurrence. If notification access is blocked, Atom keeps a bounded,
deduplicated local backlog of missed occurrences and retries it after access is
restored; advancing a recurrence never silently discards its blocked alert.

The Settings screen reports notification permission, exact-alarm permission,
optional battery troubleshooting information, and the time of the last
successful reconciliation.
The status refreshes after returning from Android settings. Atom also reruns
reconciliation when exact-alarm access changes.

Battery-optimization exemption is not a permission requirement and is never
part of first-launch setup or notification health. It remains an optional
troubleshooting link for unusually aggressive device-manufacturer behavior.

## Delivery actions

Every scheduled notification and the real full-screen Alarm Mode offer Done,
Snooze 10 minutes, Remind in 1 hour, and Ignore. One-off reminders become completed
when Done is selected. For recurring reminders, delivery has already advanced
the stored next occurrence, so Done dismisses only the ringing occurrence.
Actions write their state locally before any future network sync.

Atom keeps a private on-device alarm activity history. It records when an alarm
rings and appends each later status change, including snoozed, reminded again,
completed, missed, opened, or ignored. The notification bell opens this history
with the newest event first and marks displayed entries as read. Android records
the alarm receiver directly. iOS records foreground deliveries immediately and
reconciles notifications still present in Notification Center when the app
becomes active, because iOS does not expose a universal background callback for
the instant a notification is displayed.

Selecting a history entry opens a dismissible detail view showing the original
scheduled time, the time the alarm rang, the recorded action, and any
replacement schedule created by Snooze or Remind Again. Ignored entries use a
soft red full-card treatment so they remain visually distinct in both themes.

Android cannot guarantee a ring after the owner force-stops the app, revokes
permission, powers off the device, or applies aggressive vendor battery
restrictions. Atom must surface these conditions honestly and provide a repair
action instead of claiming everything is healthy.
