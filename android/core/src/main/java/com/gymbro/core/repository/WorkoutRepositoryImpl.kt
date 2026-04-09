package com.gymbro.core.repository

import android.util.Log
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {

    override suspend fun startWorkout(): Workout {
        val now = Instant.now()
        val result = retryWithBackoff {
            runCatchingAsResult {
                val entity = WorkoutEntity(
                    id = UUID.randomUUID().toString(),
                    startedAt = now.toEpochMilli(),
                )
                workoutDao.insertWorkout(entity)
                Workout(
                    id = UUID.fromString(entity.id),
                    name = "",
                    startedAt = now,
                )
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to start workout: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun addSet(workoutId: String, set: ExerciseSet) {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val entity = WorkoutSetEntity(
                    id = set.id.toString(),
                    workoutId = workoutId,
                    exerciseId = set.exerciseId.toString(),
                    setNumber = 0,
                    weight = set.weightKg,
                    reps = set.reps,
                    rpe = set.rpe,
                    isWarmup = set.isWarmup,
                    completedAt = set.completedAt.toEpochMilli(),
                )
                workoutDao.insertSet(entity)
            }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to add set to workout $workoutId: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun removeSet(setId: String) {
        val result = retryWithBackoff {
            runCatchingAsResult { workoutDao.deleteSet(setId) }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to remove set $setId: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun completeWorkout(workoutId: String, durationSeconds: Long, notes: String) {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val existing = workoutDao.getWorkoutWithSets(workoutId)
                if (existing != null) {
                    workoutDao.updateWorkout(
                        existing.workout.copy(
                            completedAt = System.currentTimeMillis(),
                            durationSeconds = durationSeconds,
                            notes = notes,
                            completed = true,
                        ),
                    )
                }
            }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to complete workout $workoutId: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        val result = retryWithBackoff {
            runCatchingAsResult { workoutDao.getWorkoutWithSets(workoutId) }
        }
        return when (result) {
            is AppResult.Success -> result.data?.toDomain()
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get workout $workoutId: ${result.error.message}")
                null
            }
        }
    }

    override fun observeWorkout(workoutId: String): Flow<Workout?> {
        return workoutDao.observeWorkoutWithSets(workoutId)
            .map { it?.toDomain() }
            .catch { e ->
                Log.e(TAG, "Error observing workout $workoutId", e)
                emit(null)
            }
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workoutDao.getRecentWorkouts(limit)
            .map { list -> list.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Error getting recent workouts", e)
                emit(emptyList())
            }
    }

    override suspend fun getBestWeight(exerciseId: String, reps: Int): Double? {
        val result = retryWithBackoff {
            runCatchingAsResult { workoutDao.getBestWeight(exerciseId, reps) }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get best weight for exercise $exerciseId: ${result.error.message}")
                null
            }
        }
    }

    override suspend fun getDaysSinceLastWorkout(): Int? {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val lastTimestamp = workoutDao.getLastCompletedTimestamp() ?: return@runCatchingAsResult null
                val lastInstant = Instant.ofEpochMilli(lastTimestamp)
                val now = Instant.now()
                java.time.Duration.between(lastInstant, now).toDays().toInt()
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get days since last workout: ${result.error.message}")
                null
            }
        }
    }

    companion object {
        private const val TAG = "WorkoutRepositoryImpl"
    }
}

private fun WorkoutWithSets.toDomain(): Workout {
    val domainSets = sets.map { set ->
        ExerciseSet(
            id = UUID.fromString(set.id),
            exerciseId = UUID.fromString(set.exerciseId),
            weightKg = set.weight,
            reps = set.reps,
            rpe = set.rpe,
            isWarmup = set.isWarmup,
            completedAt = Instant.ofEpochMilli(set.completedAt),
        )
    }
    return Workout(
        id = UUID.fromString(workout.id),
        name = "",
        startedAt = Instant.ofEpochMilli(workout.startedAt),
        completedAt = workout.completedAt?.let { Instant.ofEpochMilli(it) },
        sets = domainSets,
        notes = workout.notes,
    )
}
