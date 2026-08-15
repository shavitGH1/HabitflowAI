package com.habitflowai.domain.repository

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.ReclassifyRequest
import com.habitflowai.data.model.ReclassifyResponse
import com.habitflowai.util.Resource

interface PersonaRepository {
    suspend fun classifyPersona(request: ClassifyPersonaRequest): Resource<ClassifyPersonaResponse>
    suspend fun reclassifyPersona(request: ReclassifyRequest): Resource<ReclassifyResponse>
}
