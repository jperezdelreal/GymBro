package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.WorkoutSetEntity
import javax.inject.Inject

/**
 * Performance-based progression engine using RPE/RIR data.
 *
 * Rules:
 * - Auto-progression: all working sets completed at RPE ≤7 → suggest +2.5kg next session
 * - Auto-regression: last 2 working sets at RPE 10 or incomplete → suggest -5% weight
 * - No RPE data: fall back to last weight (no suggestion)
 */
class ProgressionEngine @Inject constructor(
    private val workoutDao: WorkoutDao,
) {

    data class ProgressionSuggestion(
        val suggestedWeightKg: Double,
        val reason: ProgressionReason,
    )

    enum class ProgressionReason {
        /** All sets were easy (RPE ≤ 7) — increase weight */
        PROGRESS,
        /** Last sets were maximal effort or failed — reduce weight */
        REGRESS,
        /** Maintain current weight — RPE in moderate range */
        MAINTAIN,
        /** No RPE data available — use last weight */
        NO_DATA,
    }

    /**
     * Calculate suggested weight for the next session of a given exercise.
     * Analyzes the most recent workout's working sets for that exercise.
     */
    suspend fun getSuggestion(exerciseId: String): ProgressionSuggestion? {
        val sets = workoutDao.getSetsByExercise(exerciseId)
        if (sets.isEmpty()) return null

        // Get the most recent workout's sets (group by workoutId, take latest)
        val lastWorkoutId = sets.last().workoutId
        val lastWorkoutSets = sets.filter { it.workoutId == lastWorkoutId && !it.isWarmup }

        if (lastWorkoutSets.isEmpty()) return null

        val lastWeight = lastWorkoutSets.maxOf { it.weight }
        val setsWithRpe = lastWorkoutSets.filter { it.rpe != null }

        // No RPE data — can't make a recommendation
        if (setsWithRpe.isEmpty()) {
            return ProgressionSuggestion(
                suggestedWeightKg = lastWeight,
                reason = ProgressionReason.NO_DATA,
            )
        }

        // Check regression: last 2 sets at RPE 10
        val shouldRegress = checkRegression(lastWorkoutSets)
        if (shouldRegress) {
            val regressedWeight = roundToNearest2_5(lastWeight * 0.95)
            return ProgressionSuggestion(
                suggestedWeightKg = regressedWeight,
                reason = ProgressionReason.REGRESS,
            )
        }

        // Check progression: all working sets RPE ≤ 7
        val shouldProgress = setsWithRpe.all { (it.rpe ?: 10.0) <= 7.0 }
        if (shouldProgress) {
            return ProgressionSuggestion(
                suggestedWeightKg = lastWeight + 2.5,
                reason = ProgressionReason.PROGRESS,
            )
        }

        // Moderate RPE (8-9) — maintain current weight
        return ProgressionSuggestion(
            suggestedWeightKg = lastWeight,
            reason = ProgressionReason.MAINTAIN,
        )
    }

    private fun checkRegression(workingSets: List<WorkoutSetEntity>): Boolean {
        if (workingSets.size < 2) return false
        val lastTwo = workingSets.takeLast(2)
        return lastTwo.all { set ->
            val rpe = set.rpe ?: return@all false
            rpe >= 10.0
        }
    }

    private fun roundToNearest2_5(value: Double): Double {
        return Math.round(value / 2.5) * 2.5
    }
}
