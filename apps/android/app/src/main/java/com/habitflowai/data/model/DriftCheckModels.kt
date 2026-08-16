package com.habitflowai.data.model

import com.google.gson.annotations.SerializedName

data class DriftCheckRequest(
    @SerializedName("recentCompletionRate") val recentCompletionRate: Double? = null,
    @SerializedName("activeStreak") val activeStreak: Int? = null,
    @SerializedName("completedHabits") val completedHabits: List<String>? = null,
    @SerializedName("skippedHabits") val skippedHabits: List<String>? = null
)

data class DriftCheckResponse(
    @SerializedName("driftDetected") val driftDetected: Boolean,
    @SerializedName("driftScore") val driftScore: Double,
    @SerializedName("newSuggestedPersona") val newSuggestedPersona: String?,
    @SerializedName("currentBreakdown") val currentBreakdown: Map<String, Double>?,
    @SerializedName("rationale") val rationale: String
)

data class FcmTokenUpdateRequest(
    @SerializedName("fcmToken") val fcmToken: String
)

data class LocationSyncRequest(
    @SerializedName("habitId") val habitId: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("isPublic") val isPublic: Boolean = true,
    @SerializedName("type") val type: String = "task"
)

data class LocationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("habitId") val habitId: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("placeName") val placeName: String? = null,
    @SerializedName("taskDescription") val taskDescription: String? = null,
    @SerializedName("timestamp") val timestamp: Long = 0,
    @SerializedName("isPublic") val isPublic: Boolean = true,
    @SerializedName("username") val username: String? = null,
    @SerializedName("relationship") val relationship: String? = null,
    @SerializedName("type") val type: String = "task"
)
