package com.habitflowai.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.model.LocationResponse
import com.habitflowai.data.model.LocationSyncRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Calendar
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

    override suspend fun captureAndSaveLocation(habitId: String?, isPublic: Boolean = true) {
        val location = getCurrentLocation()
        if (location == null) {
            Log.w(TAG, "captureAndSaveLocation: no location available, skipping (habitId=$habitId)")
            return
        }
        if (habitId != null) {
            val now = System.currentTimeMillis()
            val alreadyRecordedToday = locationDao.getLocationsByHabitId(habitId)
                .any { isSameLocalDay(it.timestamp, now) }
            if (alreadyRecordedToday) {
                Log.w(TAG, "captureAndSaveLocation: location already recorded for habitId=$habitId today, skipping")
                return
            }
        }
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_CREATE,
            isPublic = isPublic
        )
        locationDao.insert(entity)
        Log.d(TAG, "captureAndSaveLocation: saved ${location.latitude},${location.longitude} (habitId=$habitId, isPublic=$isPublic)")

        val request = LocationSyncRequest(
            habitId = entity.habitId,
            latitude = entity.latitude,
            longitude = entity.longitude,
            timestamp = entity.timestamp,
            isPublic = entity.isPublic
        )
        val uploaded = try {
            val response = api.recordLocation(request)
            if (response.isSuccessful) {
                locationDao.markSynced(entity.id)
                Log.d(TAG, "captureAndSaveLocation: uploaded immediately (${entity.id})")
                true
            } else {
                Log.w(TAG, "captureAndSaveLocation: server returned ${response.code()}, will retry via worker")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "captureAndSaveLocation: upload threw (${e.message}), will retry via worker")
            false
        }
        if (!uploaded) {
            enqueueSync()
        }
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

    override suspend fun getMyLocations(): List<LocationResponse> {
        return try {
            api.getMyLocations().also { Log.d(TAG, "getMyLocations: ${it.size} records") }
        } catch (e: Exception) {
            Log.w(TAG, "getMyLocations failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getCurrentLocation(): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "getCurrentLocation: ACCESS_FINE_LOCATION not granted")
            return null
        }
        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(5000)
                .build()
            fusedLocationClient.getCurrentLocation(request, CancellationTokenSource().token).await()
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLocation failed (${e.message}), falling back to lastLocation")
            try {
                fusedLocationClient.lastLocation.await()
            } catch (e2: Exception) {
                Log.w(TAG, "lastLocation also failed: ${e2.message}")
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

    private fun isSameLocalDay(firstMillis: Long, secondMillis: Long): Boolean {
        val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val TAG = "LocationRepo"
    }
}
