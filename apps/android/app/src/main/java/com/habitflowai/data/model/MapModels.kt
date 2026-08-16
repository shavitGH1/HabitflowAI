package com.habitflowai.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

enum class MarkerRelationship {
    MINE, FRIEND, STRANGER;

    companion object {
        fun from(value: String?): MarkerRelationship = when (value) {
            "mine" -> MINE
            "friend" -> FRIEND
            else -> STRANGER
        }
    }
}

enum class TimeRangeFilter(val label: String, val days: Int?) {
    LAST_DAY("Last day", 1),
    LAST_3_DAYS("Last 3 days", 3),
    LAST_WEEK("Last week", 7),
    LAST_MONTH("Last month", 30),
    LAST_YEAR("Last year", 365),
    ALL_TIME("All time", null)
}

enum class AudienceFilter(val label: String, val apiValue: String) {
    EVERYONE("Everyone", "all"),
    FRIENDS("Friends", "friends"),
    MINE("Mine", "mine")
}

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
    val username: String? = null,
    val userId: String = "",
    val relationship: MarkerRelationship = MarkerRelationship.STRANGER
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = habitName
    override fun getSnippet(): String = username?.takeIf { it.isNotBlank() } ?: "$personaEmoji $habitType"
    override fun getZIndex(): Float? = null
}
