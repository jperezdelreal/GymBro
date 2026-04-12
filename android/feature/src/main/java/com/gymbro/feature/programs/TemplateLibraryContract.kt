package com.gymbro.feature.programs

import com.gymbro.core.model.WorkoutTemplateLibrary

sealed interface TemplateLibraryEvent {
    data class FilterChanged(val audience: WorkoutTemplateLibrary.TargetAudience?) : TemplateLibraryEvent
    data class TemplateClicked(val template: WorkoutTemplateLibrary.CuratedTemplate) : TemplateLibraryEvent
}

data class TemplateLibraryState(
    val templates: List<WorkoutTemplateLibrary.CuratedTemplate> = WorkoutTemplateLibrary.templates,
    val selectedFilter: WorkoutTemplateLibrary.TargetAudience? = null,
    val filteredTemplates: List<WorkoutTemplateLibrary.CuratedTemplate> = templates
)

sealed interface TemplateLibraryEffect {
    data class NavigateToTemplateDetail(val template: WorkoutTemplateLibrary.CuratedTemplate) : TemplateLibraryEffect
}
