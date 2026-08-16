package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val goal: String?,
    val personaType: String?,
    val portfolioSummary: String? = null,
    val tips: String? = null,
    val failurePatterns: String? = null,
    val confidenceScore: Double? = null,
    val profilePicture: String? = null
)
