package com.gymbro.core.model

import com.gymbro.core.model.Exercise

data class InProgressWorkout(
    val workoutId: String,
    val exercises: List<InProgressExercise>,
    val elapsedSeconds: Long,
    val totalVolume: Double,
    val totalSets: Int,
    val restTimerSeconds: Int,
    val restTimerTotal: Int,
    val isRestTimerActive: Boolean,
)

data class InProgressExercise(
    val exercise: Exercise,
    val sets: List<InProgressSet>,
)

data class InProgressSet(
    val id: String,
    val setNumber: Int,
    val weight: String,
    val reps: String,
    val rpe: String,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
)
