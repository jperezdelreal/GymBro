package com.gymbro.feature.programs

import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay

data class PlanDayDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workoutDay: WorkoutDay? = null,
    val planName: String = "",
    val isEditMode: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

sealed interface PlanDayDetailIntent {
    data class LoadDay(val dayNumber: Int) : PlanDayDetailIntent
    data object Retry : PlanDayDetailIntent
    data object StartWorkout : PlanDayDetailIntent
    data object ToggleEditMode : PlanDayDetailIntent
    data class RemoveExercise(val exerciseId: String) : PlanDayDetailIntent
    data class UpdateExercise(val exercise: PlannedExercise) : PlanDayDetailIntent
    data object SaveChanges : PlanDayDetailIntent
    data object DiscardChanges : PlanDayDetailIntent
    data object AddExercise : PlanDayDetailIntent
    data class ExerciseSelected(val exerciseName: String) : PlanDayDetailIntent
}

sealed interface PlanDayDetailEffect {
    data object NavigateToActiveWorkout : PlanDayDetailEffect
    data object NavigateToExercisePicker : PlanDayDetailEffect
    data object ShowSaveSuccess : PlanDayDetailEffect
}
