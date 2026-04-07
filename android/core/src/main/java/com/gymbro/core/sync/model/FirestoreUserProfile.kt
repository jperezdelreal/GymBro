package com.gymbro.core.sync.model

/**
 * Firestore document for user preferences and settings.
 * Stored at users/{userId}/profile.
 */
data class FirestoreUserProfile(
    val userId: String = "",
    val displayName: String = "",
    val weightUnit: String = "KG",
    val autoSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
