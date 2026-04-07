package com.gymbro.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gymbro.core.preferences.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WorkoutReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val userPreferences: UserPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationsEnabled = userPreferences.notificationsEnabled.first()
            
            if (!notificationsEnabled) {
                return Result.success()
            }

            val motivationalMessages = listOf(
                "Time to hit the gym! 💪",
                "Your muscles are waiting for you!",
                "Don't skip leg day bro! 🦵",
                "Gains don't come from the couch!",
                "Let's get those PRs! 🔥",
                "The iron is calling! ⚡",
                "Consistency is key! Let's train!",
            )

            val message = motivationalMessages.random()
            notificationHelper.showWorkoutReminder("GymBro Reminder", message)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
