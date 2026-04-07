package com.gymbro.core.sync.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.gymbro.core.sync.ConnectivityObserver
import com.gymbro.core.sync.model.FirestoreUserProfile
import com.gymbro.core.sync.model.SyncStatusModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncOperation {
    data object Exercises : SyncOperation()
    data object Workouts : SyncOperation()
    data object Profile : SyncOperation()
}

/**
 * Queues sync operations when offline and flushes them when connectivity resumes.
 * This is the orchestrator between the local Room DB and Firestore.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudSyncService: CloudSyncService,
    private val connectivityObserver: ConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingQueue = mutableListOf<SyncOperation>()
    private val prefs: SharedPreferences = context.getSharedPreferences("sync_queue", Context.MODE_PRIVATE)
    
    @Volatile
    private var _pendingProfile: FirestoreUserProfile? = null
    
    private val _syncStatusModel = MutableStateFlow<SyncStatusModel>(SyncStatusModel.Idle)
    val syncStatusModel: StateFlow<SyncStatusModel> = _syncStatusModel.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isConnected

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        loadQueueFromDisk()
        observeConnectivity()
    }

    private fun loadQueueFromDisk() {
        try {
            val exercises = prefs.getBoolean("queue_exercises", false)
            val workouts = prefs.getBoolean("queue_workouts", false)
            val profile = prefs.getBoolean("queue_profile", false)
            
            synchronized(pendingQueue) {
                pendingQueue.clear()
                if (exercises) pendingQueue.add(SyncOperation.Exercises)
                if (workouts) pendingQueue.add(SyncOperation.Workouts)
                if (profile) pendingQueue.add(SyncOperation.Profile)
            }
            Log.d(TAG, "Loaded ${pendingQueue.size} operations from disk")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load queue from disk", e)
        }
    }

    private fun saveQueueToDisk() {
        try {
            val editor = prefs.edit()
            var hasExercises = false
            var hasWorkouts = false
            var hasProfile = false
            
            synchronized(pendingQueue) {
                pendingQueue.forEach { op ->
                    when (op) {
                        is SyncOperation.Exercises -> hasExercises = true
                        is SyncOperation.Workouts -> hasWorkouts = true
                        is SyncOperation.Profile -> hasProfile = true
                    }
                }
            }
            
            editor.putBoolean("queue_exercises", hasExercises)
            editor.putBoolean("queue_workouts", hasWorkouts)
            editor.putBoolean("queue_profile", hasProfile)
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save queue to disk", e)
        }
    }

    private fun observeConnectivity() {
        scope.launch {
            connectivityObserver.onConnectivityRestored.collect { timestamp ->
                if (timestamp > 0) {
                    Log.d(TAG, "Connectivity restored, flushing queue")
                    cloudSyncService.resolveConflicts()
                    processQueue()
                }
            }
        }
    }

    fun queueExerciseSync() {
        addToQueue(SyncOperation.Exercises)
    }

    fun queueWorkoutSync() {
        addToQueue(SyncOperation.Workouts)
    }

    fun queueProfileSync(profile: FirestoreUserProfile) {
        _pendingProfile = profile
        addToQueue(SyncOperation.Profile)
    }

    private fun addToQueue(operation: SyncOperation) {
        synchronized(pendingQueue) {
            val alreadyQueued = pendingQueue.any { existing ->
                when {
                    existing is SyncOperation.Exercises && operation is SyncOperation.Exercises -> true
                    existing is SyncOperation.Workouts && operation is SyncOperation.Workouts -> true
                    existing is SyncOperation.Profile && operation is SyncOperation.Profile -> true
                    else -> false
                }
            }
            
            if (!alreadyQueued) {
                if (pendingQueue.size >= MAX_QUEUE_SIZE) {
                    Log.w(TAG, "Queue at max size ($MAX_QUEUE_SIZE), removing oldest operation")
                    pendingQueue.removeAt(0)
                }
                pendingQueue.add(operation)
                saveQueueToDisk()
            }
        }
        
        if (connectivityObserver.isConnected.value) {
            processQueue()
        } else {
            _syncStatus.value = SyncStatus.OFFLINE
            _syncStatusModel.value = SyncStatusModel.Offline
        }
    }

    fun syncNow() {
        queueExerciseSync()
        queueWorkoutSync()
    }

    private fun processQueue() {
        scope.launch {
            synchronized(pendingQueue) {
                if (pendingQueue.isEmpty()) return@launch
            }
            
            while (true) {
                val operation = synchronized(pendingQueue) {
                    pendingQueue.firstOrNull()
                } ?: break
                
                if (!connectivityObserver.isConnected.value) {
                    _syncStatus.value = SyncStatus.OFFLINE
                    _syncStatusModel.value = SyncStatusModel.Offline
                    Log.d(TAG, "No connectivity, pausing queue processing")
                    break
                }

                _syncStatus.value = SyncStatus.SYNCING
                _syncStatusModel.value = SyncStatusModel.Syncing
                
                val result = when (operation) {
                    is SyncOperation.Exercises -> cloudSyncService.syncExercises()
                    is SyncOperation.Workouts -> cloudSyncService.syncWorkouts()
                    is SyncOperation.Profile -> {
                        val profile = _pendingProfile
                        if (profile != null) {
                            cloudSyncService.syncUserProfile(profile)
                        } else {
                            Result.success(Unit)
                        }
                    }
                }

                if (result.isSuccess) {
                    synchronized(pendingQueue) {
                        pendingQueue.remove(operation)
                        saveQueueToDisk()
                    }
                    _syncStatus.value = SyncStatus.SUCCESS
                    _syncStatusModel.value = SyncStatusModel.Success(System.currentTimeMillis())
                    Log.d(TAG, "Sync operation succeeded: $operation")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Sync failed for $operation", error)
                    _syncStatus.value = SyncStatus.ERROR
                    _syncStatusModel.value = SyncStatusModel.Error(
                        message = error?.message ?: "Unknown error",
                        retryable = true
                    )
                    break
                }
            }
        }
    }

    companion object {
        private const val TAG = "OfflineSyncManager"
        private const val MAX_QUEUE_SIZE = 100
    }
}
