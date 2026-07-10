package com.habitflowai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.habitflowai.data.repository.DriftCheckWorker
import com.habitflowai.domain.repository.AuthRepository
import com.habitflowai.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HabitFlowApplication : Application(), Configuration.Provider {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory
    @Inject lateinit var authRepository: AuthRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannels()
        scheduleWeeklyDriftCheck()
        sendFcmTokenOnLaunch()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    private fun sendFcmTokenOnLaunch() {
        applicationScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                authRepository.updateFcmToken(token)
            } catch (_: Exception) {
                // Will retry on next launch or token refresh
            }
        }
    }

    private fun scheduleWeeklyDriftCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val driftRequest = PeriodicWorkRequestBuilder<DriftCheckWorker>(
            7, TimeUnit.DAYS
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weekly_drift_check",
            ExistingPeriodicWorkPolicy.KEEP,
            driftRequest
        )
    }
}
