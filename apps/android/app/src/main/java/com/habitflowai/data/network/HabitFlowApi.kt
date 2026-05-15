package com.habitflowai.data.network

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface HabitFlowApi {
    @POST("api/v1/personas/classify")
    suspend fun classifyPersona(@Body request: ClassifyPersonaRequest): ClassifyPersonaResponse
}

