package com.gymbro.core.sync.model

/**
 * Firestore document representation of an Exercise.
 * All fields are nullable/defaulted for Firestore deserialization.
 */
data class FirestoreExercise(
    val id: String = "",
    val name: String = "",
    val muscleGroup: String = "",
    val category: String = "COMPOUND",
    val equipment: String = "BARBELL",
    val description: String = "",
    val youtubeUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
)
