package com.gymbro.core.sync.model

sealed class SyncStatusModel {
    object Idle : SyncStatusModel()
    object Syncing : SyncStatusModel()
    data class Error(val message: String, val retryable: Boolean = true) : SyncStatusModel()
    data class Success(val lastSyncTimestamp: Long) : SyncStatusModel()
    object Offline : SyncStatusModel()
}
