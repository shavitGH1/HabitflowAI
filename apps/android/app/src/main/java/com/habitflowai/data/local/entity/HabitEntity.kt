package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val frequency: String,
    val userId: String,
    val completed: Boolean
)
