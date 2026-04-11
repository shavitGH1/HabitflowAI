package com.example.habitflowai.data.network

import com.example.habitflowai.data.model.ClassifyPersonaRequest
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import com.example.habitflowai.data.model.GenerateGoalsRequest
import com.example.habitflowai.data.model.GenerateGoalsResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface HabitFlowApi {
    @POST("api/v1/personas/classify")
    suspend fun classifyPersona(@Body request: ClassifyPersonaRequest): ClassifyPersonaResponse

    @POST("api/v1/goals/generate")
    suspend fun generateGoals(@Body request: GenerateGoalsRequest): GenerateGoalsResponse
}
