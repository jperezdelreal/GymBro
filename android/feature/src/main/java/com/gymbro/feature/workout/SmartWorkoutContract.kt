package com.gymbro.feature.workout

import com.gymbro.core.model.Exercise
import com.gymbro.core.model.MuscleGroup

data class SmartWorkoutState(
    val isLoading: Boolean = true,
    val exercises: List<SuggestedExerciseUi> = emptyList(),
    val targetMuscleGroups: List<MuscleGroup> = emptyList(),
    val reasoning: String = "",
    val recoveryScore: Int = 0,
    val isGenerating: Boolean = false,
    val error: String? = null,
)

data class SuggestedExerciseUi(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: String,
    val suggestedWeight: String?,
    val progressionHint: String?,
)

sealed interface SmartWorkoutEvent {
    data object RegenerateWorkout : SmartWorkoutEvent
    data object StartWorkout : SmartWorkoutEvent
    data object NavigateBack : SmartWorkoutEvent
}

sealed interface SmartWorkoutEffect {
    data class StartWorkoutWithExercises(val exercises: List<Exercise>) : SmartWorkoutEffect
    data object NavigateBack : SmartWorkoutEffect
}
