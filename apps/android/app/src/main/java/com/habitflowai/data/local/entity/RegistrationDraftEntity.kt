package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registration_drafts")
data class RegistrationDraftEntity(
    @PrimaryKey
    val email: String,
    val firstName: String,
    val lastName: String,
    val goal: String,
    val quizAnswers: List<String>,
    val savedAt: Long = System.currentTimeMillis()
)
