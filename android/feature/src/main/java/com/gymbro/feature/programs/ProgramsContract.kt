package com.gymbro.feature.programs

import com.gymbro.core.error.UiError
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.model.WorkoutTemplate

data class ProgramsState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val activePlan: WorkoutPlan? = null,
    val isLoading: Boolean = true,
    val isGeneratingPlan: Boolean = false,
    val error: UiError? = null,
    val showCreateDialog: Boolean = false,
)

sealed interface ProgramsEvent {
    data class TemplateClicked(val template: WorkoutTemplate) : ProgramsEvent
    data object CreateTemplateClicked : ProgramsEvent
    data object CreateDialogDismissed : ProgramsEvent
    data class DeleteTemplate(val templateId: String) : ProgramsEvent
    data class StartWorkoutFromTemplate(val template: WorkoutTemplate) : ProgramsEvent
    data object GenerateNewPlan : ProgramsEvent
    data class ViewPlanDay(val dayNumber: Int) : ProgramsEvent
}

sealed interface ProgramsEffect {
    data class NavigateToCreateTemplate(val templateId: String?) : ProgramsEffect
    data class NavigateToActiveWorkout(val template: WorkoutTemplate) : ProgramsEffect
    data class NavigateToPlanDayDetail(val dayNumber: Int) : ProgramsEffect
}
