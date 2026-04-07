package com.gymbro.core.health

import com.gymbro.core.model.RecoveryMetrics
import com.gymbro.core.model.SleepData

interface HealthConnectRepository {
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun getSleepHistory(days: Int = 7): List<SleepData>
    suspend fun getRecoveryMetrics(): RecoveryMetrics
    suspend fun writeWorkoutSession(
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): Boolean
}
