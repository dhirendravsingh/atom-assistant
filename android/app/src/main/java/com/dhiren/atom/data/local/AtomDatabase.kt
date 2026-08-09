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
    ],
    version = 2,
    exportSchema = true,
)
abstract class AtomDatabase : RoomDatabase() {
    abstract fun ownerProfileDao(): OwnerProfileDao

    abstract fun reminderDao(): ReminderDao

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
                    .addMigrations(Migration1To2)
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
