package com.gymbro.core.model

data class RecoveryMetrics(
    val sleepHours: Double = 0.0,
    val restingHR: Double? = null,
    val hrv: Double? = null,
    val steps: Long = 0,
    val daysSinceLastWorkout: Int? = null,
    val readinessScore: Int = 0,
) {
    val readinessLabel: String
        get() = when {
            readinessScore >= 80 -> "Ready to Train"
            readinessScore >= 60 -> "Moderate"
            readinessScore >= 40 -> "Light Day"
            else -> "Rest Recommended"
        }

    companion object {
        /**
         * Calculate readiness score (0–100) as weighted combo of:
         * - Sleep quality (40%): 8h = 100, linear down to 0 at 4h
         * - HRV (30%): higher is better, normalized to personal baseline
         * - Rest days (30%): 1 rest day = 80, 2+ = 100, 0 = 50
         */
        fun calculateReadiness(
            sleepHours: Double,
            hrv: Double?,
            daysSinceLastWorkout: Int?,
        ): Int {
            val sleepScore = ((sleepHours - 4.0) / 4.0 * 100.0).coerceIn(0.0, 100.0)

            // HRV: use 50ms as "average" baseline — above is better
            val hrvScore = if (hrv != null) {
                ((hrv - 20.0) / 60.0 * 100.0).coerceIn(0.0, 100.0)
            } else {
                50.0 // neutral when unavailable
            }

            val restScore = when (daysSinceLastWorkout) {
                null -> 60.0
                0 -> 50.0
                1 -> 80.0
                else -> 100.0
            }

            return (sleepScore * 0.4 + hrvScore * 0.3 + restScore * 0.3).toInt().coerceIn(0, 100)
        }
    }
}
