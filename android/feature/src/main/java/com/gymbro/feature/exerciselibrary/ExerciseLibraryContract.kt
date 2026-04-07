package com.gymbro.feature.exerciselibrary

import com.gymbro.core.model.Exercise
import com.gymbro.core.model.MuscleGroup

data class ExerciseLibraryState(
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val selectedMuscleGroup: MuscleGroup? = null,
    val isLoading: Boolean = true,
)

sealed interface ExerciseLibraryEvent {
    data class SearchQueryChanged(val query: String) : ExerciseLibraryEvent
    data class MuscleGroupSelected(val muscleGroup: MuscleGroup?) : ExerciseLibraryEvent
    data class ExerciseClicked(val exercise: Exercise) : ExerciseLibraryEvent
}

sealed interface ExerciseLibraryEffect {
    data class NavigateToDetail(val exerciseId: String) : ExerciseLibraryEffect
}
