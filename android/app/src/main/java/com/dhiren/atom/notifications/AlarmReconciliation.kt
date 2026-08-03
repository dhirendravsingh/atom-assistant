package com.dhiren.atom.notifications

import java.time.Duration
import java.time.Instant
import java.nio.charset.StandardCharsets
import java.util.Base64

enum class AlarmReconciliationReason(
    val displayLabel: String,
    val recalculateRecurringSchedules: Boolean = false,
) {
    AppStart("app launch"),
    Boot("device reboot"),
    AppUpdate("app update"),
    ClockChanged("clock change", recalculateRecurringSchedules = true),
    TimezoneChanged("timezone change", recalculateRecurringSchedules = true),
    LocaleChanged("locale change"),
    ExactAlarmPermissionChanged("alarm permission change"),
    DeliveryRecovery("blocked delivery recovery"),
}

data class MissedReminderOccurrence(
    val reminderId: Long,
    val title: String,
    val scheduledAt: Instant,
    val recurring: Boolean,
) {
    val occurrenceKey: String = "$reminderId@${scheduledAt.toEpochMilli()}"
}

data class AlarmReconciliationResult(
    val completedAt: Instant,
    val scheduledAlarmCount: Int,
    val missedOccurrences: List<MissedReminderOccurrence>,
)

object MissedReminderPolicy {
    val NotificationWindow: Duration = Duration.ofHours(24)

    fun shouldNotify(scheduledAt: Instant, now: Instant): Boolean {
        if (scheduledAt.isAfter(now)) return false
        return Duration.between(scheduledAt, now) <= NotificationWindow
    }
}

object MissedReminderOccurrenceCodec {
    fun encode(occurrence: MissedReminderOccurrence): String {
        val encodedTitle = Base64.getUrlEncoder().withoutPadding().encodeToString(
            occurrence.title.toByteArray(StandardCharsets.UTF_8),
        )
        return listOf(
            occurrence.reminderId,
            occurrence.scheduledAt.toEpochMilli(),
            occurrence.recurring,
            encodedTitle,
        ).joinToString("|")
    }

    fun decode(value: String): MissedReminderOccurrence? {
        val parts = value.split('|', limit = 4)
        if (parts.size != 4) return null
        return runCatching {
            MissedReminderOccurrence(
                reminderId = parts[0].toLong(),
                scheduledAt = Instant.ofEpochMilli(parts[1].toLong()),
                recurring = parts[2].toBooleanStrict(),
                title = String(Base64.getUrlDecoder().decode(parts[3]), StandardCharsets.UTF_8),
            )
        }.getOrNull()
    }
}
