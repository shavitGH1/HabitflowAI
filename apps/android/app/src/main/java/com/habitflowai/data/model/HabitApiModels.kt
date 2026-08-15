package com.habitflowai.data.model

import com.google.gson.annotations.SerializedName

data class HabitRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("completed") val completed: Boolean
)

data class HabitResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("completionHistory") val completionHistory: List<String>? = emptyList()
)

data class ActiveGoalResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("targetDate") val targetDate: String?,
    @SerializedName("progress") val progress: Double?
)
