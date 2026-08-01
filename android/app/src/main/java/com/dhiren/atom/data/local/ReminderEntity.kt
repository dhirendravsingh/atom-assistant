package com.dhiren.atom.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["scheduled_at_utc"]),
        Index(value = ["state"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "source_text")
    val sourceText: String,
    @ColumnInfo(name = "scheduled_at_utc")
    val scheduledAtUtc: String?,
    @ColumnInfo(name = "local_date")
    val localDate: String?,
    @ColumnInfo(name = "local_time")
    val localTime: String?,
    val timezone: String,
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String?,
    val source: String,
    val state: String,
    val accent: String,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: String,
    @ColumnInfo(name = "updated_at_utc")
    val updatedAtUtc: String,
)
