package com.habitflowai.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.habitflowai.data.network.HabitFlowApi
import com.habitflowai.data.model.FcmTokenUpdateRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var api: HabitFlowApi

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.data}")

        val notificationData = message.data
        when (notificationData["type"]) {
            "drift_detected" -> {
                val driftScore = notificationData["driftScore"]?.toDoubleOrNull() ?: 0.0
                val suggestedPersona = notificationData["newSuggestedPersona"]
                val rationale = notificationData["rationale"]
                val helper = NotificationHelper(applicationContext)
                helper.showDriftDetectedNotification(driftScore, suggestedPersona, rationale)
            }
            "daily_reminder" -> {
                val personaType = notificationData["personaType"]
                val helper = NotificationHelper(applicationContext)
                helper.showDailyReminder(personaType)
            }
            else -> {
                val helper = NotificationHelper(applicationContext)
                helper.showDailyReminder(null)
            }
        }
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateFcmToken(FcmTokenUpdateRequest(token))
                Log.d(TAG, "FCM token sent to server")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send FCM token to server", e)
            }
        }
    }

    companion object {
        private const val TAG = "FcmService"
    }
}
