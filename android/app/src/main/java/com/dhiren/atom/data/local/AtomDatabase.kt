package com.dhiren.atom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Database(
    entities = [
        OwnerProfileEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
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
                    .addCallback(OwnerProfileCallback)
                    .build()
                    .also { instance = it }
            }

        private object OwnerProfileCallback : RoomDatabase.Callback() {
            override fun onCreate(database: SupportSQLiteDatabase) {
                super.onCreate(database)
                val now = Instant.now().toString()
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO owner_profile (
                        id, display_name, timezone, locale, created_at_utc, updated_at_utc
                    ) VALUES (1, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        "Dhiren Sir",
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
