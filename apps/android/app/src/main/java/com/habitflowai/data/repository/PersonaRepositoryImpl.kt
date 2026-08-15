package com.habitflowai.data.repository

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.ReclassifyRequest
import com.habitflowai.data.model.ReclassifyResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.PersonaRepository
import com.habitflowai.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PersonaRepositoryImpl @Inject constructor(
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

    override suspend fun reclassifyPersona(request: ReclassifyRequest): Resource<ReclassifyResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(api.reclassifyPersona(request))
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Unknown error")
            }
        }
    }
}
