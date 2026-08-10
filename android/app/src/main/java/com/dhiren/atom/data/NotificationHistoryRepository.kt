package com.dhiren.atom.data

import com.dhiren.atom.data.local.NotificationHistoryDao
import com.dhiren.atom.data.local.NotificationHistoryEntity
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class NotificationHistoryEventType {
    Rang,
    Missed,
    Opened,
    Snoozed,
    RemindedAgain,
    Completed,
    Ignored,
}

data class NotificationHistoryUi(
    val id: Long,
    val reminderId: Long,
    val title: String,
    val eventType: NotificationHistoryEventType,
    val detail: String?,
    val resultingScheduledAt: Instant?,
    val occurredAt: Instant,
    val isRead: Boolean,
)

class NotificationHistoryRepository(
    private val dao: NotificationHistoryDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    val events: Flow<List<NotificationHistoryUi>> = dao.observeAll().map { rows ->
        rows.mapNotNull(NotificationHistoryEntity::toUi)
    }
    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    suspend fun record(
        reminderId: Long,
        title: String,
        eventType: NotificationHistoryEventType,
        detail: String? = null,
        resultingScheduledAt: Instant? = null,
    ) {
        dao.insert(
            NotificationHistoryEntity(
                reminderId = reminderId,
                title = title.ifBlank { "Reminder" },
                eventType = eventType.name,
                detail = detail,
                resultingScheduledAtUtc = resultingScheduledAt?.toString(),
                occurredAtUtc = Instant.now(clock).toString(),
            ),
        )
    }

    suspend fun markAllRead() = dao.markAllRead()
}

private fun NotificationHistoryEntity.toUi(): NotificationHistoryUi? {
    val parsedType = runCatching { NotificationHistoryEventType.valueOf(eventType) }.getOrNull() ?: return null
    val parsedOccurredAt = runCatching { Instant.parse(occurredAtUtc) }.getOrNull() ?: return null
    return NotificationHistoryUi(
        id = id,
        reminderId = reminderId,
        title = title,
        eventType = parsedType,
        detail = detail,
        resultingScheduledAt = resultingScheduledAtUtc?.let { runCatching { Instant.parse(it) }.getOrNull() },
        occurredAt = parsedOccurredAt,
        isRead = isRead,
    )
}
