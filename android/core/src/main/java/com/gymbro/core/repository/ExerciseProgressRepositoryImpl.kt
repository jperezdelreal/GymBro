package com.gymbro.core.repository

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.model.ExerciseProgressPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ExerciseProgressRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : ExerciseProgressRepository {

    override fun getExerciseProgressData(exerciseId: String): Flow<List<ExerciseProgressPoint>> = flow {
        val allSets = workoutDao.getExerciseHistorySets(exerciseId, limit = 500)

        val grouped = allSets.groupBy { set ->
            Instant.ofEpochMilli(set.workoutDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        val points = grouped.map { (date, setsWithDate) ->
            val sets = setsWithDate.map { it.set }
            val maxWeightSet = sets.maxByOrNull { it.weight }
            val maxWeight = maxWeightSet?.weight ?: 0.0

            val bestE1rm = sets.maxOfOrNull { set ->
                estimateOneRepMax(set.weight, set.reps)
            } ?: 0.0

            val totalVolume = sets.sumOf { it.weight * it.reps }

            val bestSet = maxWeightSet?.let {
                "%.1f kg × %d".format(it.weight, it.reps)
            } ?: ""

            val rpeSets = sets.mapNotNull { it.rpe }
            val averageRpe = if (rpeSets.isNotEmpty()) {
                rpeSets.average()
            } else null

            ExerciseProgressPoint(
                date = date,
                maxWeight = maxWeight,
                estimatedOneRepMax = bestE1rm,
                totalVolume = totalVolume,
                bestSet = bestSet,
                averageRpe = averageRpe,
            )
        }.sortedBy { it.date }

        emit(points)
    }

    companion object {
        /** Brzycki formula: weight × (36 / (37 - reps)) */
        fun estimateOneRepMax(weight: Double, reps: Int): Double {
            if (reps <= 0) return 0.0
            if (reps == 1) return weight
            if (reps >= 37) return weight * 2.5
            return weight * (36.0 / (37.0 - reps))
        }
    }
}
