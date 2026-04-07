package com.gymbro.core.model

import java.util.UUID

data class Exercise(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val muscleGroup: MuscleGroup,
    val category: ExerciseCategory = ExerciseCategory.COMPOUND,
    val equipment: Equipment = Equipment.BARBELL,
    val description: String = "",
    val youtubeUrl: String? = null,
)

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    QUADRICEPS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    FOREARMS("Forearms"),
    FULL_BODY("Full Body"),
}

enum class ExerciseCategory(val displayName: String) {
    COMPOUND("Compound"),
    ISOLATION("Isolation"),
    ACCESSORY("Accessory"),
    CARDIO("Cardio"),
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
