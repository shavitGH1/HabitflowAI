package com.example.habitflowai.data.repository

import com.example.habitflowai.data.model.ClassifyPersonaRequest
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import com.example.habitflowai.data.network.HabitFlowApi
import com.example.habitflowai.domain.repository.PersonaRepository
import com.example.habitflowai.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PersonaRepositoryImpl(
    private val api: HabitFlowApi
) : PersonaRepository {
    override suspend fun classifyPersona(request: ClassifyPersonaRequest): Resource<ClassifyPersonaResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(api.classifyPersona(request))
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Unknown error")
            }
        }
    }
}

