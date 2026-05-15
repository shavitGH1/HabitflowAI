package com.example.habitflowai.domain.repository

import com.example.habitflowai.data.model.ClassifyPersonaRequest
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import com.example.habitflowai.util.Resource

interface PersonaRepository {
    suspend fun classifyPersona(request: ClassifyPersonaRequest): Resource<ClassifyPersonaResponse>
}

