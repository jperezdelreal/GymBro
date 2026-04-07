package com.gymbro.feature.exerciselibrary

import com.gymbro.core.error.UiError
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.MuscleGroup

data class CreateExerciseState(
    val exerciseName: String = "",
    val selectedMuscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val selectedCategory: ExerciseCategory = ExerciseCategory.COMPOUND,
    val selectedEquipment: Equipment = Equipment.BARBELL,
    val description: String = "",
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val nameError: String? = null,
)

sealed interface CreateExerciseEvent {
    data class NameChanged(val name: String) : CreateExerciseEvent
    data class MuscleGroupSelected(val muscleGroup: MuscleGroup) : CreateExerciseEvent
    data class CategorySelected(val category: ExerciseCategory) : CreateExerciseEvent
    data class EquipmentSelected(val equipment: Equipment) : CreateExerciseEvent
    data class DescriptionChanged(val description: String) : CreateExerciseEvent
    data object SaveClicked : CreateExerciseEvent
    data object CancelClicked : CreateExerciseEvent
}

sealed interface CreateExerciseEffect {
    data object NavigateBack : CreateExerciseEffect
}
