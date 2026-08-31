package com.habitflowai.data.repository

import com.habitflowai.data.local.dao.DailyTaskDao
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.dao.UserDao
import com.habitflowai.data.local.entity.DailyTaskEntity
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.model.GenerateGoalsRequest
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.data.model.toGoalPairList
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.ResolveHabitsOutcome
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GoalsRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi,
    private val userDao: UserDao,
    private val habitDao: HabitDao,
    private val dailyTaskDao: DailyTaskDao,
    private val authManager: AuthManager
) : GoalsRepository {
    override suspend fun fetchGoals(userId: String, dayOfWeek: Int): List<Pair<String, Int>> {
        val request = GenerateGoalsRequest(userId, dayOfWeek)
        val response = api.generateGoals(request)

        if (!response.isValid) {
            throw Exception(response.errorReason ?: "Validation failed without a known reason")
        }

        return response.toGoalPairList()
    }

    override suspend fun getHomeData(force: Boolean): HomeResponse {
        val response = api.getHome(if (force) true else null, LocalDate.now().toString())
        savePortfolioToRoom(response)
        return response
    }

    private suspend fun savePortfolioToRoom(homeData: HomeResponse) {
        val userId = authManager.currentUserId.value ?: return
        val existingUser = userDao.getUserById(userId) ?: return
        val updatedUser = existingUser.copy(
            portfolioSummary = homeData.portfolioSummary,
            tips = homeData.tips?.joinToString(","),
            failurePatterns = homeData.failurePatterns?.joinToString(","),
            confidenceScore = homeData.confidenceScore
        )
        userDao.insert(updatedUser)
    }

    override suspend fun completeTask(taskId: String): Boolean {
        val userId = authManager.currentUserId.value ?: return false
        return updateTaskCompletion(userId, taskId, true)
    }

    override suspend fun getActiveGoal(): com.habitflowai.data.model.ActiveGoalResponse? {
        return api.getActiveGoal()
    }

    override suspend fun achieveGoal(goalId: String): Boolean {
        return try {
            api.achieveGoal(goalId).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun forfeitGoal(goalId: String): Boolean {
        return try {
            api.forfeitGoal(goalId).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun transitionGoal(
        goalId: String,
        resolution: String,
        newGoalTitle: String,
        newGoalTargetDate: String
    ): String? {
        return try {
            val request = com.habitflowai.data.model.TransitionGoalRequest(resolution, newGoalTitle, newGoalTargetDate)
            api.transitionGoal(goalId, request).newGoal.id
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun resolveHabits(oldGoalId: String, newGoalId: String, decision: String?): ResolveHabitsOutcome {
        return try {
            val request = com.habitflowai.data.model.ResolveHabitsRequest(newGoalId, decision)
            val response = api.resolveHabits(oldGoalId, request)
            when (response.outcome) {
                "resolved" -> ResolveHabitsOutcome.Resolved
                "needs_decision" -> ResolveHabitsOutcome.NeedsDecision(response.pendingHabitIds?.size ?: 0)
                else -> ResolveHabitsOutcome.Failed
            }
        } catch (e: Exception) {
            ResolveHabitsOutcome.Failed
        }
    }

    override fun getTasksForDate(userId: String, date: String): Flow<List<DailyTaskEntity>> {
        return dailyTaskDao.getTasksForDate(userId, date)
    }

    override suspend fun syncDailyTasks(date: String, force: Boolean): Result<HomeResponse> {
        return try {
            val userId = authManager.currentUserId.value ?: throw Exception("Not logged in")
            
            // CRITICAL: Refresh local habits first to ensure we can map habitIds to titles
            try {
                val habitsResponse = api.getHabits()
                val habits = habitsResponse.map { res ->
                    com.habitflowai.data.local.entity.HabitEntity(
                        id = res.id,
                        title = res.title,
                        description = res.description,
                        frequency = res.frequency,
                        userId = userId,
                        completed = res.completed,
                        serverId = res.id,
                        goalId = res.goalId,
                        completionHistory = res.completionHistory ?: emptyList()
                    )
                }
                habitDao.upsertAll(habits)
            } catch (e: Exception) {
                // If habit refresh fails, continue with local habits
            }

            val homeData = getHomeData(force)

            // Clear old tasks for this date before inserting new ones to avoid stale data/hallucinations
            dailyTaskDao.deleteTasksForDate(userId, date)

            val dailyTasks = mapToDailyTaskEntities(homeData.coreGoals + homeData.dailyVariations, userId, date)
            dailyTaskDao.insertTasks(dailyTasks)
            
            // Prune older than 28 days for this user
            val threshold = LocalDate.now().minusDays(28).toString()
            dailyTaskDao.deleteOldTasks(userId, threshold)
            
            Result.success(homeData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaskCompletion(userId: String, taskId: String, isCompleted: Boolean, note: String?): Boolean {
        val today = LocalDate.now().toString()
        val body = mutableMapOf("date" to today)
        note?.let { body["note"] = it }
        val response = api.completeTask(taskId, body)
        if (response.isSuccessful) {
            dailyTaskDao.updateTaskCompletion(userId, taskId, isCompleted)
        }
        return response.isSuccessful
    }

    override fun getDatesWithCompletions(userId: String): Flow<List<String>> {
        return dailyTaskDao.getDatesWithCompletions(userId)
    }

    override suspend fun ensureHistoryLoaded(userId: String, date: String) {
        val existing = dailyTaskDao.getTasksForDateSync(userId, date)
        if (existing.isNotEmpty()) return

        try {
            val response = api.getTaskHistory(date)
            if (response.tasks.isEmpty()) return
            val entities = mapToDailyTaskEntities(response.tasks, userId, date)
            dailyTaskDao.insertTasks(entities)
        } catch (e: Exception) {
            // Nothing recorded for this date, or the request failed - leave it empty either way.
        }
    }

    private suspend fun mapToDailyTaskEntities(
        tasks: List<com.habitflowai.data.model.HomeGoalTask>,
        userId: String,
        date: String
    ): List<DailyTaskEntity> {
        val userHabits = habitDao.getAllForUser(userId)
        val habitMap = userHabits.associateBy { it.id }
        val habitMapByTitle = userHabits.associateBy { it.title.lowercase().trim() }

        return tasks.map { task ->
            val habitById = if (!task.habitId.isNullOrBlank()) habitMap[task.habitId] else null

            // Flexible matching: Try ID first, then try matching description to a habit title
            val habit = habitById ?: habitMapByTitle[task.description.lowercase().trim()]

            val resolvedHabitTitle = when {
                habit != null -> habit.title
                task.genre == "goal" -> "Main Goal"
                else -> "Goal Task" // Final fallback
            }

            DailyTaskEntity(
                id = task.id,
                userId = userId,
                habitId = habit?.id ?: task.habitId ?: "",
                habitTitle = resolvedHabitTitle,
                date = date,
                description = task.description,
                isCompleted = task.completed,
                genre = task.genre
            )
        }
    }
}
