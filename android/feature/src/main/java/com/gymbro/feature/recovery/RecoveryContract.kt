package com.gymbro.feature.recovery

import com.gymbro.core.model.RecoveryMetrics
import com.gymbro.core.model.SleepData

data class RecoveryState(
    val recoveryMetrics: RecoveryMetrics = RecoveryMetrics(),
    val sleepHistory: List<SleepData> = emptyList(),
    val healthConnectAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val manualEntry: ManualRecoveryEntry = ManualRecoveryEntry(),
    val isManualMode: Boolean = false,
)

data class ManualRecoveryEntry(
    val sleepHours: Float = 7.0f, // 0-12
    val readinessScore: Float = 5f, // 1-10
    val entryDate: Long = System.currentTimeMillis(),
) {
    val recoveryScore: Int
        get() = ((sleepHours / 8f) * 0.5f + (readinessScore / 10f) * 0.5f * 100f).toInt().coerceIn(0, 100)
    
    val readinessLabel: String
        get() = when {
            readinessScore >= 9f -> "Crushed"
            readinessScore >= 7f -> "Good"
            readinessScore >= 5f -> "OK"
            readinessScore >= 3f -> "Tired"
            else -> "Wrecked"
        }
}

sealed interface RecoveryEvent {
    data object RequestPermissions : RecoveryEvent
    data object RefreshData : RecoveryEvent
    data class UpdateSleepHours(val value: Float) : RecoveryEvent
    data class UpdateReadinessScore(val value: Float) : RecoveryEvent
    data class UpdateEntryDate(val date: Long) : RecoveryEvent
    data object SaveManualEntry : RecoveryEvent
}

sealed interface RecoveryEffect {
    data object LaunchPermissionRequest : RecoveryEffect
    data class ShowError(val message: String) : RecoveryEffect
}
