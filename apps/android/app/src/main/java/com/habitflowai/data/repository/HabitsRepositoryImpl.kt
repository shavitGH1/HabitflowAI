package com.habitflowai.data.repository

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.model.HabitRequest
import com.habitflowai.di.AuthManager
import com.habitflowai.domain.repository.HabitsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HabitsRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi,
    private val habitDao: HabitDao,
    private val workManager: WorkManager,
    private val authManager: AuthManager
) : HabitsRepository {

    override fun getHabits(userId: String): Flow<List<HabitEntity>> {
        return habitDao.getHabitsByUserId(userId)
    }

    override suspend fun refreshHabits() {
        try {
            val userIdAtRequestTime = authManager.currentUserId.value ?: return
            val response = api.getHabits()
            val currentUserId = authManager.currentUserId.value ?: return
            if (currentUserId != userIdAtRequestTime) return

            for (res in response) {
                val localByServerId = habitDao.getHabitByServerId(res.id)
                val localById = habitDao.getHabitById(res.id)
                
                val entity = HabitEntity(
                    id = localByServerId?.id ?: localById?.id ?: res.id,
                    title = res.title,
                    description = res.description,
                    frequency = res.frequency,
                    userId = currentUserId,
                    completed = res.completed,
                    syncStatus = SyncStatus.SYNCED,
                    serverId = res.id,
                    goalId = res.goalId,
                    completionHistory = res.completionHistory ?: emptyList(),
                    relevanceWarning = res.relevanceWarning,
                    verificationWarning = res.verificationWarning
                )
                habitDao.insert(entity)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun createHabit(habit: HabitEntity, goalId: String?): Result<HabitEntity> {
        return try {
            val request = HabitRequest(habit.title, habit.description, habit.frequency, goalId = goalId)
            val response = api.createHabit(request)
            val entity = habit.copy(
                id = response.id, // Use server ID as local primary key immediately
                serverId = response.id,
                goalId = response.goalId, // Trust the server's decision (might be null if unrelated)
                completed = response.completed,
                completionHistory = response.completionHistory ?: emptyList(),
                relevanceWarning = response.relevanceWarning,
                verificationWarning = response.verificationWarning,
                syncStatus = SyncStatus.SYNCED
            )
            
            // Use transaction to atomically remove temporary UUID and insert server record
            habitDao.replaceHabit(habit, entity)
            
            try { refreshHabits() } catch (_: Exception) {}
            Result.success(entity)
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() in 400..499) {
                // The server definitively rejected this (e.g. the 3-habits-per-goal
                // cap) — retrying later can't fix it, so don't queue a doomed
                // PENDING_CREATE row; just surface the real error.
                Result.failure(e)
            } else {
                habitDao.insert(habit.copy(syncStatus = SyncStatus.PENDING_CREATE))
                enqueueSync()
                Result.failure(e)
            }
        }
    }

    override suspend fun updateHabit(habit: HabitEntity) {
        try {
            val request = HabitRequest(habit.title, habit.description, habit.frequency)
            val serverId = habit.serverId ?: habit.id
            api.updateHabit(serverId, request)
            refreshHabits()
        } catch (e: Exception) {
            val entity = habit.copy(
                syncStatus = SyncStatus.PENDING_UPDATE,
                updatedAt = System.currentTimeMillis()
            )
            habitDao.update(entity)
            enqueueSync()
        }
    }

    override suspend fun deleteHabit(habit: HabitEntity) {
        try {
            api.deleteHabit(habit.id)
            habitDao.delete(habit)
        } catch (e: Exception) {
            habitDao.updateSyncStatus(habit.id, SyncStatus.PENDING_DELETE)
            enqueueSync()
        }
    }

    override suspend fun completeHabit(habit: HabitEntity, note: String?): Boolean {
        val today = java.time.LocalDate.now().toString()
        val updatedHistory = (habit.completionHistory + today).distinct()
        
        return try {
            val idToComplete = habit.serverId ?: habit.id
            val params = mutableMapOf("date" to today)
            note?.let { params["note"] = it }
            val response = api.completeHabit(idToComplete, params)
            
            if (response.isSuccessful) {
                val body = response.body()
                // Update locally immediately with the server-returned data (via refresh)
                // but also ensure our local state is updated right now.
                habitDao.update(habit.copy(
                    completed = true, 
                    completionHistory = body?.completionHistory ?: updatedHistory,
                    relevanceWarning = body?.relevanceWarning,
                    verificationWarning = body?.verificationWarning,
                    syncStatus = SyncStatus.SYNCED
                ))
                try { refreshHabits() } catch (_: Exception) {}
                true
            } else {
                // Fallback for ANY server error (404, 500, etc.)
                // This ensures the user isn't blocked by server/sync issues.
                val entity = habit.copy(
                    completed = true,
                    completionHistory = updatedHistory,
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    updatedAt = System.currentTimeMillis()
                )
                habitDao.update(entity)
                enqueueSync()
                true
            }
        } catch (e: Exception) {
            // Network error - fallback to local update + background sync
            val entity = habit.copy(
                completed = true,
                completionHistory = updatedHistory,
                syncStatus = SyncStatus.PENDING_UPDATE,
                updatedAt = System.currentTimeMillis()
            )
            habitDao.update(entity)
            enqueueSync()
            true
        }
    }

    override suspend fun getHabitStats(habitId: String): Map<String, Any> {
        return try {
            api.getHabitStats(habitId)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<HabitSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }

    private fun hasNetwork(): Boolean {
        return try {
            val cm = WorkManager::class.java.classLoader
            true
        } catch (_: Exception) {
            false
        }
    }
}
