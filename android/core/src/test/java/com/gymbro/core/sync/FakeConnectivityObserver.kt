package com.gymbro.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake ConnectivityObserver for testing network state transitions.
 */
class FakeConnectivityObserver {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _onConnectivityRestored = MutableStateFlow(0L)
    val onConnectivityRestored: StateFlow<Long> = _onConnectivityRestored

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun triggerConnectivityRestored() {
        _onConnectivityRestored.value = System.currentTimeMillis()
    }
}
