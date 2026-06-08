package com.habitflowai.data.repository

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.util.Resource
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
