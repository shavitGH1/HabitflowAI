package com.habitflowai.domain.repository

import com.habitflowai.data.local.entity.DailyTaskEntity
import com.habitflowai.data.model.HomeResponse
import kotlinx.coroutines.flow.Flow

interface GoalsRepository {
    suspend fun fetchGoals(userId: String, dayOfWeek: Int): List<Pair<String, Int>>
    suspend fun getHomeData(force: Boolean = false): HomeResponse
    suspend fun completeTask(taskId: String): Boolean
    suspend fun getActiveGoal(): com.habitflowai.data.model.ActiveGoalResponse?
    
    fun getTasksForDate(userId: String, date: String): Flow<List<DailyTaskEntity>>
    suspend fun syncDailyTasks(date: String, force: Boolean = false): Result<HomeResponse>
    suspend fun updateTaskCompletion(userId: String, taskId: String, isCompleted: Boolean): Boolean
    fun getDatesWithCompletions(userId: String): Flow<List<String>>
}
