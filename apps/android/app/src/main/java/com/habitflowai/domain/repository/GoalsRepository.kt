package com.habitflowai.domain.repository

import com.habitflowai.data.model.HomeResponse

interface GoalsRepository {
    suspend fun fetchGoals(userId: String, dayOfWeek: Int): List<Pair<String, Int>>
    suspend fun getHomeData(): HomeResponse
    suspend fun completeTask(taskId: String): Boolean
}
