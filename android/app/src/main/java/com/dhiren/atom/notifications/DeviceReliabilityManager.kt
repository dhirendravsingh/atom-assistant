package com.dhiren.atom.notifications

import com.dhiren.atom.data.ReminderRepository
import com.dhiren.atom.data.NotificationHistoryEventType
import com.dhiren.atom.data.NotificationHistoryRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeviceReliabilityManager(
    private val reminderRepository: ReminderRepository,
    private val notificationCenter: AtomNotificationCenter,
    private val notificationHistoryRepository: NotificationHistoryRepository,
    private val preferences: ReliabilityPreferences,
    private val clock: Clock = Clock.systemUTC(),
    val monitor: DeviceReliabilityMonitor,
) {
    private val reconciliationMutex = Mutex()

    suspend fun reconcile(reason: AlarmReconciliationReason): AlarmReconciliationResult =
        reconciliationMutex.withLock {
            val result = reminderRepository.reconcileAlarms(reason)
            val now = Instant.now(clock)
            result.missedOccurrences.forEach(preferences::queueMissedOccurrence)
            preferences.pendingMissedOccurrences().forEach { occurrence ->
                val shouldNotify = MissedReminderPolicy.shouldNotify(occurrence.scheduledAt, now)
                when {
                    preferences.wasMissedOccurrenceReported(occurrence.occurrenceKey) -> {
                        if (!occurrence.recurring) {
                            reminderRepository.markMissed(occurrence.reminderId, occurrence.scheduledAt)
                        }
                        preferences.removePendingMissedOccurrence(occurrence.occurrenceKey)
                    }

                    shouldNotify &&
                        notificationCenter.showMissedReminder(
                            reminderId = occurrence.reminderId,
                            title = occurrence.title,
                            scheduledAt = occurrence.scheduledAt,
                    ) -> {
                        notificationHistoryRepository.record(
                            reminderId = occurrence.reminderId,
                            title = occurrence.title,
                            eventType = NotificationHistoryEventType.Missed,
                            detail = "Delivered after the device became available",
                        )
                        if (!occurrence.recurring) {
                            reminderRepository.markMissed(occurrence.reminderId, occurrence.scheduledAt)
                        }
                        preferences.markMissedOccurrenceReported(occurrence.occurrenceKey)
                        preferences.removePendingMissedOccurrence(occurrence.occurrenceKey)
                    }

                    !shouldNotify -> {
                        if (!occurrence.recurring) {
                            reminderRepository.markMissed(occurrence.reminderId, occurrence.scheduledAt)
                        }
                        preferences.removePendingMissedOccurrence(occurrence.occurrenceKey)
                    }
                }
            }
            preferences.recordSuccessfulReconciliation(reason, result)
            result
        }
}
