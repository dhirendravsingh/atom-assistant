package com.dhiren.atom.data

import com.dhiren.atom.data.local.ReminderDao
import com.dhiren.atom.data.local.ReminderEntity
import com.dhiren.atom.notifications.ReminderAlarmScheduler
import com.dhiren.atom.notifications.AlarmReconciliationReason
import com.dhiren.atom.ui.ReminderAccent
import com.dhiren.atom.ui.ReminderState
import com.dhiren.atom.ui.ReminderUi
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRepositoryAlarmTest {
    private val now = Instant.parse("2026-08-01T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val timezone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `reschedule cancels old alarm before scheduling replacement`() = runBlocking {
        val existing = entity(id = 7L, scheduledAtUtc = "2026-08-01T12:10:00Z")
        val dao = FakeReminderDao(existing)
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        repository.save(
            reminder(
                id = 7L,
                date = "Tomorrow",
                time = "6:30 PM",
            ),
        )

        assertEquals(listOf("cancel:7", "schedule:7:2026-08-02T13:00:00Z"), scheduler.events)
    }

    @Test
    fun `delete cancels alarm before deleting room row`() = runBlocking {
        val dao = FakeReminderDao(entity(id = 9L, scheduledAtUtc = "2026-08-01T12:10:00Z"))
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        repository.delete(9L)

        assertEquals(listOf("cancel:9"), scheduler.events)
        assertTrue(dao.entities.value.isEmpty())
    }

    @Test
    fun `snooze stores and schedules the requested future instant`() = runBlocking {
        val dao = FakeReminderDao(entity(id = 11L, scheduledAtUtc = "2026-08-01T12:00:00Z"))
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        repository.snooze(11L, Duration.ofMinutes(10))

        assertEquals("2026-08-01T12:10:00Z", dao.entities.value.single().scheduledAtUtc)
        assertEquals(listOf("cancel:11", "schedule:11:2026-08-01T12:10:00Z"), scheduler.events)
    }

    @Test
    fun `remind again creates a separate occurrence and completes one off original`() = runBlocking {
        val dao = FakeReminderDao(entity(id = 13L, scheduledAtUtc = "2026-08-01T12:00:00Z"))
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        val newId = repository.remindAgain(13L, Duration.ofHours(1))

        assertEquals(14L, newId)
        assertEquals("Completed", dao.entities.value.first { it.id == 13L }.state)
        assertEquals("2026-08-01T13:00:00Z", dao.entities.value.first { it.id == 14L }.scheduledAtUtc)
        assertEquals(listOf("schedule:14:2026-08-01T13:00:00Z", "cancel:13"), scheduler.events)
    }

    @Test
    fun `reboot reconciliation restores a future alarm`() = runBlocking {
        val dao = FakeReminderDao(entity(id = 17L, scheduledAtUtc = "2026-08-01T12:20:00Z"))
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        val result = repository.reconcileAlarms(AlarmReconciliationReason.Boot)

        assertEquals(1, result.scheduledAlarmCount)
        assertTrue(result.missedOccurrences.isEmpty())
        assertEquals(listOf("cancel:17", "schedule:17:2026-08-01T12:20:00Z"), scheduler.events)
    }

    @Test
    fun `reconciliation reports a missed one off without scheduling it in the past`() = runBlocking {
        val dao = FakeReminderDao(entity(id = 19L, scheduledAtUtc = "2026-08-01T11:50:00Z"))
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        val result = repository.reconcileAlarms(AlarmReconciliationReason.AppStart)

        assertEquals(0, result.scheduledAlarmCount)
        assertEquals(19L, result.missedOccurrences.single().reminderId)
        assertEquals(Instant.parse("2026-08-01T11:50:00Z"), result.missedOccurrences.single().scheduledAt)
        assertEquals(listOf("cancel:19"), scheduler.events)
    }

    @Test
    fun `missed recurring occurrence is reported and advanced to the next future alarm`() = runBlocking {
        val dao = FakeReminderDao(
            entity(
                id = 21L,
                scheduledAtUtc = "2026-08-01T11:50:00Z",
                localTime = "09:00",
                recurrenceRule = "FREQ=DAILY",
            ),
        )
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        val result = repository.reconcileAlarms(AlarmReconciliationReason.Boot)

        assertTrue(result.missedOccurrences.single().recurring)
        assertEquals("2026-08-02T03:30:00Z", dao.entities.value.single().scheduledAtUtc)
        assertEquals(listOf("cancel:21", "schedule:21:2026-08-02T03:30:00Z"), scheduler.events)
    }

    @Test
    fun `clock changes recalculate a future recurring occurrence from local time`() = runBlocking {
        val dao = FakeReminderDao(
            entity(
                id = 23L,
                scheduledAtUtc = "2026-08-03T03:30:00Z",
                localTime = "09:00",
                recurrenceRule = "FREQ=DAILY",
            ),
        )
        val scheduler = RecordingScheduler()
        val repository = ReminderRepository(dao, clock, { timezone }, scheduler)

        repository.reconcileAlarms(AlarmReconciliationReason.ClockChanged)

        assertEquals("2026-08-02T03:30:00Z", dao.entities.value.single().scheduledAtUtc)
        assertEquals(listOf("cancel:23", "schedule:23:2026-08-02T03:30:00Z"), scheduler.events)
    }

    @Test
    fun `mark missed changes only the expected one off occurrence`() = runBlocking {
        val dueAt = Instant.parse("2026-08-01T11:50:00Z")
        val dao = FakeReminderDao(entity(id = 25L, scheduledAtUtc = dueAt.toString()))
        val repository = ReminderRepository(dao, clock, { timezone }, RecordingScheduler())

        repository.markMissed(25L, dueAt)

        assertEquals("Missed", dao.entities.value.single().state)
        assertEquals(dueAt.toString(), dao.entities.value.single().scheduledAtUtc)
    }

    private fun reminder(id: Long, date: String?, time: String?) = ReminderUi(
        id = id,
        title = "Review priorities",
        date = date,
        time = time,
        source = "Text",
        state = ReminderState.Scheduled,
        accent = ReminderAccent.Mint,
        sourceText = "Remind me to review priorities",
        timezone = timezone.id,
    )

    private fun entity(
        id: Long,
        scheduledAtUtc: String?,
        localTime: String = "17:30",
        recurrenceRule: String? = null,
    ) = ReminderEntity(
        id = id,
        title = "Review priorities",
        sourceText = "Remind me to review priorities",
        scheduledAtUtc = scheduledAtUtc,
        localDate = "2026-08-01",
        localTime = localTime,
        timezone = timezone.id,
        recurrenceRule = recurrenceRule,
        source = "Text",
        state = "Scheduled",
        accent = "Mint",
        createdAtUtc = now.toString(),
        updatedAtUtc = now.toString(),
    )
}

private class RecordingScheduler : ReminderAlarmScheduler {
    val events = mutableListOf<String>()

    override fun schedule(reminderId: Long, title: String, triggerAt: Instant) {
        events += "schedule:$reminderId:$triggerAt"
    }

    override fun cancel(reminderId: Long) {
        events += "cancel:$reminderId"
    }
}

private class FakeReminderDao(vararg initial: ReminderEntity) : ReminderDao {
    val entities = MutableStateFlow(initial.toList())

    override fun observeAll(): Flow<List<ReminderEntity>> = entities

    override suspend fun getById(id: Long): ReminderEntity? = entities.value.firstOrNull { it.id == id }

    override suspend fun upsert(reminder: ReminderEntity): Long {
        val id = reminder.id.takeIf { it != 0L } ?: ((entities.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        entities.value = entities.value.filterNot { it.id == id } + reminder.copy(id = id)
        return id
    }

    override suspend fun getScheduled(): List<ReminderEntity> = entities.value.filter { it.state == "Scheduled" }

    override suspend fun deleteById(id: Long) {
        entities.value = entities.value.filterNot { it.id == id }
    }
}
