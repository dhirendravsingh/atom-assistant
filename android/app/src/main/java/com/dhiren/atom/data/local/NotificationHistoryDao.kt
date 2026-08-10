package com.dhiren.atom.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Query(
        """
        SELECT * FROM notification_history
        ORDER BY occurred_at_utc DESC, id DESC
        """,
    )
    fun observeAll(): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT COUNT(*) FROM notification_history WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(event: NotificationHistoryEntity): Long

    @Query("UPDATE notification_history SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllRead()
}
