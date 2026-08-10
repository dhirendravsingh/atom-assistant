package com.dhiren.atom.data

import com.dhiren.atom.data.local.NotificationHistoryDao
import com.dhiren.atom.data.local.NotificationHistoryEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHistoryRepositoryTest {
    private val now = Instant.parse("2026-08-10T08:30:00Z")

    @Test
    fun recordsAlarmEventsAndMarksThemRead() = runBlocking {
        val dao = FakeNotificationHistoryDao()
        val repository = NotificationHistoryRepository(
            dao = dao,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        repository.record(7L, "Call Mum", NotificationHistoryEventType.Rang, "Alarm rang")
        repository.record(
            7L,
            "Call Mum",
            NotificationHistoryEventType.Snoozed,
            "Snoozed for 10 minutes",
            now.plusSeconds(600),
        )

        val events = repository.events.first()
        assertEquals(listOf(NotificationHistoryEventType.Snoozed, NotificationHistoryEventType.Rang), events.map { it.eventType })
        assertEquals(now.plusSeconds(600), events.first().resultingScheduledAt)
        assertTrue(events.all { !it.isRead })
        assertEquals(2, repository.unreadCount.first())

        repository.markAllRead()

        assertEquals(0, repository.unreadCount.first())
        assertFalse(repository.events.first().any { !it.isRead })
    }
}

private class FakeNotificationHistoryDao : NotificationHistoryDao {
    private val rows = MutableStateFlow<List<NotificationHistoryEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<NotificationHistoryEntity>> = rows

    override fun observeUnreadCount(): Flow<Int> = rows.map { values -> values.count { !it.isRead } }

    override suspend fun insert(event: NotificationHistoryEntity): Long {
        val id = nextId++
        rows.value = listOf(event.copy(id = id)) + rows.value
        return id
    }

    override suspend fun markAllRead() {
        rows.value = rows.value.map { it.copy(isRead = true) }
    }
}
