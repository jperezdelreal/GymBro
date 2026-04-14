package com.gymbro.feature.settings

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.notification.ReminderScheduler
import com.gymbro.core.R
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.TrainingPhase
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.feature.common.TooltipManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val reminderScheduler: ReminderScheduler,
    private val tooltipManager: TooltipManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSettings()
        checkHealthConnect()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferences.weightUnit,
                userPreferences.defaultRestTimer,
                userPreferences.autoStartRestTimer,
                userPreferences.notificationsEnabled,
                userPreferences.trainingPhase,
            ) { values: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                SettingsState(
                    weightUnit = values[0] as WeightUnit,
                    trainingPhase = values[4] as TrainingPhase,
                    themePreference = _state.value.themePreference,
                    defaultRestTimer = values[1] as Int,
                    autoStartRestTimer = values[2] as Boolean,
                    notificationsEnabled = values[3] as Boolean,
                    isHealthConnectAvailable = _state.value.isHealthConnectAvailable,
                    isHealthConnectConnected = _state.value.isHealthConnectConnected,
                    appVersion = getAppVersion(),
                    isLoading = false,
                )
            }.combine(userPreferences.themePreference) { state, theme ->
                state.copy(themePreference = theme)
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    private fun checkHealthConnect() {
        viewModelScope.launch {
            val sdkStatus = HealthConnectClient.getSdkStatus(context)
            val isAvailable = sdkStatus == HealthConnectClient.SDK_AVAILABLE
            _state.update { it.copy(isHealthConnectAvailable = isAvailable) }

            if (isAvailable) {
                try {
                    val client = HealthConnectClient.getOrCreate(context)
                    _state.update { it.copy(isHealthConnectConnected = true) }
                } catch (e: Exception) {
                    _state.update { it.copy(isHealthConnectConnected = false) }
                }
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetWeightUnit -> setWeightUnit(event.unit)
            is SettingsEvent.SetTrainingPhase -> setTrainingPhase(event.phase)
            is SettingsEvent.SetThemePreference -> setThemePreference(event.theme)
            is SettingsEvent.SetDefaultRestTimer -> setDefaultRestTimer(event.seconds)
            is SettingsEvent.SetAutoStartRestTimer -> setAutoStartRestTimer(event.enabled)
            is SettingsEvent.SetNotifications -> setNotifications(event.enabled)
            is SettingsEvent.ClearAllData -> clearAllData()
            is SettingsEvent.RedoSetup -> redoSetup()
            is SettingsEvent.OpenHealthConnect -> openHealthConnect()
            is SettingsEvent.SendFeedback -> sendFeedback()
            is SettingsEvent.ViewLicenses -> viewLicenses()
            is SettingsEvent.NavigateBack -> navigateBack()
            is SettingsEvent.ResetTooltips -> resetTooltips()
        }
    }

    private fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            userPreferences.setWeightUnit(unit)
        }
    }

    private fun setTrainingPhase(phase: TrainingPhase) {
        viewModelScope.launch {
            userPreferences.setTrainingPhase(phase)
        }
    }

    private fun setThemePreference(theme: UserPreferences.ThemePreference) {
        viewModelScope.launch {
            userPreferences.setThemePreference(theme)
        }
    }

    private fun setDefaultRestTimer(seconds: Int) {
        viewModelScope.launch {
            userPreferences.setDefaultRestTimer(seconds)
        }
    }

    private fun setAutoStartRestTimer(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoStartRestTimer(enabled)
        }
    }

    private fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleWorkoutReminders()
            } else {
                reminderScheduler.cancelWorkoutReminders()
            }
        }
    }

    private fun clearAllData() {
        viewModelScope.launch {
            userPreferences.clearAllData()
            _effects.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_data_cleared)))
        }
    }

    private fun redoSetup() {
        viewModelScope.launch {
            userPreferences.setOnboardingComplete(false)
            _effects.send(SettingsEffect.NavigateToOnboarding)
        }
    }

    private fun openHealthConnect() {
        viewModelScope.launch {
            _effects.send(SettingsEffect.OpenHealthConnectSettings)
        }
    }

    private fun sendFeedback() {
        viewModelScope.launch {
            _effects.send(SettingsEffect.OpenUrl("https://github.com/yourusername/gymbro/issues"))
        }
    }

    private fun viewLicenses() {
        viewModelScope.launch {
            _effects.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_licenses_shown)))
        }
    }

    private fun resetTooltips() {
        viewModelScope.launch {
            tooltipManager.resetAll()
            _effects.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_tooltips_reset_done)))
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effects.send(SettingsEffect.NavigateBack)
        }
    }
}
