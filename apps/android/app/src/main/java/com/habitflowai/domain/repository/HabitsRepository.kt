package com.habitflowai.domain.repository

import com.habitflowai.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

interface HabitsRepository {
    fun getHabits(userId: String): Flow<List<HabitEntity>>
    suspend fun createHabit(habit: HabitEntity)
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(habit: HabitEntity)
    suspend fun completeHabit(habit: HabitEntity): Boolean
}
