package com.example.habitflowai.data.model

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
