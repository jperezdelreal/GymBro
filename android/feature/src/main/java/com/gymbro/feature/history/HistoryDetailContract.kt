package com.gymbro.feature.history

import androidx.annotation.StringRes
import com.gymbro.core.model.MuscleGroup

data class HistoryDetailState(
    val isLoading: Boolean = true,
    @StringRes val errorRes: Int? = null,
    val workoutDetail: WorkoutDetail? = null,
    val editingSetId: String? = null,
    val editWeight: String = "",
    val editReps: String = "",
    val editRpe: String = "",
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
    val setId: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
)

sealed interface HistoryDetailIntent {
    data class LoadWorkout(val workoutId: String) : HistoryDetailIntent
    object Retry : HistoryDetailIntent
    data class StartEditingSet(val setId: String, val weight: Double, val reps: Int, val rpe: Double?) : HistoryDetailIntent
    object CancelEditing : HistoryDetailIntent
    data class UpdateWeight(val weight: String) : HistoryDetailIntent
    data class UpdateReps(val reps: String) : HistoryDetailIntent
    data class UpdateRpe(val rpe: String) : HistoryDetailIntent
    object SaveEdit : HistoryDetailIntent
}
