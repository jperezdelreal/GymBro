package com.gymbro.core.model

enum class PlateauType {
    STAGNATION,
    REGRESSION,
}

enum class PlateauSeverity {
    MILD,
    MODERATE,
    SEVERE,
}

data class PlateauAlert(
    val exerciseId: String,
    val exerciseName: String,
    val type: PlateauType,
    val weeksDuration: Int,
    val suggestion: String,
    val severity: PlateauSeverity,
    val daysSinceLastPR: Int,
)
