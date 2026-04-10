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
    val sleepQuality: Float = 5f, // 1-10
    val muscleSoreness: Float = 5f, // 1-10
    val energyLevel: Float = 5f, // 1-10
) {
    val recoveryScore: Int
        get() = ((sleepQuality + (11f - muscleSoreness) + energyLevel) / 3f * 10f).toInt().coerceIn(0, 100)
}

sealed interface RecoveryEvent {
    data object RequestPermissions : RecoveryEvent
    data object RefreshData : RecoveryEvent
    data class UpdateSleepQuality(val value: Float) : RecoveryEvent
    data class UpdateMuscleSoreness(val value: Float) : RecoveryEvent
    data class UpdateEnergyLevel(val value: Float) : RecoveryEvent
    data object SaveManualEntry : RecoveryEvent
}

sealed interface RecoveryEffect {
    data object LaunchPermissionRequest : RecoveryEffect
    data class ShowError(val message: String) : RecoveryEffect
}
