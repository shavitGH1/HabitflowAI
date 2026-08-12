package com.habitflowai.domain.repository

import com.habitflowai.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun captureAndSaveLocation(habitId: String?, taskTitle: String? = null)
    fun getLastLocation(): LocationEntity?
    suspend fun getLocationsForHabit(habitId: String): List<LocationEntity>
    suspend fun getLocations(): List<LocationEntity>
    fun getLocationsFlow(): Flow<List<LocationEntity>>
    suspend fun refreshFromServer()
}
