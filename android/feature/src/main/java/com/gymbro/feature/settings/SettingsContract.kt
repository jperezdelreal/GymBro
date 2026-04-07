package com.gymbro.feature.settings

import com.gymbro.core.preferences.UserPreferences.WeightUnit

data class SettingsState(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestTimer: Int = 90,
    val autoStartRestTimer: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val isHealthConnectAvailable: Boolean = false,
    val isHealthConnectConnected: Boolean = false,
    val appVersion: String = "",
    val isLoading: Boolean = true,
)

sealed interface SettingsEvent {
    data class SetWeightUnit(val unit: WeightUnit) : SettingsEvent
    data class SetDefaultRestTimer(val seconds: Int) : SettingsEvent
    data class SetAutoStartRestTimer(val enabled: Boolean) : SettingsEvent
    data class SetNotifications(val enabled: Boolean) : SettingsEvent
    data object ClearAllData : SettingsEvent
    data object OpenHealthConnect : SettingsEvent
    data object SendFeedback : SettingsEvent
    data object ViewLicenses : SettingsEvent
    data object NavigateBack : SettingsEvent
}

sealed interface SettingsEffect {
    data class ShowMessage(val message: String) : SettingsEffect
    data class ShowError(val message: String) : SettingsEffect
    data object NavigateBack : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect
    data object OpenHealthConnectSettings : SettingsEffect
}
