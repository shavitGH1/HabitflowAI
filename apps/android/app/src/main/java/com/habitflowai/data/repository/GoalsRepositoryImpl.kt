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

    override suspend fun getHomeData(): HomeResponse {
        val response = api.getHome()
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

    override fun getTasksForDate(userId: String, date: String): Flow<List<DailyTaskEntity>> {
        return dailyTaskDao.getTasksForDate(userId, date)
    }

    override suspend fun syncDailyTasks(date: String): Result<Unit> {
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

            val homeData = getHomeData()
            val userHabits = habitDao.getAllForUser(userId)
            val habitMap = userHabits.associateBy { it.id }

            val dailyTasks = (homeData.coreGoals + homeData.dailyVariations).map { task ->
                DailyTaskEntity(
                    id = task.id,
                    userId = userId,
                    habitId = task.habitId ?: "",
                    habitTitle = task.habitId?.let { habitMap[it]?.title } ?: "General",
                    date = date,
                    description = task.description,
                    isCompleted = task.completed
                )
            }
            
            dailyTaskDao.insertTasks(dailyTasks)
            
            // Prune older than 28 days for this user
            val threshold = LocalDate.now().minusDays(28).toString()
            dailyTaskDao.deleteOldTasks(userId, threshold)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaskCompletion(userId: String, taskId: String, isCompleted: Boolean): Boolean {
        val today = LocalDate.now().toString()
        val response = api.completeTask(taskId, mapOf("date" to today))
        if (response.isSuccessful) {
            dailyTaskDao.updateTaskCompletion(userId, taskId, isCompleted)
        }
        return response.isSuccessful
    }

    override fun getDatesWithCompletions(userId: String): Flow<List<String>> {
        return dailyTaskDao.getDatesWithCompletions(userId)
    }
}
