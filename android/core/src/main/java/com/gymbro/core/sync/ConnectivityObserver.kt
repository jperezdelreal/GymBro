package com.gymbro.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _onConnectivityRestored = MutableStateFlow(0L)
    val onConnectivityRestored: StateFlow<Long> = _onConnectivityRestored.asStateFlow()
    
    init {
        updateConnectivityState()
        registerNetworkCallback()
    }
    
    private fun updateConnectivityState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        _isConnected.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        Log.d(TAG, "Connectivity state: ${_isConnected.value}")
    }
    
    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = !_isConnected.value
                _isConnected.value = true
                if (wasOffline) {
                    _onConnectivityRestored.value = System.currentTimeMillis()
                    Log.d(TAG, "Connectivity restored")
                }
            }
            
            override fun onLost(network: Network) {
                _isConnected.value = false
                Log.d(TAG, "Connectivity lost")
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val wasOffline = !_isConnected.value
                _isConnected.value = hasInternet
                if (hasInternet && wasOffline) {
                    _onConnectivityRestored.value = System.currentTimeMillis()
                    Log.d(TAG, "Connectivity restored (capabilities changed)")
                }
            }
        })
    }
    
    companion object {
        private const val TAG = "ConnectivityObserver"
    }
}
