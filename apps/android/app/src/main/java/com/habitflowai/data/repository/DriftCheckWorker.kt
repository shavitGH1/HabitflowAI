package com.habitflowai.data.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habitflowai.data.local.dao.DriftCheckDao
import com.habitflowai.data.local.dao.HabitDao
import com.habitflowai.data.local.entity.DriftCheckEntity
import com.habitflowai.data.model.DriftCheckRequest
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class DriftCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: HabitFlowApi,
    private val driftCheckDao: DriftCheckDao,
    private val habitDao: HabitDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = api.driftCheck(buildRequest())
            val entity = DriftCheckEntity(
                id = UUID.randomUUID().toString(),
                driftDetected = result.driftDetected,
                driftScore = result.driftScore,
                newSuggestedPersona = result.newSuggestedPersona,
                rationale = result.rationale,
                checkedAt = System.currentTimeMillis()
            )
            driftCheckDao.insert(entity)

            if (result.driftDetected) {
                notificationHelper.showDriftDetectedNotification(
                    driftScore = result.driftScore,
                    suggestedPersona = result.newSuggestedPersona,
                    rationale = result.rationale
                )
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun buildRequest(): DriftCheckRequest {
        val habits = habitDao.getAll()
        val completed = habits.filter { it.completed }.map { it.title }
        val skipped = habits.filter { !it.completed }.map { it.title }
        val completionRate = if (habits.isNotEmpty()) {
            completed.size.toDouble() / habits.size
        } else null

        return DriftCheckRequest(
            recentCompletionRate = completionRate,
            activeStreak = null,
            completedHabits = completed.ifEmpty { null },
            skippedHabits = skipped.ifEmpty { null }
        )
    }
}
