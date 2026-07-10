package com.habitflowai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.habitflowai.MainActivity
import com.habitflowai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_DRIFT = "persona_drift"
        const val CHANNEL_REMINDER = "daily_reminder"
        const val CHANNEL_GENERAL = "general"
        private const val DRIFT_NOTIFICATION_ID = 1001
        private const val REMINDER_NOTIFICATION_ID = 1002
    }

    fun createNotificationChannels() {
        val driftChannel = NotificationChannel(
            CHANNEL_DRIFT,
            "Persona Drift Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Alerts when persona drift is detected" }

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER,
            "Daily Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Daily habit reminders and motivation" }

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "General Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Other app notifications" }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(listOf(driftChannel, reminderChannel, generalChannel))
    }

    fun showDriftDetectedNotification(
        driftScore: Double,
        suggestedPersona: String?,
        rationale: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "drift")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (suggestedPersona != null) {
            "Persona Drift Detected"
        } else {
            "Persona Check Complete"
        }
        val message = if (suggestedPersona != null) {
            "You may be shifting toward $suggestedPersona (score: $driftScore)"
        } else {
            rationale?.take(120) ?: "Your persona is stable (score: $driftScore)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DRIFT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(DRIFT_NOTIFICATION_ID, notification)
    }

    fun showDailyReminder(personaType: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (personaType) {
            "Achiever" -> "Time to crush your goals! Check today's tasks."
            "Grower" -> "Small steps lead to big growth. Stay consistent!"
            "Socializer" -> "Connect and build habits together."
            "Explorer" -> "New challenges await. Explore your habits!"
            "Altruist" -> "Your habits make a difference. Keep going!"
            "Architect" -> "Stay structured. Your routine matters."
            else -> "Don't forget to check your daily habits!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("HabitFlow Daily Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(REMINDER_NOTIFICATION_ID, notification)
    }
}
