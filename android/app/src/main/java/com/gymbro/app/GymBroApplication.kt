package com.gymbro.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gymbro.core.notification.NotificationHelper
import com.gymbro.core.notification.ReminderScheduler
import com.gymbro.core.preferences.UserPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymBroApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: dagger.Lazy<NotificationHelper>

    @Inject
    lateinit var reminderScheduler: dagger.Lazy<ReminderScheduler>

    @Inject
    lateinit var userPreferences: dagger.Lazy<UserPreferences>

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Defer all non-essential initialization to background thread
        // to avoid blocking the main thread during cold start
        applicationScope.launch {
            notificationHelper.get().createNotificationChannels()

            val notificationsEnabled = userPreferences.get().notificationsEnabled.first()
            if (notificationsEnabled) {
                reminderScheduler.get().scheduleWorkoutReminders()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
