package com.gymbro.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "in_progress_workouts")
data class InProgressWorkoutEntity(
    @PrimaryKey
    val workoutId: String,
    val exercisesJson: String, // JSON serialization of WorkoutExerciseUi list
    val elapsedSeconds: Long,
    val totalVolume: Double,
    val totalSets: Int,
    val restTimerSeconds: Int,
    val restTimerTotal: Int,
    val isRestTimerActive: Boolean,
    val lastSavedAt: Long = System.currentTimeMillis(),
)
