package com.gymbro.core.sync.service

import com.gymbro.core.sync.model.FirestoreUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpCloudSyncService @Inject constructor() : CloudSyncService {

    override suspend fun syncExercises(): Result<Unit> =
        Result.failure(Exception("Firebase is not configured. Cloud sync is disabled."))

    override suspend fun syncWorkouts(): Result<Unit> =
        Result.failure(Exception("Firebase is not configured. Cloud sync is disabled."))

    override suspend fun syncUserProfile(profile: FirestoreUserProfile): Result<Unit> =
        Result.failure(Exception("Firebase is not configured. Cloud sync is disabled."))

    override suspend fun resolveConflicts() {
        // No-op when Firebase is disabled
    }

    override fun observeChanges(): Flow<SyncStatus> = flowOf(SyncStatus.DISABLED)
}
