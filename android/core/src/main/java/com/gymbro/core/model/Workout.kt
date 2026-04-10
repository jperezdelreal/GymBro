package com.gymbro.core.model

import java.time.Instant
import java.util.UUID

data class Workout(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val sets: List<ExerciseSet> = emptyList(),
    val notes: String = "",
)

data class ExerciseSet(
    val id: UUID = UUID.randomUUID(),
    val exerciseId: UUID,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val rir: Int? = null,
    val isWarmup: Boolean = false,
    val completedAt: Instant = Instant.now(),
) {
    /** Epley formula: weight × (1 + reps/30) */
    val estimatedOneRepMax: Double
        get() = if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)

    /** Convert stored kg to lbs for display */
    val weightLbs: Double
        get() = weightKg * 2.20462

    companion object {
        /** Convert RPE (1-10) to RIR (Reps in Reserve). RPE 10 = 0 RIR, RPE 7 = 3 RIR */
        fun rpeToRir(rpe: Double): Int = (10 - rpe).coerceIn(0.0, 5.0).toInt()

        /** Convert RIR (0-5) to RPE (1-10). 0 RIR = RPE 10, 3 RIR = RPE 7 */
        fun rirToRpe(rir: Int): Double = (10.0 - rir).coerceIn(5.0, 10.0)
    }
}
