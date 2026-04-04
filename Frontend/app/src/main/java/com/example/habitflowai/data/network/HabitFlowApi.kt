package com.example.habitflowai.data.network

import com.example.habitflowai.data.model.ClassifyPersonaRequest
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface HabitFlowApi {
    @POST("api/v1/personas/classify")
    suspend fun classifyPersona(@Body request: ClassifyPersonaRequest): ClassifyPersonaResponse
}

