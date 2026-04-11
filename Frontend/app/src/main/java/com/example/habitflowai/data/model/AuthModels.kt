package com.example.habitflowai.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val success: Boolean
)

data class TokenRefreshRequest(
    val refreshToken: String
)

data class TokenRefreshResponse(
    val accessToken: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val goal: String,
    val quizAnswers: List<String>
)

data class RegisterResponse(
    val message: String,
    val userId: String,
    val success: Boolean
)

data class HomeGoalTask(
    val description: String,
    val points: Int,
    val id: String,
    val completed: Boolean
)

data class HomeResponse(
    @SerializedName("goal") val goal: String,
    @SerializedName("motivationalMessage") val motivationalMessage: String,
    @SerializedName("coreGoals") val coreGoals: List<HomeGoalTask>,
    @SerializedName("dailyVariations") val dailyVariations: List<HomeGoalTask>,
    @SerializedName("success") val success: Boolean,
    @SerializedName("personaType", alternate = ["persona", "persona_type"]) val personaType: String? = null
)
