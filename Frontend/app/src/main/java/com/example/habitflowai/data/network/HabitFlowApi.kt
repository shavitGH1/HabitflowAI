package com.example.habitflowai.data.network

import com.example.habitflowai.data.model.ClassifyPersonaRequest
import com.example.habitflowai.data.model.ClassifyPersonaResponse
import com.example.habitflowai.data.model.GenerateGoalsRequest
import com.example.habitflowai.data.model.GenerateGoalsResponse
import com.example.habitflowai.data.model.LoginRequest
import com.example.habitflowai.data.model.LoginResponse
import com.example.habitflowai.data.model.RegisterRequest
import com.example.habitflowai.data.model.RegisterResponse
import com.example.habitflowai.data.model.TokenRefreshRequest
import com.example.habitflowai.data.model.TokenRefreshResponse
import com.example.habitflowai.data.model.HomeResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HabitFlowApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/refresh")
    fun refresh(@Body request: TokenRefreshRequest): Call<TokenRefreshResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("api/v1/users/me/home")
    suspend fun getHome(): HomeResponse

    @POST("api/v1/personas/classify")
    suspend fun classifyPersona(@Body request: ClassifyPersonaRequest): ClassifyPersonaResponse

    @POST("api/v1/goals/generate")
    suspend fun generateGoals(@Body request: GenerateGoalsRequest): GenerateGoalsResponse
}
