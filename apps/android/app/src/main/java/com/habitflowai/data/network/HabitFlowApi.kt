package com.habitflowai.data.network

import com.habitflowai.data.model.ClassifyPersonaRequest
import com.habitflowai.data.model.ClassifyPersonaResponse
import com.habitflowai.data.model.GenerateGoalsRequest
import com.habitflowai.data.model.GenerateGoalsResponse
import com.habitflowai.data.model.HabitRequest
import com.habitflowai.data.model.HabitResponse
import com.habitflowai.data.model.CheckEmailRequest
import com.habitflowai.data.model.CheckEmailResponse
import com.habitflowai.data.model.LoginRequest
import com.habitflowai.data.model.LoginResponse
import com.habitflowai.data.model.RegisterRequest
import com.habitflowai.data.model.RegisterResponse
import com.habitflowai.data.model.TokenRefreshRequest
import com.habitflowai.data.model.TokenRefreshResponse
import com.habitflowai.data.model.HomeResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.Response

interface HabitFlowApi {
    @POST("api/v1/auth/check-email")
    suspend fun checkEmail(@Body request: CheckEmailRequest): CheckEmailResponse

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

    @PATCH("api/v1/tasks/{taskId}/complete")
    suspend fun completeTask(@Path("taskId") taskId: String): Response<Unit>

    @POST("api/v1/habits")
    suspend fun createHabit(@Body habit: HabitRequest): HabitResponse

    @PUT("api/v1/habits/{id}")
    suspend fun updateHabit(@Path("id") id: String, @Body habit: HabitRequest): HabitResponse

    @DELETE("api/v1/habits/{id}")
    suspend fun deleteHabit(@Path("id") id: String): Response<Unit>

    @GET("api/v1/habits")
    suspend fun getHabits(): List<HabitResponse>
}
