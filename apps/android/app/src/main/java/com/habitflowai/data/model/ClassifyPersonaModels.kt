package com.habitflowai.data.model

import com.google.gson.annotations.SerializedName

data class ClassifyPersonaRequest(
    @SerializedName("goal") val goal: String,
    @SerializedName("quizAnswers") val quizAnswers: List<String>
)

data class ClassifyPersonaResponse(
    @SerializedName("id") val id: String,
    @SerializedName("personaType") val personaType: String,
    @SerializedName("motivationalMessage") val motivationalMessage: String,
    @SerializedName("success") val success: Boolean,
    @SerializedName("userId") val userId: String? = null
)

data class ReclassifyRequest(
    @SerializedName("openAnswers") val openAnswers: List<String>
)

data class ReclassifyResponse(
    @SerializedName("personaType") val personaType: String,
    @SerializedName("weightedBreakdown") val weightedBreakdown: Map<String, Double>? = null,
    @SerializedName("success") val success: Boolean
)
