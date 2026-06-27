package com.habitflowai.data.repository

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.entity.HabitEntity
import com.habitflowai.data.local.entity.SyncStatus
import com.habitflowai.domain.repository.HabitsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HabitsRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val workManager: WorkManager
) : HabitsRepository {

    override fun getHabits(userId: String): Flow<List<HabitEntity>> {
        return habitDao.getHabitsByUserId(userId)
    }

    override suspend fun createHabit(habit: HabitEntity) {
        val entity = habit.copy(syncStatus = if (hasNetwork()) SyncStatus.PENDING_CREATE else SyncStatus.PENDING_CREATE)
        habitDao.insert(entity)
        enqueueSync()
    }

    override suspend fun updateHabit(habit: HabitEntity) {
        val entity = habit.copy(
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = System.currentTimeMillis()
        )
        habitDao.update(entity)
        enqueueSync()
    }

    override suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.updateSyncStatus(habit.id, SyncStatus.PENDING_DELETE)
        enqueueSync()
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
