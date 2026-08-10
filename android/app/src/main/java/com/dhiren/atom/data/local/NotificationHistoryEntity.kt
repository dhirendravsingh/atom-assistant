package com.dhiren.atom.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_history",
    indices = [
        Index(value = ["reminder_id"]),
        Index(value = ["occurred_at_utc"]),
        Index(value = ["is_read"]),
    ],
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "reminder_id")
    val reminderId: Long,
    val title: String,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    val detail: String?,
    @ColumnInfo(name = "resulting_scheduled_at_utc")
    val resultingScheduledAtUtc: String?,
    @ColumnInfo(name = "occurred_at_utc")
    val occurredAtUtc: String,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
)
