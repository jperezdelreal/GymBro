package com.gymbro.feature.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.health.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecoveryState())
    val state: StateFlow<RecoveryState> = _state.asStateFlow()

    private val _effects = Channel<RecoveryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        checkAvailabilityAndLoad()
    }

    fun onEvent(event: RecoveryEvent) {
        when (event) {
            is RecoveryEvent.RequestPermissions -> {
                viewModelScope.launch {
                    _effects.send(RecoveryEffect.LaunchPermissionRequest)
                }
            }
            is RecoveryEvent.RefreshData -> loadData()
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        _state.update { it.copy(permissionsGranted = granted) }
        if (granted) loadData()
    }

    private fun checkAvailabilityAndLoad() {
        viewModelScope.launch {
            val available = healthConnectRepository.isAvailable()
            val hasPermissions = if (available) {
                healthConnectRepository.hasPermissions()
            } else false

            _state.update {
                it.copy(
                    healthConnectAvailable = available,
                    permissionsGranted = hasPermissions,
                )
            }

            if (available && hasPermissions) {
                loadData()
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val metrics = healthConnectRepository.getRecoveryMetrics()
                val sleep = healthConnectRepository.getSleepHistory(7)

                _state.update {
                    it.copy(
                        recoveryMetrics = metrics,
                        sleepHistory = sleep,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load recovery data",
                    )
                }
            }
        }
    }
}
