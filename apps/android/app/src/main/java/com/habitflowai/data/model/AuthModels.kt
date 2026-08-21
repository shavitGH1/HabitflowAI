package com.habitflowai.data.model

import com.google.gson.annotations.JsonAdapter
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
    val openAnswers: List<String>,
    val fcmToken: String? = null,
    val firstName: String = "",
    val lastName: String = ""
)

data class CheckEmailRequest(val email: String)
data class CheckEmailResponse(val available: Boolean)

data class OnboardingSuggestionsRequest(val goal: String)

data class OnboardingSuggestionItem(
    @SerializedName("questionId") val questionId: Int,
    @SerializedName("options") val options: List<String>
)

data class OnboardingSuggestionsResponse(
    @SerializedName("suggestions") val suggestions: List<OnboardingSuggestionItem>
)

data class RegisterResponse(
    val message: String,
    val userId: String,
    val success: Boolean,
    val personaType: String? = null,
    val motivationalMessage: String? = null
)

data class GoogleAuthRequest(val idToken: String)

// Backend response is polymorphic: an existing account returns accessToken/refreshToken
// (isNewUser: false); an unknown email returns signupToken/email/firstName/lastName
// (isNewUser: true) so the caller can route into onboarding instead of home.
data class GoogleAuthResponse(
    val success: Boolean,
    val isNewUser: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val signupToken: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class GoogleRegisterRequest(
    val signupToken: String,
    val goal: String,
    val openAnswers: List<String>,
    val fcmToken: String? = null
)

data class GoogleRegisterResponse(
    val message: String,
    val userId: String,
    val success: Boolean,
    val accessToken: String,
    val refreshToken: String,
    val personaType: String? = null,
    val motivationalMessage: String? = null
)

data class UpdateProfilePictureRequest(
    val profilePicture: String
)

data class UpdateProfileResponse(
    @JsonAdapter(ProfilePictureDeserializer::class)
    val profilePicture: String,
    val success: Boolean = true
)

data class HomeGoalTask(
    val description: String,
    val points: Int,
    val id: String,
    val completed: Boolean,
    val genre: String = "persona"
)

data class HomeResponse(
    @SerializedName("email") val email: String? = null,
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
    @SerializedName("driftRationale") val driftRationale: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    @SerializedName("profilePicture")
    @JsonAdapter(ProfilePictureDeserializer::class)
    val profilePicture: String? = null,
    val nameChangedAt: String? = null,
    val authProvider: String? = null
)

data class UpdateNameRequest(
    val firstName: String,
    val lastName: String
)

data class UpdateNameResponse(
    val firstName: String,
    val lastName: String,
    val nameChangedAt: String?,
    val success: Boolean = true
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ChangePasswordResponse(
    val success: Boolean = true
)
