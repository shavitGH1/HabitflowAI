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
                    verificationWarning = res.verificationWarning,
                    implementedAt = res.implementedAt,
                    streak = res.streak
                )
                habitDao.insert(entity)
            }

            // GET /habits only returns non-archived habits - a synced local habit whose
            // serverId is missing from this response was archived/deleted server-side.
            // Prune it, or it lingers locally forever and inflates cap counts.
            val serverIds = response.map { it.id }.toSet()
            habitDao.getAllForUser(currentUserId)
                .filter { it.syncStatus == SyncStatus.SYNCED && it.serverId != null && it.serverId !in serverIds }
                .forEach { habitDao.delete(it) }
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
                implementedAt = response.implementedAt,
                syncStatus = SyncStatus.SYNCED
            )

            // Use transaction to atomically remove temporary UUID and insert server record
            habitDao.replaceHabit(habit, entity)
            
            try { refreshHabits() } catch (_: Exception) {}
            Result.success(entity)
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() in 400..499) {
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
            // A 4xx means the server actually rejected this (e.g. already achieved) -
            // retrying it later would just fail again, so leave the habit as-is rather
            // than hiding it locally under a PENDING_DELETE that can never resolve.
            if (e is retrofit2.HttpException && e.code() in 400..499) return
            habitDao.updateSyncStatus(habit.id, SyncStatus.PENDING_DELETE)
            enqueueSync()
        }
    }

    override suspend fun markHabitAchieved(habit: HabitEntity): Boolean {
        return try {
            val idToMark = habit.serverId ?: habit.id
            val response = api.markHabitAchieved(idToMark)
            if (response.isSuccessful) {
                val body = response.body()
                habitDao.update(habit.copy(
                    implementedAt = body?.implementedAt ?: habit.implementedAt,
                    syncStatus = SyncStatus.SYNCED
                ))
                true
            } else {
                // Server re-validates streak/already-achieved - don't set implementedAt locally
                // on rejection, unlike completeHabit()'s optimistic fallback.
                false
            }
        } catch (e: Exception) {
            false
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
