package com.gymbro.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_WORKOUT_REMINDERS = "workout_reminders"
        const val CHANNEL_PR_CELEBRATIONS = "pr_celebrations"
        const val NOTIFICATION_ID_WORKOUT_REMINDER = 1001
        private const val ACCENT_GREEN = 0xFF00FF87.toInt()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val workoutChannel = NotificationChannel(
                CHANNEL_WORKOUT_REMINDERS,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Get reminded to train and stay consistent"
                enableLights(true)
                lightColor = ACCENT_GREEN
            }

            val prChannel = NotificationChannel(
                CHANNEL_PR_CELEBRATIONS,
                "PR Celebrations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebrate your personal records!"
                enableLights(true)
                lightColor = ACCENT_GREEN
            }

            notificationManager.createNotificationChannels(listOf(workoutChannel, prChannel))
        }
    }

    fun showWorkoutReminder(title: String, message: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(ACCENT_GREEN)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_WORKOUT_REMINDER, notification)
    }

    fun cancelWorkoutReminder() {
        notificationManager.cancel(NOTIFICATION_ID_WORKOUT_REMINDER)
    }
}
