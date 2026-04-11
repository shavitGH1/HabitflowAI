package com.example.habitflowai.data.repository

import com.example.habitflowai.data.network.HabitFlowApi
import com.example.habitflowai.data.model.GenerateGoalsRequest
import com.example.habitflowai.data.model.toGoalPairList

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
}
