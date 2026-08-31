package com.habitflowai.domain.repository

import com.habitflowai.data.local.entity.DailyTaskEntity
import com.habitflowai.data.model.HomeResponse
import kotlinx.coroutines.flow.Flow

sealed class ResolveHabitsOutcome {
    object Resolved : ResolveHabitsOutcome()
    data class NeedsDecision(val pendingHabitCount: Int) : ResolveHabitsOutcome()
    object Failed : ResolveHabitsOutcome()
}

interface GoalsRepository {
    suspend fun fetchGoals(userId: String, dayOfWeek: Int): List<Pair<String, Int>>
    suspend fun getHomeData(force: Boolean = false): HomeResponse
    suspend fun completeTask(taskId: String): Boolean
    suspend fun getActiveGoal(): com.habitflowai.data.model.ActiveGoalResponse?
    suspend fun achieveGoal(goalId: String): Boolean
    suspend fun forfeitGoal(goalId: String): Boolean
    suspend fun transitionGoal(goalId: String, resolution: String, newGoalTitle: String, newGoalTargetDate: String): String?
    suspend fun resolveHabits(oldGoalId: String, newGoalId: String, decision: String? = null): ResolveHabitsOutcome

    fun getTasksForDate(userId: String, date: String): Flow<List<DailyTaskEntity>>
    suspend fun syncDailyTasks(date: String, force: Boolean = false): Result<HomeResponse>
    suspend fun updateTaskCompletion(userId: String, taskId: String, isCompleted: Boolean, note: String? = null): Boolean
    fun getDatesWithCompletions(userId: String): Flow<List<String>>
}
