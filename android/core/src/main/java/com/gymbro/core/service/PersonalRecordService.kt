package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.RecordType
import com.gymbro.core.model.WorkoutHistoryItem
import java.time.Instant
import javax.inject.Inject

class PersonalRecordService @Inject constructor(
    private val workoutDao: WorkoutDao,
) {

    /** Brzycki formula: weight * (36 / (37 - reps)) */
    fun calculateE1RM(weight: Double, reps: Int): Double {
        if (weight <= 0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        val denominator = 37.0 - reps
        if (denominator <= 0) return weight * 2.0
        return weight * (36.0 / denominator)
    }

    suspend fun getPersonalRecords(exerciseId: String, exerciseName: String): List<PersonalRecord> {
        val sets = workoutDao.getSetsByExercise(exerciseId)
        if (sets.isEmpty()) return emptyList()

        val records = mutableListOf<PersonalRecord>()

        val maxWeightSet = sets.maxByOrNull { it.weight }
        if (maxWeightSet != null) {
            val previousBest = sets
                .filter { it.completedAt < maxWeightSet.completedAt }
                .maxByOrNull { it.weight }
            records += PersonalRecord(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                type = RecordType.MAX_WEIGHT,
                value = maxWeightSet.weight,
                date = Instant.ofEpochMilli(maxWeightSet.completedAt),
                previousValue = previousBest?.weight,
            )
        }

        val maxRepsSet = sets.maxByOrNull { it.reps }
        if (maxRepsSet != null) {
            val previousBest = sets
                .filter { it.completedAt < maxRepsSet.completedAt }
                .maxByOrNull { it.reps }
            records += PersonalRecord(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                type = RecordType.MAX_REPS,
                value = maxRepsSet.reps.toDouble(),
                date = Instant.ofEpochMilli(maxRepsSet.completedAt),
                previousValue = previousBest?.reps?.toDouble(),
            )
        }

        val maxVolumeSet = sets.maxByOrNull { it.weight * it.reps }
        if (maxVolumeSet != null) {
            val volume = maxVolumeSet.weight * maxVolumeSet.reps
            val previousBest = sets
                .filter { it.completedAt < maxVolumeSet.completedAt }
                .maxByOrNull { it.weight * it.reps }
            records += PersonalRecord(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                type = RecordType.MAX_VOLUME,
                value = volume,
                date = Instant.ofEpochMilli(maxVolumeSet.completedAt),
                previousValue = previousBest?.let { it.weight * it.reps },
            )
        }

        val maxE1RMSet = sets.maxByOrNull { calculateE1RM(it.weight, it.reps) }
        if (maxE1RMSet != null) {
            val e1rm = calculateE1RM(maxE1RMSet.weight, maxE1RMSet.reps)
            val previousBest = sets
                .filter { it.completedAt < maxE1RMSet.completedAt }
                .maxByOrNull { calculateE1RM(it.weight, it.reps) }
            records += PersonalRecord(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                type = RecordType.MAX_E1RM,
                value = e1rm,
                date = Instant.ofEpochMilli(maxE1RMSet.completedAt),
                previousValue = previousBest?.let { calculateE1RM(it.weight, it.reps) },
            )
        }

        return records
    }

    suspend fun getE1RMHistory(exerciseId: String): List<E1RMDataPoint> {
        val sets = workoutDao.getSetsByExercise(exerciseId)
        if (sets.isEmpty()) return emptyList()

        return sets
            .groupBy { Instant.ofEpochMilli(it.completedAt).epochSecond / 86400 }
            .mapNotNull { (_, daySets) ->
                val best = daySets.maxByOrNull { calculateE1RM(it.weight, it.reps) } ?: return@mapNotNull null
                E1RMDataPoint(
                    date = Instant.ofEpochMilli(best.completedAt),
                    e1rm = calculateE1RM(best.weight, best.reps),
                    weight = best.weight,
                    reps = best.reps,
                )
            }
            .sortedBy { it.date }
    }

    suspend fun getWorkoutHistory(): List<WorkoutHistoryItem> {
        val workouts = workoutDao.getAllCompletedWorkouts()
        return workouts.map { wws ->
            val workingSets = wws.sets.filter { !it.isWarmup }
            val exerciseIds = workingSets.map { it.exerciseId }.distinct()
            WorkoutHistoryItem(
                workoutId = wws.workout.id,
                date = Instant.ofEpochMilli(wws.workout.startedAt),
                exerciseCount = exerciseIds.size,
                totalVolume = workingSets.sumOf { it.weight * it.reps },
                durationSeconds = wws.workout.durationSeconds,
                exerciseNames = emptyList(),
            )
        }
    }

    suspend fun getExerciseIdsWithHistory(): List<String> {
        return workoutDao.getExerciseIdsWithHistory()
    }
}
