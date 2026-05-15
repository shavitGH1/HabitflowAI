package com.habitflowai.data.repository

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.HabitFlowRepository
import com.habitflowai.presentation.util.Resource

class HabitFlowRepositoryImpl(
    private val api: HabitFlowApi
) : HabitFlowRepository {
    override suspend fun classifyPersona(request: ClassifyPersonaRequest): Resource<ClassifyPersonaResponse> {
        return try {
            val response = api.classifyPersona(request)
            Resource.Success(response)
        } catch (exception: Exception) {
            Resource.Error(exception.message ?: "Unknown error")
        }
    }
}

