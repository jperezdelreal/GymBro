package com.gymbro.core.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.InProgressWorkoutEntity
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.InProgressExercise
import com.gymbro.core.model.InProgressSet
import com.gymbro.core.model.InProgressWorkout
import com.gymbro.core.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val gson: Gson,
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
                    rir = set.rir,
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

    override suspend fun saveInProgressWorkout(inProgressWorkout: InProgressWorkout) {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val exercisesJson = gson.toJson(inProgressWorkout.exercises.map { exercise ->
                    mapOf(
                        "exerciseId" to exercise.exercise.id.toString(),
                        "exerciseName" to exercise.exercise.name,
                        "muscleGroup" to exercise.exercise.muscleGroup.name,
                        "sets" to exercise.sets.map { set ->
                            mapOf(
                                "id" to set.id,
                                "setNumber" to set.setNumber,
                                "weight" to set.weight,
                                "reps" to set.reps,
                                "rpe" to set.rpe,
                                "isWarmup" to set.isWarmup,
                                "isCompleted" to set.isCompleted,
                            )
                        }
                    )
                })
                
                val entity = InProgressWorkoutEntity(
                    workoutId = inProgressWorkout.workoutId,
                    exercisesJson = exercisesJson,
                    elapsedSeconds = inProgressWorkout.elapsedSeconds,
                    totalVolume = inProgressWorkout.totalVolume,
                    totalSets = inProgressWorkout.totalSets,
                    restTimerSeconds = inProgressWorkout.restTimerSeconds,
                    restTimerTotal = inProgressWorkout.restTimerTotal,
                    isRestTimerActive = inProgressWorkout.isRestTimerActive,
                )
                workoutDao.saveInProgressWorkout(entity)
            }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to save in-progress workout: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun getInProgressWorkout(): InProgressWorkout? {
        val result = retryWithBackoff {
            runCatchingAsResult {
                workoutDao.getAnyInProgressWorkout()?.let { entity ->
                    try {
                        val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val exercisesList: List<Map<String, Any>> = gson.fromJson(entity.exercisesJson, listType)
                        
                        val exercises = exercisesList.map { exerciseMap ->
                            val exerciseId = UUID.fromString(exerciseMap["exerciseId"] as String)
                            val exerciseName = exerciseMap["exerciseName"] as String
                            val muscleGroupStr = exerciseMap["muscleGroup"] as String
                            
                            @Suppress("UNCHECKED_CAST")
                            val setsArray = exerciseMap["sets"] as List<Map<String, Any>>
                            
                            InProgressExercise(
                                exercise = com.gymbro.core.model.Exercise(
                                    id = exerciseId,
                                    name = exerciseName,
                                    muscleGroup = try {
                                        com.gymbro.core.model.MuscleGroup.valueOf(muscleGroupStr)
                                    } catch (e: Exception) {
                                        com.gymbro.core.model.MuscleGroup.FULL_BODY
                                    },
                                    category = com.gymbro.core.model.ExerciseCategory.COMPOUND,
                                    equipment = com.gymbro.core.model.Equipment.OTHER,
                                    description = "",
                                ),
                                sets = setsArray.map { setMap ->
                                    InProgressSet(
                                        id = setMap["id"] as String,
                                        setNumber = (setMap["setNumber"] as Double).toInt(),
                                        weight = setMap["weight"] as String,
                                        reps = setMap["reps"] as String,
                                        rpe = setMap["rpe"] as String,
                                        isWarmup = setMap["isWarmup"] as Boolean,
                                        isCompleted = setMap["isCompleted"] as Boolean,
                                    )
                                }
                            )
                        }
                        
                        InProgressWorkout(
                            workoutId = entity.workoutId,
                            exercises = exercises,
                            elapsedSeconds = entity.elapsedSeconds,
                            totalVolume = entity.totalVolume,
                            totalSets = entity.totalSets,
                            restTimerSeconds = entity.restTimerSeconds,
                            restTimerTotal = entity.restTimerTotal,
                            isRestTimerActive = entity.isRestTimerActive,
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to deserialize in-progress workout", e)
                        workoutDao.clearInProgressWorkout(entity.workoutId)
                        null
                    }
                }
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get in-progress workout: ${result.error.message}")
                null
            }
        }
    }

    override suspend fun clearInProgressWorkout(workoutId: String) {
        val result = retryWithBackoff {
            runCatchingAsResult { workoutDao.clearInProgressWorkout(workoutId) }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to clear in-progress workout $workoutId: ${result.error.message}")
            }
        }
    }

    override suspend fun getTotalCompletedWorkoutsCount(): Int {
        val result = retryWithBackoff {
            runCatchingAsResult { workoutDao.getTotalCompletedWorkoutsCount() }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get total workouts count: ${result.error.message}")
                0
            }
        }
    }

    override suspend fun getActiveDaysCount(): Int {
        val result = retryWithBackoff {
            runCatchingAsResult { workoutDao.getActiveDaysCount() }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get active days count: ${result.error.message}")
                0
            }
        }
    }

    override suspend fun getCurrentStreak(): Int {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val allWorkouts = workoutDao.getAllCompletedWorkouts()
                if (allWorkouts.isEmpty()) return@runCatchingAsResult 0

                val workoutDates = allWorkouts.map { workout ->
                    val instant = Instant.ofEpochMilli(workout.workout.startedAt)
                    instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }.distinct().sortedDescending()

                if (workoutDates.isEmpty()) return@runCatchingAsResult 0

                val today = java.time.LocalDate.now()
                val yesterday = today.minusDays(1)
                
                var streak = 0
                var currentDate = if (workoutDates[0] == today) today else if (workoutDates[0] == yesterday) yesterday else return@runCatchingAsResult 0
                
                for (date in workoutDates) {
                    if (date == currentDate) {
                        streak++
                        currentDate = currentDate.minusDays(1)
                    } else if (date.isBefore(currentDate)) {
                        break
                    }
                }
                
                streak
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get current streak: ${result.error.message}")
                0
            }
        }
    }

    override suspend fun getExerciseHistory(exerciseId: String, limit: Int): List<ExerciseHistorySession> {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val setsWithDates = workoutDao.getExerciseHistorySets(exerciseId, limit * 5)
                
                // Group sets by workout date
                val groupedByWorkout = setsWithDates.groupBy { it.workoutDate }
                
                // Convert to ExerciseHistorySession and take only the requested limit of sessions
                groupedByWorkout.entries
                    .sortedByDescending { it.key }
                    .take(limit)
                    .map { (workoutDate, setsWithDate) ->
                        ExerciseHistorySession(
                            workoutDate = Instant.ofEpochMilli(workoutDate),
                            sets = setsWithDate.map { setWithDate ->
                                ExerciseSet(
                                    id = UUID.fromString(setWithDate.set.id),
                                    exerciseId = UUID.fromString(setWithDate.set.exerciseId),
                                    weightKg = setWithDate.set.weight,
                                    reps = setWithDate.set.reps,
                                    rpe = setWithDate.set.rpe,
                                    rir = setWithDate.set.rir,
                                    isWarmup = setWithDate.set.isWarmup,
                                    completedAt = Instant.ofEpochMilli(setWithDate.set.completedAt),
                                )
                            }
                        )
                    }
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get exercise history: ${result.error.message}")
                emptyList()
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
            rir = set.rir,
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
