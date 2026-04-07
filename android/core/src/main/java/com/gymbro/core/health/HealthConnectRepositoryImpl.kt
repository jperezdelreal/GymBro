package com.gymbro.core.health

import com.gymbro.core.model.RecoveryMetrics
import com.gymbro.core.model.SleepData
import com.gymbro.core.repository.WorkoutRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepositoryImpl @Inject constructor(
    private val healthConnectService: HealthConnectService,
    private val workoutRepository: WorkoutRepository,
) : HealthConnectRepository {

    override suspend fun isAvailable(): Boolean {
        return healthConnectService.isAvailable()
    }

    override suspend fun hasPermissions(): Boolean {
        return healthConnectService.hasAllPermissions()
    }

    override suspend fun getSleepHistory(days: Int): List<SleepData> {
        return healthConnectService.readSleepData(days)
    }

    override suspend fun getRecoveryMetrics(): RecoveryMetrics {
        val sleepData = healthConnectService.readSleepData(1)
        val lastNightSleep = sleepData.firstOrNull()
        val sleepHours = lastNightSleep?.durationHours ?: 0.0

        val restingHR = healthConnectService.getRestingHeartRate()
        val hrv = healthConnectService.getHRV()
        val steps = healthConnectService.readStepsToday()

        val daysSinceWorkout = workoutRepository.getDaysSinceLastWorkout()

        val readinessScore = RecoveryMetrics.calculateReadiness(
            sleepHours = sleepHours,
            hrv = hrv,
            daysSinceLastWorkout = daysSinceWorkout,
        )

        return RecoveryMetrics(
            sleepHours = sleepHours,
            restingHR = restingHR,
            hrv = hrv,
            steps = steps,
            daysSinceLastWorkout = daysSinceWorkout,
            readinessScore = readinessScore,
        )
    }

    override suspend fun writeWorkoutSession(
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): Boolean {
        return healthConnectService.writeWorkoutSession(
            title = title,
            startTime = Instant.ofEpochMilli(startTimeMillis),
            endTime = Instant.ofEpochMilli(endTimeMillis),
        )
    }
}
