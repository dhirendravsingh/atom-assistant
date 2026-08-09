package com.dhiren.atom.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerProfileMigrationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "owner-profile-migration-test"

    @Before
    fun prepare() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun versionOneOwnerMigratesWithoutLosingProfileOrReminderData() = runBlocking {
        createVersionOneDatabase()

        val migrated = Room.databaseBuilder(context, AtomDatabase::class.java, databaseName)
            .addMigrations(Migration1To2)
            .allowMainThreadQueries()
            .build()
        try {
            val owner = requireNotNull(migrated.ownerProfileDao().getOwner())
            assertEquals("Dhiren", owner.displayName)
            assertEquals("Man", owner.gender)
            assertEquals("HeHim", owner.pronouns)
            assertEquals("Keep this reminder", migrated.reminderDao().getById(1L)?.title)
        } finally {
            migrated.close()
        }
    }

    private fun createVersionOneDatabase() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS owner_profile (
                                    id INTEGER NOT NULL,
                                    display_name TEXT NOT NULL,
                                    timezone TEXT NOT NULL,
                                    locale TEXT NOT NULL,
                                    created_at_utc TEXT NOT NULL,
                                    updated_at_utc TEXT NOT NULL,
                                    PRIMARY KEY(id)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS reminders (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    title TEXT NOT NULL,
                                    source_text TEXT NOT NULL,
                                    scheduled_at_utc TEXT,
                                    local_date TEXT,
                                    local_time TEXT,
                                    timezone TEXT NOT NULL,
                                    recurrence_rule TEXT,
                                    source TEXT NOT NULL,
                                    state TEXT NOT NULL,
                                    accent TEXT NOT NULL,
                                    created_at_utc TEXT NOT NULL,
                                    updated_at_utc TEXT NOT NULL
                                )
                                """.trimIndent(),
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_scheduled_at_utc ON reminders(scheduled_at_utc)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_state ON reminders(state)")
                            db.execSQL(
                                """
                                INSERT INTO owner_profile (
                                    id, display_name, timezone, locale, created_at_utc, updated_at_utc
                                ) VALUES (1, 'Dhiren Sir', 'Asia/Kolkata', 'en-IN',
                                    '2026-08-01T00:00:00Z', '2026-08-01T00:00:00Z')
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                INSERT INTO reminders (
                                    id, title, source_text, scheduled_at_utc, local_date,
                                    local_time, timezone, recurrence_rule, source, state,
                                    accent, created_at_utc, updated_at_utc
                                ) VALUES (1, 'Keep this reminder', 'Keep this reminder',
                                    '2026-08-10T04:30:00Z', '2026-08-10', '10:00',
                                    'Asia/Kolkata', NULL, 'Text', 'Scheduled', 'Mint',
                                    '2026-08-01T00:00:00Z', '2026-08-01T00:00:00Z')
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        helper.writableDatabase
        helper.close()
    }
}
