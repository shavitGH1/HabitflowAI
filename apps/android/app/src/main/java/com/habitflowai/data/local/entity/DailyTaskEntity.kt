package com.habitflowai.data.local.entity

import androidx.room.ColumnInfo
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
    val isCompleted: Boolean = false,
    // MIGRATION_13_14 adds this column via ALTER TABLE ... DEFAULT 'persona' (SQLite requires
    // a default for a NOT NULL column added to existing rows) - this annotation must match that
    // exactly or Room's migration validation fails for anyone upgrading through that migration.
    @ColumnInfo(defaultValue = "'persona'")
    val genre: String = "persona"
)
