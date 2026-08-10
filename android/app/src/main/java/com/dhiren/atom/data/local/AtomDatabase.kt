package com.dhiren.atom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Database(
    entities = [
        OwnerProfileEntity::class,
        ReminderEntity::class,
        NotificationHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AtomDatabase : RoomDatabase() {
    abstract fun ownerProfileDao(): OwnerProfileDao

    abstract fun reminderDao(): ReminderDao

    abstract fun notificationHistoryDao(): NotificationHistoryDao

    companion object {
        private const val DatabaseName = "atom.db"

        @Volatile
        private var instance: AtomDatabase? = null

        fun getInstance(context: Context): AtomDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtomDatabase::class.java,
                    DatabaseName,
                )
                    .addMigrations(Migration1To2, Migration2To3)
                    .addCallback(OwnerProfileCallback)
                    .build()
                    .also { instance = it }
            }

        private object OwnerProfileCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = Instant.now().toString()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO owner_profile (
                        id, display_name, gender, pronouns, timezone, locale, created_at_utc, updated_at_utc
                    ) VALUES (1, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        "Dhiren",
                        "Man",
                        "HeHim",
                        ZoneId.systemDefault().id,
                        Locale.getDefault().toLanguageTag(),
                        now,
                        now,
                    ),
                )
            }
        }
    }
}

internal val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE owner_profile ADD COLUMN gender TEXT NOT NULL DEFAULT 'Man'",
        )
        db.execSQL(
            "ALTER TABLE owner_profile ADD COLUMN pronouns TEXT NOT NULL DEFAULT 'HeHim'",
        )
        db.execSQL(
            "UPDATE owner_profile SET display_name = 'Dhiren' WHERE TRIM(display_name) = 'Dhiren Sir'",
        )
    }
}

internal val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notification_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                reminder_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                event_type TEXT NOT NULL,
                detail TEXT,
                resulting_scheduled_at_utc TEXT,
                occurred_at_utc TEXT NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_notification_history_reminder_id ON notification_history(reminder_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_notification_history_occurred_at_utc ON notification_history(occurred_at_utc)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_notification_history_is_read ON notification_history(is_read)",
        )
    }
}
