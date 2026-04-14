package com.gymbro.core.model

import java.time.LocalDate

/**
 * A single data point for exercise progression visualization.
 * Aggregates the best performance from a single workout session for an exercise.
 */
data class ExerciseProgressPoint(
    val date: LocalDate,
    val maxWeight: Double,
    val estimatedOneRepMax: Double,
    val totalVolume: Double,
    val bestSet: String,
    val averageRpe: Double?,
)
