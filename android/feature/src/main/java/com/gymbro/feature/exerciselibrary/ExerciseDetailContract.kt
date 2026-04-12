package com.gymbro.feature.exerciselibrary

import com.gymbro.core.model.Exercise

data class ExerciseDetailState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface ExerciseDetailEvent {
    data object RetryClicked : ExerciseDetailEvent
}

sealed interface ExerciseDetailEffect
