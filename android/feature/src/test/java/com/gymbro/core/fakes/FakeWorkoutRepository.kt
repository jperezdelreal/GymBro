package com.gymbro.core.fakes

import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.Workout
import com.gymbro.core.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class FakeWorkoutRepository : WorkoutRepository {
    
    private val workouts = MutableStateFlow<Map<String, Workout>>(emptyMap())
    
    fun setWorkouts(vararg workouts: Workout) {
        this.workouts.value = workouts.associateBy { it.id.toString() }
    }
    
    fun addWorkout(workout: Workout) {
        workouts.value = workouts.value + (workout.id.toString() to workout)
    }
    
    fun clearWorkouts() {
        workouts.value = emptyMap()
    }

    override suspend fun startWorkout(): Workout {
        val workout = Workout(
            id = UUID.randomUUID(),
            name = "New Workout",
            startedAt = Instant.now(),
            completedAt = null,
            sets = emptyList(),
            notes = ""
        )
        addWorkout(workout)
        return workout
    }

    override suspend fun addSet(workoutId: String, set: ExerciseSet) {
        val workout = workouts.value[workoutId] ?: return
        val updatedWorkout = workout.copy(sets = workout.sets + set)
        workouts.value = workouts.value + (workoutId to updatedWorkout)
    }

    override suspend fun removeSet(setId: String) {
        workouts.value = workouts.value.mapValues { (_, workout) ->
            workout.copy(sets = workout.sets.filterNot { it.id.toString() == setId })
        }
    }

    override suspend fun completeWorkout(workoutId: String, durationSeconds: Long, notes: String) {
        val workout = workouts.value[workoutId] ?: return
        val updatedWorkout = workout.copy(
            completedAt = Instant.now(),
            notes = notes
        )
        workouts.value = workouts.value + (workoutId to updatedWorkout)
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        return workouts.value[workoutId]
    }

    override fun observeWorkout(workoutId: String): Flow<Workout?> {
        return workouts.map { it[workoutId] }
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workouts.map { workoutsMap ->
            workoutsMap.values
                .filter { it.completedAt != null }
                .sortedByDescending { it.completedAt }
                .take(limit)
        }
    }

    override suspend fun getBestWeight(exerciseId: String, reps: Int): Double? {
        val exerciseUuid = try {
            UUID.fromString(exerciseId)
        } catch (e: IllegalArgumentException) {
            return null
        }
        
        return workouts.value.values
            .filter { it.completedAt != null }
            .flatMap { it.sets }
            .filter { it.exerciseId == exerciseUuid && it.reps == reps && !it.isWarmup }
            .maxOfOrNull { it.weightKg }
    }

    override suspend fun getDaysSinceLastWorkout(): Int? {
        val lastWorkout = workouts.value.values
            .filter { it.completedAt != null }
            .maxByOrNull { it.completedAt!! }
            ?: return null
        
        val now = Instant.now()
        val daysBetween = java.time.Duration.between(lastWorkout.completedAt, now).toDays()
        return daysBetween.toInt()
    }
}
