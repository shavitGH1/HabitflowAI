package com.habitflowai.data.model

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
    val openAnswers: List<String>,
    val fcmToken: String? = null
)

data class CheckEmailRequest(val email: String)
data class CheckEmailResponse(val available: Boolean)

data class RegisterResponse(
    val message: String,
    val userId: String,
    val success: Boolean,
    val personaType: String? = null,
    val motivationalMessage: String? = null
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
    @SerializedName("personaType", alternate = ["persona", "persona_type"]) val personaType: String? = null,
    @SerializedName("portfolioSummary") val portfolioSummary: String? = null,
    @SerializedName("tips") val tips: List<String>? = null,
    @SerializedName("failurePatterns") val failurePatterns: List<String>? = null,
    @SerializedName("confidenceScore") val confidenceScore: Double? = null,
    @SerializedName("driftDetected") val driftDetected: Boolean = false,
    @SerializedName("driftRationale") val driftRationale: String? = null
)
