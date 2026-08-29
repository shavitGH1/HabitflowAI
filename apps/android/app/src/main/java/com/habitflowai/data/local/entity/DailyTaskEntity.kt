package com.habitflowai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["habitId"]),
        Index(value = ["date"])
    ]
)
data class DailyTaskEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val habitId: String,
    val habitTitle: String,
    val date: String, // format: yyyy-MM-dd
    val description: String,
    val isCompleted: Boolean = false
)
