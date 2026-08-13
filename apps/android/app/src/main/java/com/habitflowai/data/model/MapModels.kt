package com.habitflowai.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Model for habit completion locations on the map.
 */
data class HabitMarker(
    val id: String,
    val habitName: String,
    val personaEmoji: String,
    val habitType: String,
    val latLng: LatLng,
    val isPublic: Boolean = false,
    val username: String? = null
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = habitName
    override fun getSnippet(): String = username?.takeIf { it.isNotBlank() } ?: "$personaEmoji $habitType"
    override fun getZIndex(): Float? = null
}
