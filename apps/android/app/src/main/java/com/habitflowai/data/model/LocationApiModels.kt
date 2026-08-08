package com.habitflowai.data.model

import com.google.gson.annotations.SerializedName

data class LocationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("habitId") val habitId: String?,
    @SerializedName("taskTitle") val taskTitle: String?,
    @SerializedName("placeName") val placeName: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("personaType") val personaType: String?,
    @SerializedName("isPublic") val isPublic: Boolean,
    @SerializedName("createdAt") val createdAt: String?
)
