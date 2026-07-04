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
    @SerializedName("timestamp") val timestamp: Long
)
