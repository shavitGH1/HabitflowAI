package com.habitflowai.data.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habitflowai.data.local.dao.LocationDao
import com.habitflowai.data.model.LocationSyncRequest
import com.habitflowai.data.network.HabitFlowApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationDao: LocationDao,
    private val api: HabitFlowApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsynced = locationDao.getUnsyncedLocations()
            Log.d(TAG, "doWork: ${unsynced.size} unsynced locations")
            for (location in unsynced) {
                try {
                    val request = LocationSyncRequest(
                        habitId = location.habitId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.timestamp
                    )
                    val response = api.recordLocation(request)
                    if (response.isSuccessful) {
                        locationDao.markSynced(location.id)
                        Log.d(TAG, "synced location ${location.id} (habitId=${location.habitId})")
                    } else {
                        Log.w(TAG, "recordLocation returned ${response.code()}, keeping unsynced (${location.id})")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "recordLocation threw for ${location.id}: ${e.message}")
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "LocationSyncWorker"
    }
}
