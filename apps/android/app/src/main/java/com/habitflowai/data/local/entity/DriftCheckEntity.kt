package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drift_checks")
data class DriftCheckEntity(
    @PrimaryKey
    val id: String,
    val driftDetected: Boolean,
    val driftScore: Double,
    val newSuggestedPersona: String?,
    val rationale: String?,
    val checkedAt: Long = System.currentTimeMillis()
)
