package com.dhiren.atom.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owner_profile")
data class OwnerProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val timezone: String,
    val locale: String,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: String,
    @ColumnInfo(name = "updated_at_utc")
    val updatedAtUtc: String,
)
