package com.gymbro.core.sync.model

/**
 * Firestore document representation of a Workout with embedded sets.
 * Denormalized for efficient single-document reads.
 */
data class FirestoreWorkout(
    val id: String = "",
    val name: String = "",
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val durationSeconds: Long = 0L,
    val notes: String = "",
    val completed: Boolean = false,
    val sets: List<FirestoreWorkoutSet> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
)

data class FirestoreWorkoutSet(
    val id: String = "",
    val exerciseId: String = "",
    val setNumber: Int = 0,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val completedAt: Long = 0L,
)
