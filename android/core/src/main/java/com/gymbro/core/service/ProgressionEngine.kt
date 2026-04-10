package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.preferences.UserPreferences
import javax.inject.Inject

/**
 * Performance-based progression engine using RPE/RIR data.
 *
 * Thresholds are phase-aware:
 * - BULK: aggressive — RPE ≤8 → progress, last 2 sets RPE 10 → regress
 * - CUT: conservative — RPE ≤6 → progress, last 2 sets RPE ≥9 → regress
 * - MAINTENANCE: balanced — RPE ≤7 → progress, last 2 sets RPE 10 → regress
 *
 * No RPE data: fall back to last weight (no suggestion).
 */
class ProgressionEngine @Inject constructor(
    private val workoutDao: WorkoutDao,
) {

    data class ProgressionSuggestion(
        val suggestedWeightKg: Double,
        val reason: ProgressionReason,
    )

    enum class ProgressionReason {
        /** All sets were easy — increase weight */
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
     *
     * @param trainingPhase determines how aggressively thresholds are applied.
     */
    suspend fun getSuggestion(
        exerciseId: String,
        trainingPhase: UserPreferences.TrainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
    ): ProgressionSuggestion? {
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

        // Phase-aware thresholds
        val progressionCeiling = when (trainingPhase) {
            UserPreferences.TrainingPhase.BULK -> 8.0
            UserPreferences.TrainingPhase.CUT -> 6.0
            UserPreferences.TrainingPhase.MAINTENANCE -> 7.0
        }
        val regressionFloor = when (trainingPhase) {
            UserPreferences.TrainingPhase.BULK -> 10.0
            UserPreferences.TrainingPhase.CUT -> 9.0
            UserPreferences.TrainingPhase.MAINTENANCE -> 10.0
        }

        // Check regression: last 2 sets at or above regression floor
        val shouldRegress = checkRegression(lastWorkoutSets, regressionFloor)
        if (shouldRegress) {
            val regressedWeight = roundToNearest2_5(lastWeight * 0.95)
            return ProgressionSuggestion(
                suggestedWeightKg = regressedWeight,
                reason = ProgressionReason.REGRESS,
            )
        }

        // Check progression: all working sets at or below progression ceiling
        val shouldProgress = setsWithRpe.all { (it.rpe ?: 10.0) <= progressionCeiling }
        if (shouldProgress) {
            return ProgressionSuggestion(
                suggestedWeightKg = lastWeight + 2.5,
                reason = ProgressionReason.PROGRESS,
            )
        }

        // Moderate RPE — maintain current weight
        return ProgressionSuggestion(
            suggestedWeightKg = lastWeight,
            reason = ProgressionReason.MAINTAIN,
        )
    }

    private fun checkRegression(
        workingSets: List<WorkoutSetEntity>,
        regressionFloor: Double,
    ): Boolean {
        if (workingSets.size < 2) return false
        val lastTwo = workingSets.takeLast(2)
        return lastTwo.all { set ->
            val rpe = set.rpe ?: return@all false
            rpe >= regressionFloor
        }
    }

    private fun roundToNearest2_5(value: Double): Double {
        return Math.round(value / 2.5) * 2.5
    }
}
