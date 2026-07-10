package com.habitflowai.data.repository

import android.content.Context
import android.location.Location
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationDao: LocationDao,
    private val workManager: WorkManager
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun captureAndSaveLocation(habitId: String?) {
        val location = getCurrentLocation() ?: return
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_CREATE
        )
        locationDao.insert(entity)
        enqueueSync()
    }

    override fun getLastLocation(): LocationEntity? {
        return kotlinx.coroutines.runBlocking {
            locationDao.getLastLocation()
        }
    }

    override suspend fun getLocationsForHabit(habitId: String): List<LocationEntity> {
        return locationDao.getLocationsByHabitId(habitId)
    }

    override suspend fun getLocations(): List<LocationEntity> {
        return locationDao.getAllLocations()
    }

    private suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<LocationSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }
}
