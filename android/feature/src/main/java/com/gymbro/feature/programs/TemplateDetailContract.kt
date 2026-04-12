package com.gymbro.feature.programs

import com.gymbro.core.model.WorkoutTemplateLibrary

sealed interface TemplateDetailEvent {
    object StartProgram : TemplateDetailEvent
    object BackClicked : TemplateDetailEvent
}

data class TemplateDetailState(
    val template: WorkoutTemplateLibrary.CuratedTemplate,
    val isStarting: Boolean = false
)

sealed interface TemplateDetailEffect {
    object NavigateBack : TemplateDetailEffect
    data class ProgramStarted(val template: WorkoutTemplateLibrary.CuratedTemplate) : TemplateDetailEffect
}
