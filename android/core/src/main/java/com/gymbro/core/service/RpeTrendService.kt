package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.WorkoutSetEntity
import javax.inject.Inject

/**
 * Tracks RPE trends per exercise using a 7-day rolling average.
 * Flags exercises where RPE is trending upward (fatigue signal).
 */
class RpeTrendService @Inject constructor(
    private val workoutDao: WorkoutDao,
) {

    data class RpeTrend(
        val exerciseId: String,
        val currentAvgRpe: Double,
        val previousAvgRpe: Double?,
        val trend: TrendDirection,
        val isFatigueWarning: Boolean,
    )

    enum class TrendDirection {
        RISING,
        STABLE,
        FALLING,
    }

    /**
     * Calculate RPE trend for a specific exercise.
     * Compares the most recent 7-day window to the prior 7-day window.
     */
    suspend fun getTrend(exerciseId: String): RpeTrend? {
        val sets = workoutDao.getSetsByExercise(exerciseId)
        val setsWithRpe = sets.filter { it.rpe != null && !it.isWarmup }

        if (setsWithRpe.isEmpty()) return null

        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val fourteenDaysMs = 14L * 24 * 60 * 60 * 1000

        val recentSets = setsWithRpe.filter { it.completedAt >= now - sevenDaysMs }
        val previousSets = setsWithRpe.filter {
            it.completedAt >= now - fourteenDaysMs && it.completedAt < now - sevenDaysMs
        }

        if (recentSets.isEmpty()) {
            // No recent data — use all available data as current
            val avgRpe = setsWithRpe.mapNotNull { it.rpe }.average()
            return RpeTrend(
                exerciseId = exerciseId,
                currentAvgRpe = avgRpe,
                previousAvgRpe = null,
                trend = TrendDirection.STABLE,
                isFatigueWarning = false,
            )
        }

        val currentAvg = recentSets.mapNotNull { it.rpe }.average()

        if (previousSets.isEmpty()) {
            return RpeTrend(
                exerciseId = exerciseId,
                currentAvgRpe = currentAvg,
                previousAvgRpe = null,
                trend = TrendDirection.STABLE,
                isFatigueWarning = currentAvg >= 9.0,
            )
        }

        val previousAvg = previousSets.mapNotNull { it.rpe }.average()
        val delta = currentAvg - previousAvg

        val trend = when {
            delta > 0.5 -> TrendDirection.RISING
            delta < -0.5 -> TrendDirection.FALLING
            else -> TrendDirection.STABLE
        }

        // Fatigue warning: RPE trending up AND current average ≥ 8.5
        val isFatigueWarning = trend == TrendDirection.RISING && currentAvg >= 8.5

        return RpeTrend(
            exerciseId = exerciseId,
            currentAvgRpe = currentAvg,
            previousAvgRpe = previousAvg,
            trend = trend,
            isFatigueWarning = isFatigueWarning,
        )
    }

    /**
     * Get fatigue warnings for all exercises with history.
     * Returns only exercises flagged as fatiguing.
     */
    suspend fun getFatigueWarnings(): List<RpeTrend> {
        val exerciseIds = workoutDao.getExerciseIdsWithHistory()
        return exerciseIds.mapNotNull { getTrend(it) }.filter { it.isFatigueWarning }
    }
}
