package com.gymbro.core.model

import java.time.Instant

data class WorkoutHistoryItem(
    val workoutId: String,
    val date: Instant,
    val exerciseCount: Int,
    val totalVolume: Double,
    val durationSeconds: Long,
    val exerciseNames: List<String>,
    val prCount: Int = 0,
)
