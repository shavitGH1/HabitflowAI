package com.habitflowai.data.model

import java.time.LocalDate

enum class HabitFrequency {
    DAILY, WEEKLY, MONTHLY
}

data class Habit(
    val id: String,
    val title: String,
    val description: String,
    val frequency: HabitFrequency,
    val createdAt: LocalDate = LocalDate.now(),
    val completionHistory: List<LocalDate> = emptyList()
)
