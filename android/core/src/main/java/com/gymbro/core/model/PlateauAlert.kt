package com.gymbro.core.model

enum class PlateauType {
    STAGNATION,
    REGRESSION,
}

data class PlateauAlert(
    val exerciseId: String,
    val exerciseName: String,
    val type: PlateauType,
    val weeksDuration: Int,
    val suggestion: String,
)
