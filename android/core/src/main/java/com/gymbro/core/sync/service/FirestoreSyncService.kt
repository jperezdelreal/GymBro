package com.gymbro.core.sync.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.sync.ConnectivityObserver
import com.gymbro.core.sync.model.FirestoreUserProfile
import com.gymbro.core.sync.model.toFirestoreExercise
import com.gymbro.core.sync.model.toFirestoreWorkout
import com.gymbro.core.sync.retry.RetryConfig
import com.gymbro.core.sync.retry.retryWithBackoff
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val auth: FirebaseAuth?,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val connectivityObserver: ConnectivityObserver,
) : CloudSyncService {

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    private val deviceId: String = android.os.Build.MODEL

    private val userId: String?
        get() = auth?.currentUser?.uid

    override suspend fun syncExercises(): Result<Unit> {
        if (firestore == null || auth == null) {
            _syncStatus.value = SyncStatus.DISABLED
            return Result.failure(Exception("Firebase is not configured"))
        }

        if (!connectivityObserver.isConnected.value) {
            _syncStatus.value = SyncStatus.OFFLINE
            return Result.failure(Exception("No network connection"))
        }
        
        _syncStatus.value = SyncStatus.SYNCING
        
        return retryWithBackoff(
            config = RetryConfig(maxRetries = 3, initialDelayMs = 1000L),
            operation = "syncExercises"
        ) {
            val uid = userId ?: throw IllegalStateException("User not signed in")
            
            val exercises = exerciseDao.getAllExercises().first()
            val batch = firestore.batch()

            exercises.forEach { entity ->
                val firestoreExercise = entity.toFirestoreExercise(deviceId)
                val docRef = firestore.collection("users").document(uid)
                    .collection("exercises").document(entity.id)
                batch.set(docRef, firestoreExercise, SetOptions.merge())
            }

            batch.commit().await()
            Unit
        }.onSuccess {
            _syncStatus.value = SyncStatus.SUCCESS
        }.onFailure {
            Log.e(TAG, "syncExercises failed", it)
            _syncStatus.value = SyncStatus.ERROR
        }
    }

    override suspend fun syncWorkouts(): Result<Unit> {
        if (firestore == null || auth == null) {
            _syncStatus.value = SyncStatus.DISABLED
            return Result.failure(Exception("Firebase is not configured"))
        }

        if (!connectivityObserver.isConnected.value) {
            _syncStatus.value = SyncStatus.OFFLINE
            return Result.failure(Exception("No network connection"))
        }
        
        _syncStatus.value = SyncStatus.SYNCING
        
        return retryWithBackoff(
            config = RetryConfig(maxRetries = 3, initialDelayMs = 1000L),
            operation = "syncWorkouts"
        ) {
            val uid = userId ?: throw IllegalStateException("User not signed in")
            
            val workouts = workoutDao.getAllWorkoutsOnce()
            val batch = firestore.batch()

            workouts.forEach { workout ->
                val sets = workoutDao.getSetsForWorkout(workout.id)
                val firestoreWorkout = workout.toFirestoreWorkout(sets, deviceId)
                val docRef = firestore.collection("users").document(uid)
                    .collection("workouts").document(workout.id)
                batch.set(docRef, firestoreWorkout, SetOptions.merge())
            }

            batch.commit().await()
            Unit
        }.onSuccess {
            _syncStatus.value = SyncStatus.SUCCESS
        }.onFailure {
            Log.e(TAG, "syncWorkouts failed", it)
            _syncStatus.value = SyncStatus.ERROR
        }
    }

    override suspend fun syncUserProfile(profile: FirestoreUserProfile): Result<Unit> {
        if (firestore == null || auth == null) {
            _syncStatus.value = SyncStatus.DISABLED
            return Result.failure(Exception("Firebase is not configured"))
        }

        if (!connectivityObserver.isConnected.value) {
            _syncStatus.value = SyncStatus.OFFLINE
            return Result.failure(Exception("No network connection"))
        }
        
        _syncStatus.value = SyncStatus.SYNCING
        
        return retryWithBackoff(
            config = RetryConfig(maxRetries = 3, initialDelayMs = 1000L),
            operation = "syncUserProfile"
        ) {
            val uid = userId ?: throw IllegalStateException("User not signed in")
            
            firestore.collection("users").document(uid)
                .set(profile, SetOptions.merge())
                .await()
            Unit
        }.onSuccess {
            _syncStatus.value = SyncStatus.SUCCESS
        }.onFailure {
            Log.e(TAG, "syncUserProfile failed", it)
            _syncStatus.value = SyncStatus.ERROR
        }
    }

    override suspend fun resolveConflicts() {
        // MVP: last-write-wins based on updatedAt timestamp.
        // The Firestore SetOptions.merge() with updatedAt = currentTimeMillis
        // ensures the latest writer wins on a per-field basis.
        Log.d(TAG, "Conflict resolution: using last-write-wins strategy")
    }

    override fun observeChanges(): Flow<SyncStatus> = callbackFlow {
        if (firestore == null || auth == null) {
            trySend(SyncStatus.DISABLED)
            close()
            return@callbackFlow
        }

        val uid = userId
        if (uid == null) {
            trySend(SyncStatus.DISABLED)
            close()
            return@callbackFlow
        }

        val listeners = mutableListOf<ListenerRegistration>()

        val workoutListener = firestore.collection("users").document(uid)
            .collection("workouts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(SyncStatus.ERROR)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.metadata.isFromCache) {
                    trySend(SyncStatus.SUCCESS)
                }
            }
        listeners.add(workoutListener)

        val exerciseListener = firestore.collection("users").document(uid)
            .collection("exercises")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(SyncStatus.ERROR)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.metadata.isFromCache) {
                    trySend(SyncStatus.SUCCESS)
                }
            }
        listeners.add(exerciseListener)

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    companion object {
        private const val TAG = "FirestoreSyncService"
    }
}
