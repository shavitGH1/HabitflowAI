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
import com.habitflowai.domain.repository.HabitsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HabitsRepositoryImpl @Inject constructor(
    private val api: HabitFlowApi,
    private val habitDao: HabitDao,
    private val workManager: WorkManager
) : HabitsRepository {

    override fun getHabits(userId: String): Flow<List<HabitEntity>> {
        return habitDao.getHabitsByUserId(userId)
    }

    override suspend fun refreshHabits() {
        try {
            val response = api.getHabits()
            val entities = response.map { res ->
                HabitEntity(
                    id = res.id,
                    title = res.title,
                    description = res.description,
                    frequency = res.frequency,
                    userId = "me", // Should ideally be current user id
                    completed = res.completed,
                    syncStatus = SyncStatus.SYNCED,
                    completionHistory = res.completionHistory ?: emptyList()
                )
            }
            habitDao.upsertAll(entities)
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun createHabit(habit: HabitEntity) {
        try {
            val request = HabitRequest(habit.title, habit.description, habit.frequency, habit.completed)
            api.createHabit(request)
            refreshHabits()
        } catch (e: Exception) {
            habitDao.insert(habit.copy(syncStatus = SyncStatus.PENDING_CREATE))
            enqueueSync()
        }
    }

    override suspend fun updateHabit(habit: HabitEntity) {
        try {
            val request = HabitRequest(habit.title, habit.description, habit.frequency, habit.completed)
            api.updateHabit(habit.id, request)
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
        return try {
            api.completeHabit(habit.id)
            refreshHabits()
            true
        } catch (e: Exception) {
            val entity = habit.copy(
                completed = true,
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
