package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [Index(value = ["serverId"], unique = true)]
)
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val frequency: String,
    val userId: String,
    val completed: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverId: String? = null,
    val goalId: String? = null,
    val completionHistory: List<String> = emptyList(),
    val relevanceWarning: String? = null,
    val verificationWarning: String? = null
)
