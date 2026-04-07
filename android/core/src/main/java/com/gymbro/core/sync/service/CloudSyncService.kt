package com.gymbro.core.sync.service

import com.gymbro.core.sync.model.FirestoreUserProfile
import kotlinx.coroutines.flow.Flow

/** Sync status for UI display. */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE,
    DISABLED,
}

/**
 * Cloud sync contract. All operations are offline-safe — they queue changes
 * when there is no network and flush when connectivity resumes.
 */
interface CloudSyncService {

    /** Push local exercises to cloud, pull remote changes. */
    suspend fun syncExercises(): Result<Unit>

    /** Push local workouts + sets to cloud, pull remote changes. */
    suspend fun syncWorkouts(): Result<Unit>

    /** Push user profile/settings to cloud. */
    suspend fun syncUserProfile(profile: FirestoreUserProfile): Result<Unit>

    /**
     * Last-write-wins conflict resolution for MVP.
     * Compares updatedAt timestamps and keeps the newer document.
     */
    suspend fun resolveConflicts()

    /** Observe real-time changes from Firestore snapshot listeners. */
    fun observeChanges(): Flow<SyncStatus>
}
