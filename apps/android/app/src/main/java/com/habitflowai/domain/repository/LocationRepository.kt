package com.habitflowai.domain.repository

import com.habitflowai.data.local.entity.LocationEntity

interface LocationRepository {
    suspend fun captureAndSaveLocation(habitId: String?)
    fun getLastLocation(): LocationEntity?
    suspend fun getLocationsForHabit(habitId: String): List<LocationEntity>
    suspend fun getLocations(): List<LocationEntity>
}
