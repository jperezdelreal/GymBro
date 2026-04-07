package com.gymbro.core.model

import java.time.Instant

data class E1RMDataPoint(
    val date: Instant,
    val e1rm: Double,
    val weight: Double,
    val reps: Int,
)
