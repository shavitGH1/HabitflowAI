package com.habitflowai.domain.repository

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.util.Resource

interface PersonaRepository {
    suspend fun classifyPersona(request: ClassifyPersonaRequest): Resource<ClassifyPersonaResponse>
}
