package com.gymbro.core.model

import java.time.Instant

data class SleepData(
    val durationMinutes: Double,
    val quality: SleepQuality,
    val startTime: Instant,
    val endTime: Instant,
    val deepSleepMinutes: Double = 0.0,
    val remSleepMinutes: Double = 0.0,
    val lightSleepMinutes: Double = 0.0,
    val awakeMinutes: Double = 0.0,
) {
    val durationHours: Double get() = durationMinutes / 60.0
}

enum class SleepQuality {
    POOR,
    FAIR,
    GOOD,
    EXCELLENT;

    companion object {
        fun fromDurationHours(hours: Double): SleepQuality = when {
            hours >= 8.0 -> EXCELLENT
            hours >= 7.0 -> GOOD
            hours >= 6.0 -> FAIR
            else -> POOR
        }
    }
}
