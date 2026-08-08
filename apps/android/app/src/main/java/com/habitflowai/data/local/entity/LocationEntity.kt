package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val habitId: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val taskTitle: String? = null,
    val placeName: String? = null,
    val address: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE
)
