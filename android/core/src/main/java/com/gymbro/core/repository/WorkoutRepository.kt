package com.gymbro.core.repository

import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.Workout
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class ExerciseHistorySession(
    val workoutDate: Instant,
    val sets: List<ExerciseSet>,
)

interface WorkoutRepository {
    suspend fun startWorkout(): Workout
    suspend fun addSet(workoutId: String, set: ExerciseSet)
    suspend fun removeSet(setId: String)
    suspend fun updateSet(setId: String, weight: Double, reps: Int, rpe: Double?)
    suspend fun completeWorkout(workoutId: String, durationSeconds: Long, notes: String)
    suspend fun getWorkout(workoutId: String): Workout?
    fun observeWorkout(workoutId: String): Flow<Workout?>
    fun getRecentWorkouts(limit: Int = 20): Flow<List<Workout>>
    suspend fun getBestWeight(exerciseId: String, reps: Int): Double?
    suspend fun getDaysSinceLastWorkout(): Int?
    
    suspend fun saveInProgressWorkout(inProgressWorkout: com.gymbro.core.model.InProgressWorkout)
    suspend fun getInProgressWorkout(): com.gymbro.core.model.InProgressWorkout?
    suspend fun clearInProgressWorkout(workoutId: String)
    suspend fun clearAllInProgressWorkouts()
    
    suspend fun getTotalCompletedWorkoutsCount(): Int
    suspend fun getActiveDaysCount(): Int
    suspend fun getCurrentStreak(): Int
    
    suspend fun getExerciseHistory(exerciseId: String, limit: Int = 10): List<ExerciseHistorySession>
}
