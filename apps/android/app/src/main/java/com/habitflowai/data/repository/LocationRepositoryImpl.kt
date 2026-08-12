package com.habitflowai.data.repository

import android.content.Context
import android.location.Location
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.model.LocationResponse
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationDao: LocationDao,
    private val api: HabitFlowApi,
    private val workManager: WorkManager
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun captureAndSaveLocation(habitId: String?, taskTitle: String?) {
        val location = getCurrentLocation() ?: return
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            taskTitle = taskTitle,
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

    override fun getLocationsFlow(): Flow<List<LocationEntity>> {
        return locationDao.getAllLocationsFlow()
    }

    override suspend fun refreshFromServer() {
        val serverLocations = try {
            api.getMyLocations()
        } catch (_: Exception) {
            return
        }
        if (serverLocations.isEmpty()) return

        val local = locationDao.getAllLocations()
        val localByKey = local.associateBy { it.toMatchKey() }
        val merged = serverLocations.map { server ->
            localByKey[server.toMatchKey()]?.let { existing ->
                existing.copy(
                    taskTitle = server.taskTitle ?: existing.taskTitle,
                    placeName = server.placeName ?: existing.placeName,
                    address = server.address ?: existing.address,
                    syncStatus = SyncStatus.SYNCED
                )
            } ?: LocationEntity(
                id = server.id,
                habitId = server.habitId,
                latitude = server.latitude,
                longitude = server.longitude,
                timestamp = server.timestamp,
                taskTitle = server.taskTitle,
                placeName = server.placeName,
                address = server.address,
                syncStatus = SyncStatus.SYNCED
            )
        }
        locationDao.upsertAll(merged)
    }

    private suspend fun getCurrentLocation(): Location? {
        return try {
            val tokenSource = CancellationTokenSource()
            fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .await()
        } catch (e: Exception) {
            try {
                fusedLocationClient.lastLocation.await()
            } catch (_: Exception) {
                null
            }
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

    private fun LocationEntity.toMatchKey(): String {
        return "${habitId}|$latitude|$longitude|$timestamp"
    }

    private fun LocationResponse.toMatchKey(): String {
        return "$habitId|$latitude|$longitude|$timestamp"
    }
}
