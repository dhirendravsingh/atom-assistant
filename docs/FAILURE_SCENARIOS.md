# Failure scenarios

| Scenario | Required behavior |
| --- | --- |
| Microphone permission denied | Keep text entry fully usable and show a button that opens Atom's Android app settings |
| Speech unavailable offline | Explain that the recognizer or language pack is unavailable; preserve typed text and keep all text fields usable |
| Speech recognition fails after a partial result | Restore the field's value from before listening so partial output never overwrites typed text |
| Empty or low-confidence transcript | Keep the editable transcript and ask the owner to retry or type |
| Date or time missing | Ask once with mic and text; allow Save to Unscheduled |
| AM/PM missing | Mark time incomplete and require AM or PM |
| Conflicting schedule phrases | Make no mutation; show both interpretations for owner confirmation |
| Date/time is in the past | Ask for a future schedule; never silently roll forward |
| Invalid recurrence | Preserve the task as unscheduled and ask for a supported repeat pattern |
| Notification permission denied | Save the reminder, mark delivery unhealthy, and prompt for permission |
| Exact alarms unavailable | Explain reduced precision and offer the Android permission screen |
| Device reboot or app upgrade | Reconcile all future alarms from Room |
| Timezone or clock changes | Recompute future occurrences from stored local values and IANA timezone |
| Device is off at trigger time | Show a missed-reminder notification after boot if still relevant |
| App is force-stopped | Show an explicit reliability warning after next launch |
| Railway is offline | Continue locally and queue idempotent sync operations |
| Sync conflict | Prefer the latest owner-confirmed mutation; retain an audit entry |
| Local alarm and push both arrive | Dedupe by occurrence ID and display only once |
| Edit scheduling fails | Restore or retry the prior alarm and mark the reminder as needing attention |
| Recurrence calculation fails | Stop scheduling future occurrences, preserve the rule, and request repair |
| OpenAI fallback has no credit/key | Return to deterministic parsing without losing the transcript |
| API or transcript timeout | Cancel the request safely and let the owner retry |

No failure may discard a captured task silently. Atom must always leave the
owner with either a confirmed schedule, an Unscheduled reminder, or an explicit
unchanged state.
