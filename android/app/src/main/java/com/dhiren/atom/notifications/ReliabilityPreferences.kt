package com.dhiren.atom.notifications

import android.content.Context
import java.time.Instant

class ReliabilityPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "atom_device_reliability",
        Context.MODE_PRIVATE,
    )

    val lastSuccessfulReconciliation: Instant?
        get() = preferences.getLong(LastReconciliationEpochMillisKey, MissingEpochMillis)
            .takeIf { it != MissingEpochMillis }
            ?.let(Instant::ofEpochMilli)

    val lastReconciliationReason: AlarmReconciliationReason?
        get() = preferences.getString(LastReconciliationReasonKey, null)
            ?.let { stored -> AlarmReconciliationReason.entries.firstOrNull { it.name == stored } }

    val lastScheduledAlarmCount: Int
        get() = preferences.getInt(LastScheduledAlarmCountKey, 0)

    val lastMissedReminderCount: Int
        get() = preferences.getInt(LastMissedReminderCountKey, 0)

    fun recordSuccessfulReconciliation(
        reason: AlarmReconciliationReason,
        result: AlarmReconciliationResult,
    ) {
        preferences.edit()
            .putLong(LastReconciliationEpochMillisKey, result.completedAt.toEpochMilli())
            .putString(LastReconciliationReasonKey, reason.name)
            .putInt(LastScheduledAlarmCountKey, result.scheduledAlarmCount)
            .putInt(LastMissedReminderCountKey, result.missedOccurrences.size)
            .apply()
    }

    fun wasMissedOccurrenceReported(occurrenceKey: String): Boolean =
        occurrenceKey in preferences.getStringSet(ReportedMissedOccurrencesKey, emptySet()).orEmpty()

    fun markMissedOccurrenceReported(occurrenceKey: String) {
        val retained = (
            preferences.getStringSet(ReportedMissedOccurrencesKey, emptySet()).orEmpty() + occurrenceKey
            )
            .sortedByDescending(::occurrenceEpochMillis)
            .take(MaxReportedMissedOccurrences)
            .toSet()
        preferences.edit().putStringSet(ReportedMissedOccurrencesKey, retained).apply()
    }

    fun pendingMissedOccurrences(): List<MissedReminderOccurrence> =
        preferences.getStringSet(PendingMissedOccurrencesKey, emptySet()).orEmpty()
            .mapNotNull(MissedReminderOccurrenceCodec::decode)
            .sortedBy { it.scheduledAt }

    fun queueMissedOccurrence(occurrence: MissedReminderOccurrence) {
        if (wasMissedOccurrenceReported(occurrence.occurrenceKey)) return
        val pending = (
            pendingMissedOccurrences().filterNot { it.occurrenceKey == occurrence.occurrenceKey } + occurrence
            )
            .sortedByDescending { it.scheduledAt }
            .take(MaxPendingMissedOccurrences)
            .map(MissedReminderOccurrenceCodec::encode)
            .toSet()
        preferences.edit().putStringSet(PendingMissedOccurrencesKey, pending).apply()
    }

    fun removePendingMissedOccurrence(occurrenceKey: String) {
        val pending = pendingMissedOccurrences()
            .filterNot { it.occurrenceKey == occurrenceKey }
            .map(MissedReminderOccurrenceCodec::encode)
            .toSet()
        preferences.edit().putStringSet(PendingMissedOccurrencesKey, pending).apply()
    }

    private fun occurrenceEpochMillis(key: String): Long = key.substringAfterLast('@').toLongOrNull() ?: 0L

    private companion object {
        const val MissingEpochMillis = Long.MIN_VALUE
        const val MaxReportedMissedOccurrences = 256
        const val MaxPendingMissedOccurrences = 64
        const val LastReconciliationEpochMillisKey = "last_reconciliation_epoch_millis"
        const val LastReconciliationReasonKey = "last_reconciliation_reason"
        const val LastScheduledAlarmCountKey = "last_scheduled_alarm_count"
        const val LastMissedReminderCountKey = "last_missed_reminder_count"
        const val ReportedMissedOccurrencesKey = "reported_missed_occurrences"
        const val PendingMissedOccurrencesKey = "pending_missed_occurrences"
    }
}
