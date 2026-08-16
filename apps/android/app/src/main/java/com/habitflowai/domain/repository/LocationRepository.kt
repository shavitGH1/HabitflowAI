package com.habitflowai.domain.repository

import android.location.Location
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.model.LocationResponse

interface LocationRepository {
    suspend fun captureAndSaveLocation(
        habitId: String?,
        isPublic: Boolean = true,
        type: String = "task"
    )
    /** One-shot device location fetch (not saved/synced) — null if permission is missing or it can't be resolved. */
    suspend fun getCurrentDeviceLocation(): Location?
    fun getLastLocation(): LocationEntity?
    suspend fun getLocationsForHabit(habitId: String): List<LocationEntity>
    suspend fun getLocations(): List<LocationEntity>
    suspend fun getMyLocations(): List<LocationResponse>
    suspend fun getPublicLocations(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        since: Long? = null,
        scope: String? = null
    ): List<LocationResponse>
}
