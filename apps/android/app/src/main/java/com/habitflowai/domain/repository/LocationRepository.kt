package com.habitflowai.domain.repository

import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.model.LocationResponse

interface LocationRepository {
    suspend fun captureAndSaveLocation(habitId: String?)
    fun getLastLocation(): LocationEntity?
    suspend fun getLocationsForHabit(habitId: String): List<LocationEntity>
    suspend fun getLocations(): List<LocationEntity>
    suspend fun getMyLocations(): List<LocationResponse>
}
