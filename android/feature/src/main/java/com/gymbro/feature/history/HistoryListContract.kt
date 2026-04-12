package com.gymbro.feature.history

import com.gymbro.core.model.MuscleGroup

data class HistoryListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workouts: List<WorkoutListItem> = emptyList(),
    val groupedWorkouts: List<WorkoutGroup> = emptyList(),
)

data class WorkoutListItem(
    val workoutId: String,
    val date: Long,
    val durationSeconds: Long,
    val exerciseCount: Int,
    val totalVolume: Double,
    val totalSets: Int,
    val muscleGroups: Set<MuscleGroup>,
    val prCount: Int,
    val volumeHistory: List<Double> = emptyList(),
)

data class WorkoutGroup(
    val monthYear: String,
    val workouts: List<WorkoutListItem>,
)

sealed interface HistoryListIntent {
    object LoadHistory : HistoryListIntent
    object Retry : HistoryListIntent
    data class WorkoutClicked(val workoutId: String) : HistoryListIntent
}
