package com.habitflowai.data.repository

import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.model.GenerateGoalsRequest
import com.habitflowai.data.model.HomeResponse
import com.habitflowai.data.model.toGoalPairList

class GoalsRepositoryImpl(
    private val api: HabitFlowApi
) {
    suspend fun fetchGoals(userId: String, dayOfWeek: Int): List<Pair<String, Int>> {
        val request = GenerateGoalsRequest(userId, dayOfWeek)
        val response = api.generateGoals(request)

        if (!response.isValid) {
            throw Exception(response.errorReason ?: "Validation failed without a known reason")
        }

        return response.toGoalPairList()
    }

    suspend fun getHomeData(): HomeResponse {
        return api.getHome()
    }

    suspend fun completeTask(taskId: String): Boolean {
        val response = api.completeTask(taskId)
        return response.isSuccessful
    }
}
