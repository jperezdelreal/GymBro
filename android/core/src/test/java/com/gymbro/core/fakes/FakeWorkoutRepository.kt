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
    private var inProgressWorkout: com.gymbro.core.model.InProgressWorkout? = null
    
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

    override suspend fun saveInProgressWorkout(inProgressWorkout: com.gymbro.core.model.InProgressWorkout) {
        this.inProgressWorkout = inProgressWorkout
    }

    override suspend fun getInProgressWorkout(): com.gymbro.core.model.InProgressWorkout? {
        return inProgressWorkout
    }

    override suspend fun clearInProgressWorkout(workoutId: String) {
        if (inProgressWorkout?.workoutId == workoutId) {
            inProgressWorkout = null
        }
    }

    override suspend fun getTotalCompletedWorkoutsCount(): Int {
        return workouts.value.values.count { it.completedAt != null }
    }

    override suspend fun getActiveDaysCount(): Int {
        return workouts.value.values
            .filter { it.completedAt != null }
            .map { it.startedAt.epochSecond / 86400 }
            .distinct()
            .size
    }

    override suspend fun getCurrentStreak(): Int {
        val completedWorkouts = workouts.value.values
            .filter { it.completedAt != null }
            .sortedByDescending { it.completedAt }
        
        if (completedWorkouts.isEmpty()) return 0
        
        var streak = 0
        var lastDate = completedWorkouts.first().completedAt!!.epochSecond / 86400
        val today = java.time.Instant.now().epochSecond / 86400
        
        if (today - lastDate > 7) return 0
        
        for (workout in completedWorkouts) {
            val workoutDay = workout.completedAt!!.epochSecond / 86400
            if (lastDate - workoutDay <= 7) {
                streak++
                lastDate = workoutDay
            } else {
                break
            }
        }
        
        return streak
    }

    override suspend fun getExerciseHistory(exerciseId: String, limit: Int): List<com.gymbro.core.repository.ExerciseHistorySession> {
        val exerciseUuid = try {
            UUID.fromString(exerciseId)
        } catch (e: IllegalArgumentException) {
            return emptyList()
        }
        
        return workouts.value.values
            .filter { it.completedAt != null }
            .sortedByDescending { it.completedAt }
            .take(limit)
            .mapNotNull { workout ->
                val sets = workout.sets.filter { it.exerciseId == exerciseUuid }
                if (sets.isEmpty()) null
                else com.gymbro.core.repository.ExerciseHistorySession(
                    workoutDate = workout.startedAt,
                    sets = sets
                )
            }
    }
}
