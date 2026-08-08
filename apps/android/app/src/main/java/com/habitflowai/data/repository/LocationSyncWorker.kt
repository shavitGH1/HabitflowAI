package com.habitflowai.data.repository

import android.content.Context
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
            for (location in unsynced) {
                try {
                    val request = com.habitflowai.data.model.LocationSyncRequest(
                        habitId = location.habitId,
                        taskTitle = location.taskTitle,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.timestamp
                    )
                    api.recordLocation(request)
                    locationDao.markSynced(location.id)
                } catch (_: Exception) {
                    // Will retry on next sync
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
