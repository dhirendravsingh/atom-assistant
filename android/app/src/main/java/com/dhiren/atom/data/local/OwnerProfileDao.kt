package com.dhiren.atom.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerProfileDao {
    @Query("SELECT * FROM owner_profile WHERE id = 1")
    fun observeOwner(): Flow<OwnerProfileEntity?>
}
