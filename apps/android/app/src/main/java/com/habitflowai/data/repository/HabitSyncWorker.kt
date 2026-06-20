package com.habitflowai.data.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.model.HabitRequest
import com.habitflowai.data.network.HabitFlowApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class HabitSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitDao: HabitDao,
    private val api: HabitFlowApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            pushPendingChanges()
            pullServerChanges()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun pushPendingChanges() {
        val unsynced = habitDao.getUnsyncedHabits()
        for (habit in unsynced) {
            when (habit.syncStatus) {
                SyncStatus.PENDING_CREATE -> pushCreate(habit)
                SyncStatus.PENDING_UPDATE -> pushUpdate(habit)
                SyncStatus.PENDING_DELETE -> pushDelete(habit)
                SyncStatus.SYNCED -> { /* no-op */ }
            }
        }
    }

    private suspend fun pushCreate(habit: HabitEntity) {
        try {
            val request = habit.toHabitRequest()
            val response = api.createHabit(request)
            habitDao.markSynced(habit.id)
            if (habit.serverId == null) {
                habitDao.update(habit.copy(serverId = response.id))
            }
        } catch (_: Exception) {
            // Will retry on next sync
        }
    }

    private suspend fun pushUpdate(habit: HabitEntity) {
        try {
            val request = habit.toHabitRequest()
            val serverId = habit.serverId ?: habit.id
            api.updateHabit(serverId, request)
            habitDao.markSynced(habit.id)
        } catch (_: Exception) {
            // Will retry on next sync
        }
    }

    private suspend fun pushDelete(habit: HabitEntity) {
        try {
            val serverId = habit.serverId ?: habit.id
            api.deleteHabit(serverId)
            habitDao.delete(habit)
        } catch (_: Exception) {
            // Will retry on next sync
        }
    }

    private suspend fun pullServerChanges() {
        try {
            val remoteHabits = api.getHabits()
            val entities = remoteHabits.map { remote ->
                HabitEntity(
                    id = remote.id,
                    title = remote.title,
                    description = remote.description,
                    frequency = remote.frequency,
                    userId = "",
                    completed = remote.completed,
                    syncStatus = SyncStatus.SYNCED,
                    serverId = remote.id
                )
            }
            if (entities.isNotEmpty()) {
                habitDao.upsertAll(entities)
            }
            habitDao.deleteBySyncStatus(SyncStatus.PENDING_DELETE)
        } catch (_: Exception) {
            // Server pull failed, will retry
        }
    }

    private fun HabitEntity.toHabitRequest() = HabitRequest(
        title = title,
        description = description,
        frequency = frequency,
        completed = completed
    )
}
