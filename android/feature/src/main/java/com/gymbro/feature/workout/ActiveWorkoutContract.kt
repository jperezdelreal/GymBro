package com.gymbro.feature.workout

import com.gymbro.core.model.Exercise
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.service.ProgressionEngine

data class ActiveWorkoutState(
    val workoutId: String? = null,
    val exercises: List<WorkoutExerciseUi> = emptyList(),
    val elapsedSeconds: Long = 0,
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val restTimerSeconds: Int = 0,
    val restTimerTotal: Int = 90,
    val isRestTimerActive: Boolean = false,
    val isCompleting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hasInProgressWorkout: Boolean = false,
    val fatigueWarnings: List<FatigueWarningUi> = emptyList(),
)

data class FatigueWarningUi(
    val exerciseId: String,
    val exerciseName: String,
    val currentAvgRpe: Double,
    val rpeDelta: Double?,
)

data class WorkoutExerciseUi(
    val exercise: Exercise,
    val sets: List<WorkoutSetUi> = emptyList(),
    val progressionSuggestion: ProgressionSuggestionUi? = null,
)

data class ProgressionSuggestionUi(
    val lastWeight: Double,
    val lastReps: Int,
    val lastRpe: Double?,
    val suggestedWeight: Double,
    val reason: ProgressionEngine.ProgressionReason,
)

data class WorkoutSetUi(
    val id: String,
    val setNumber: Int,
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
    val isWarmup: Boolean = false,
    val isCompleted: Boolean = false,
)

sealed interface ActiveWorkoutEvent {
    data object AddExerciseClicked : ActiveWorkoutEvent
    data class ExercisePicked(val exercise: Exercise) : ActiveWorkoutEvent
    data class AddSet(val exerciseIndex: Int) : ActiveWorkoutEvent
    data class UpdateSetWeight(val exerciseIndex: Int, val setIndex: Int, val weight: String) : ActiveWorkoutEvent
    data class UpdateSetReps(val exerciseIndex: Int, val setIndex: Int, val reps: String) : ActiveWorkoutEvent
    data class UpdateSetRpe(val exerciseIndex: Int, val setIndex: Int, val rpe: String) : ActiveWorkoutEvent
    data class ToggleWarmup(val exerciseIndex: Int, val setIndex: Int) : ActiveWorkoutEvent
    data class CompleteSet(val exerciseIndex: Int, val setIndex: Int) : ActiveWorkoutEvent
    data class QuickCompleteSet(val exerciseIndex: Int, val setIndex: Int) : ActiveWorkoutEvent
    data class RemoveSet(val exerciseIndex: Int, val setIndex: Int) : ActiveWorkoutEvent
    data class RemoveExercise(val exerciseIndex: Int) : ActiveWorkoutEvent
    data class VoiceInput(val exerciseIndex: Int, val setIndex: Int, val weight: String, val reps: String, val rpe: String = "") : ActiveWorkoutEvent
    data object StartRestTimer : ActiveWorkoutEvent
    data object SkipRestTimer : ActiveWorkoutEvent
    data class AdjustRestTimer(val deltaSeconds: Int) : ActiveWorkoutEvent
    data object CompleteWorkout : ActiveWorkoutEvent
    data object DiscardWorkout : ActiveWorkoutEvent
    data object ClearError : ActiveWorkoutEvent
    data object DismissFatigueWarnings : ActiveWorkoutEvent
    data object RetryStartWorkout : ActiveWorkoutEvent
    data object ResumeWorkout : ActiveWorkoutEvent
    data object StartNewWorkout : ActiveWorkoutEvent
}

sealed interface ActiveWorkoutEffect {
    data object ShowExercisePicker : ActiveWorkoutEffect
    data class NavigateToSummary(
        val durationSeconds: Long,
        val totalVolume: Double,
        val totalSets: Int,
        val exerciseCount: Int,
        val personalRecords: List<PersonalRecord>,
    ) : ActiveWorkoutEffect
    data object NavigateBack : ActiveWorkoutEffect
    data object RestTimerFinished : ActiveWorkoutEffect
}
