package com.gymbro.core.sync.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.gymbro.core.sync.model.FirestoreUserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues sync operations when offline and flushes them when connectivity resumes.
 * This is the orchestrator between the local Room DB and Firestore.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudSyncService: CloudSyncService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingQueue = Channel<SyncOperation>(Channel.UNLIMITED)

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        monitorConnectivity()
        processQueue()
    }

    fun queueExerciseSync() {
        pendingQueue.trySend(SyncOperation.EXERCISES)
    }

    fun queueWorkoutSync() {
        pendingQueue.trySend(SyncOperation.WORKOUTS)
    }

    fun queueProfileSync(profile: FirestoreUserProfile) {
        pendingQueue.trySend(SyncOperation.PROFILE)
        // Store profile for when we actually process
        _pendingProfile = profile
    }

    fun syncNow() {
        queueExerciseSync()
        queueWorkoutSync()
    }

    @Volatile
    private var _pendingProfile: FirestoreUserProfile? = null

    private fun processQueue() {
        scope.launch {
            for (operation in pendingQueue) {
                if (!_isOnline.value) {
                    _syncStatus.value = SyncStatus.OFFLINE
                    // Re-queue and wait for connectivity
                    pendingQueue.trySend(operation)
                    continue
                }

                _syncStatus.value = SyncStatus.SYNCING
                val result = when (operation) {
                    SyncOperation.EXERCISES -> cloudSyncService.syncExercises()
                    SyncOperation.WORKOUTS -> cloudSyncService.syncWorkouts()
                    SyncOperation.PROFILE -> {
                        val profile = _pendingProfile
                        if (profile != null) {
                            cloudSyncService.syncUserProfile(profile)
                        } else {
                            Result.success(Unit)
                        }
                    }
                }

                _syncStatus.value = if (result.isSuccess) {
                    SyncStatus.SUCCESS
                } else {
                    Log.e(TAG, "Sync failed for $operation", result.exceptionOrNull())
                    SyncStatus.ERROR
                }
            }
        }
    }

    private fun monitorConnectivity() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check current state
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                // Flush pending operations
                scope.launch {
                    cloudSyncService.resolveConflicts()
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                _syncStatus.value = SyncStatus.OFFLINE
            }
        })
    }

    private enum class SyncOperation {
        EXERCISES,
        WORKOUTS,
        PROFILE,
    }

    companion object {
        private const val TAG = "OfflineSyncManager"
    }
}
