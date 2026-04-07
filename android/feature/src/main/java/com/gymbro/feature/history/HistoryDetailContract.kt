package com.gymbro.feature.history

import com.gymbro.core.model.MuscleGroup

data class HistoryDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workoutDetail: WorkoutDetail? = null,
)

data class WorkoutDetail(
    val workoutId: String,
    val date: Long,
    val durationSeconds: Long,
    val totalVolume: Double,
    val totalSets: Int,
    val totalReps: Int,
    val exercises: List<ExerciseDetail>,
    val prExerciseIds: Set<String>,
    val volumeByMuscleGroup: Map<MuscleGroup, Double>,
)

data class ExerciseDetail(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val sets: List<SetDetail>,
    val totalVolume: Double,
    val hasPR: Boolean,
)

data class SetDetail(
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
)

sealed interface HistoryDetailIntent {
    data class LoadWorkout(val workoutId: String) : HistoryDetailIntent
    object Retry : HistoryDetailIntent
}
