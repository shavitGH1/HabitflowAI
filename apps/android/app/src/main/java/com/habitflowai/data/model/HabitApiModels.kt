package com.habitflowai.data.model

import com.google.gson.annotations.SerializedName

data class HabitRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("targetCount") val targetCount: Int = 1,
    @SerializedName("goalId") val goalId: String? = null
)

data class HabitResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("completionHistory") val completionHistory: List<String>? = emptyList()
) {
    val completed: Boolean
        get() = completionHistory?.contains(java.time.LocalDate.now().toString()) == true
}

data class ActiveGoalResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("targetDate") val targetDate: String?,
    @SerializedName("progress") val progress: Double?
)
