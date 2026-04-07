package com.gymbro.core.repository

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {

    override suspend fun startWorkout(): Workout {
        val now = Instant.now()
        val entity = WorkoutEntity(
            id = UUID.randomUUID().toString(),
            startedAt = now.toEpochMilli(),
        )
        workoutDao.insertWorkout(entity)
        return Workout(
            id = UUID.fromString(entity.id),
            name = "",
            startedAt = now,
        )
    }

    override suspend fun addSet(workoutId: String, set: ExerciseSet) {
        val entity = WorkoutSetEntity(
            id = set.id.toString(),
            workoutId = workoutId,
            exerciseId = set.exerciseId.toString(),
            setNumber = 0, // calculated by caller
            weight = set.weightKg,
            reps = set.reps,
            rpe = set.rpe,
            isWarmup = set.isWarmup,
            completedAt = set.completedAt.toEpochMilli(),
        )
        workoutDao.insertSet(entity)
    }

    override suspend fun removeSet(setId: String) {
        workoutDao.deleteSet(setId)
    }

    override suspend fun completeWorkout(workoutId: String, durationSeconds: Long, notes: String) {
        val existing = workoutDao.getWorkoutWithSets(workoutId) ?: return
        workoutDao.updateWorkout(
            existing.workout.copy(
                completedAt = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                notes = notes,
                completed = true,
            ),
        )
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        return workoutDao.getWorkoutWithSets(workoutId)?.toDomain()
    }

    override fun observeWorkout(workoutId: String): Flow<Workout?> {
        return workoutDao.observeWorkoutWithSets(workoutId).map { it?.toDomain() }
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workoutDao.getRecentWorkouts(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getBestWeight(exerciseId: String, reps: Int): Double? {
        return workoutDao.getBestWeight(exerciseId, reps)
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
