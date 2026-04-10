package com.gymbro.feature.progress

import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.PlateauAlert
import com.gymbro.core.model.WorkoutHistoryItem

data class ProgressState(
    val workoutHistory: List<WorkoutHistoryItem> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val exerciseOptions: List<ExerciseOption> = emptyList(),
    val selectedExerciseId: String? = null,
    val chartData: List<E1RMDataPoint> = emptyList(),
    val plateauAlerts: List<PlateauAlert> = emptyList(),
    val isLoading: Boolean = true,
    val totalVolume: Double = 0.0,
    val workoutsThisWeek: Int = 0,
    val recentPRs: Int = 0,
    val weeklyVolumeData: List<WeeklyVolume> = emptyList(),
)

data class WeeklyVolume(
    val weekNumber: Int,
    val volume: Double,
)

data class ExerciseOption(
    val id: String,
    val name: String,
)

sealed interface ProgressEvent {
    data class SelectExercise(val exerciseId: String) : ProgressEvent
    data object RefreshData : ProgressEvent
    data class ViewWorkoutDetail(val workoutId: String) : ProgressEvent
    data class DismissPlateauAlert(val exerciseId: String) : ProgressEvent
    data class GetCoachingAdvice(val exerciseName: String, val weeksDuration: Int) : ProgressEvent
}

sealed interface ProgressEffect {
    data class NavigateToWorkoutDetail(val workoutId: String) : ProgressEffect
    data class NavigateToCoach(val prompt: String) : ProgressEffect
}
