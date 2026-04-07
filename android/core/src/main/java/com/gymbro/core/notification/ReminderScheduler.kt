package com.gymbro.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val WORK_NAME_WORKOUT_REMINDER = "workout_reminder_periodic"
        private const val REPEAT_INTERVAL_HOURS = 24L
    }

    fun scheduleWorkoutReminders() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(REPEAT_INTERVAL_HOURS, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_WORKOUT_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelWorkoutReminders() {
        workManager.cancelUniqueWork(WORK_NAME_WORKOUT_REMINDER)
    }
}
