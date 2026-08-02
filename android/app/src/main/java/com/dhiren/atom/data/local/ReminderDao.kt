package com.dhiren.atom.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query(
        """
        SELECT * FROM reminders
        ORDER BY
            CASE WHEN scheduled_at_utc IS NULL THEN 1 ELSE 0 END,
            scheduled_at_utc ASC,
            updated_at_utc DESC
        """,
    )
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminders WHERE state = 'Scheduled'")
    suspend fun getScheduled(): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
