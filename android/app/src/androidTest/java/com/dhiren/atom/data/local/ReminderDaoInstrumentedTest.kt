package com.dhiren.atom.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoInstrumentedTest {
    private lateinit var database: AtomDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AtomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.reminderDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun scheduledReminderSurvivesRoomRoundTripAndEdit() = runBlocking {
        val insertedId = dao.upsert(reminder(title = "Call the dentist"))
        val inserted = requireNotNull(dao.getById(insertedId))

        assertEquals("2026-08-05T10:30:00Z", inserted.scheduledAtUtc)
        assertEquals("Scheduled", inserted.state)

        dao.upsert(
            inserted.copy(
                title = "Call Dr Shah",
                scheduledAtUtc = "2026-08-05T11:30:00Z",
                localTime = "17:00",
                updatedAtUtc = "2026-08-05T09:05:00Z",
            ),
        )

        val edited = requireNotNull(dao.getById(insertedId))
        assertEquals("Call Dr Shah", edited.title)
        assertEquals("2026-08-05T11:30:00Z", edited.scheduledAtUtc)
        assertEquals(1, dao.getScheduled().size)
    }

    @Test
    fun deletingReminderRemovesItFromRoom() = runBlocking {
        val insertedId = dao.upsert(reminder(title = "Temporary reminder"))

        dao.deleteById(insertedId)

        assertNull(dao.getById(insertedId))
    }

    private fun reminder(title: String) = ReminderEntity(
        title = title,
        sourceText = "Remind me to $title",
        scheduledAtUtc = "2026-08-05T10:30:00Z",
        localDate = "2026-08-05",
        localTime = "16:00",
        timezone = "Asia/Kolkata",
        recurrenceRule = null,
        source = "Text",
        state = "Scheduled",
        accent = "Mint",
        createdAtUtc = Instant.parse("2026-08-05T09:00:00Z").toString(),
        updatedAtUtc = Instant.parse("2026-08-05T09:00:00Z").toString(),
    )
}
