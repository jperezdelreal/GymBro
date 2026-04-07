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
)

sealed interface RecoveryEvent {
    data object RequestPermissions : RecoveryEvent
    data object RefreshData : RecoveryEvent
}

sealed interface RecoveryEffect {
    data object LaunchPermissionRequest : RecoveryEffect
    data class ShowError(val message: String) : RecoveryEffect
}
