package com.gymbro.core.model

import java.util.UUID

data class Exercise(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment = Equipment.BARBELL,
    val instructions: String = "",
)

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    QUADRICEPS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    CORE,
    FOREARMS,
    FULL_BODY,
}

enum class Equipment {
    BARBELL,
    DUMBBELL,
    CABLE,
    MACHINE,
    BODYWEIGHT,
    KETTLEBELL,
    BAND,
    OTHER,
}
