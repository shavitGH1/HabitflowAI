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
import com.habitflowai.di.AuthManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class HabitSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitDao: HabitDao,
    private val api: HabitFlowApi,
    private val authManager: AuthManager
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
        val userId = authManager.currentUserId.value ?: return
        val unsynced = habitDao.getUnsyncedHabits(userId)
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
        val serverId = habit.serverId
        if (serverId == null) {
            // Never synced to the server (create hadn't landed yet) — nothing to delete remotely.
            habitDao.delete(habit)
            return
        }
        try {
            api.deleteHabit(serverId)
            habitDao.delete(habit)
        } catch (_: Exception) {
            // Will retry on next sync
        }
    }

    private suspend fun pullServerChanges() {
        try {
            val userIdAtRequestTime = authManager.currentUserId.value ?: return
            val remoteHabits = api.getHabits()
            val currentUserId = authManager.currentUserId.value ?: return
            if (currentUserId != userIdAtRequestTime) return

            for (remote in remoteHabits) {
                val localByServerId = habitDao.getHabitByServerId(remote.id)
                val localById = habitDao.getHabitById(remote.id)
                
                val entity = HabitEntity(
                    id = localByServerId?.id ?: localById?.id ?: remote.id,
                    title = remote.title,
                    description = remote.description,
                    frequency = remote.frequency,
                    userId = currentUserId,
                    completed = remote.completed,
                    syncStatus = SyncStatus.SYNCED,
                    serverId = remote.id,
                    goalId = remote.goalId,
                    completionHistory = remote.completionHistory ?: emptyList(),
                    relevanceWarning = remote.relevanceWarning,
                    verificationWarning = remote.verificationWarning
                )
                habitDao.insert(entity)
            }
            habitDao.deleteBySyncStatus(currentUserId, SyncStatus.PENDING_DELETE)
        } catch (_: Exception) {
            // Server pull failed, will retry
        }
    }

    private fun HabitEntity.toHabitRequest() = HabitRequest(
        title = title,
        description = description,
        frequency = frequency,
        targetCount = 1,
        goalId = goalId
    )
}
