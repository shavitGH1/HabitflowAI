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
            val response = api.getHabits()
            val currentUserId = authManager.currentUserId.value ?: return
            
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
                    completionHistory = res.completionHistory ?: emptyList()
                )
                habitDao.insert(entity)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun createHabit(habit: HabitEntity) {
        try {
            val request = HabitRequest(habit.title, habit.description, habit.frequency)
            val response = api.createHabit(request)
            // Update local habit with serverId and mark as synced
            habitDao.update(habit.copy(serverId = response.id, syncStatus = SyncStatus.SYNCED))
            refreshHabits()
        } catch (e: Exception) {
            habitDao.insert(habit.copy(syncStatus = SyncStatus.PENDING_CREATE))
            enqueueSync()
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

    override suspend fun completeHabit(habit: HabitEntity): Boolean {
        val today = java.time.LocalDate.now().toString()
        val updatedHistory = (habit.completionHistory + today).distinct()
        
        return try {
            val idToComplete = habit.serverId ?: habit.id
            // Pass an empty body because the backend expects CompleteHabitDto
            val response = api.completeHabit(idToComplete, emptyMap())
            
            if (response.isSuccessful) {
                // Update locally immediately with the server-returned data (via refresh)
                // but also ensure our local state is updated right now.
                habitDao.update(habit.copy(completed = true, completionHistory = updatedHistory, syncStatus = SyncStatus.SYNCED))
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
