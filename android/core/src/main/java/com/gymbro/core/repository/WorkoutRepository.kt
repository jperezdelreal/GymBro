package com.gymbro.core.repository

import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun startWorkout(): Workout
    suspend fun addSet(workoutId: String, set: ExerciseSet)
    suspend fun removeSet(setId: String)
    suspend fun completeWorkout(workoutId: String, durationSeconds: Long, notes: String)
    suspend fun getWorkout(workoutId: String): Workout?
    fun observeWorkout(workoutId: String): Flow<Workout?>
    fun getRecentWorkouts(limit: Int = 20): Flow<List<Workout>>
    suspend fun getBestWeight(exerciseId: String, reps: Int): Double?
}
