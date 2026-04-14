package com.gymbro.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.R
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.PersonalRecordService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val personalRecordService: PersonalRecordService,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryDetailState())
    val state: StateFlow<HistoryDetailState> = _state.asStateFlow()

    fun onIntent(intent: HistoryDetailIntent) {
        when (intent) {
            is HistoryDetailIntent.LoadWorkout -> loadWorkout(intent.workoutId)
            is HistoryDetailIntent.Retry -> {
                state.value.workoutDetail?.workoutId?.let { loadWorkout(it) }
            }
            is HistoryDetailIntent.StartEditingSet -> {
                _state.value = _state.value.copy(
                    editingSetId = intent.setId,
                    editWeight = intent.weight.toString(),
                    editReps = intent.reps.toString(),
                    editRpe = intent.rpe?.toString() ?: ""
                )
            }
            is HistoryDetailIntent.CancelEditing -> {
                _state.value = _state.value.copy(
                    editingSetId = null,
                    editWeight = "",
                    editReps = "",
                    editRpe = ""
                )
            }
            is HistoryDetailIntent.UpdateWeight -> {
                _state.value = _state.value.copy(editWeight = intent.weight)
            }
            is HistoryDetailIntent.UpdateReps -> {
                _state.value = _state.value.copy(editReps = intent.reps)
            }
            is HistoryDetailIntent.UpdateRpe -> {
                _state.value = _state.value.copy(editRpe = intent.rpe)
            }
            is HistoryDetailIntent.SaveEdit -> saveEdit()
        }
    }

    private fun loadWorkout(workoutId: String) {
        viewModelScope.launch {
            _state.value = HistoryDetailState(isLoading = true, errorRes = null)
            try {
                val workout = workoutRepository.getWorkout(workoutId)
                if (workout == null) {
                    _state.value = HistoryDetailState(
                        isLoading = false,
                        errorRes = R.string.history_workout_not_found
                    )
                    return@launch
                }

                val sets = workout.sets.filter { !it.isWarmup }
                val totalSets = sets.size
                val totalReps = sets.sumOf { it.reps }
                val totalVolume = sets.sumOf { it.weightKg * it.reps }

                val exerciseIds = sets.map { it.exerciseId }.distinct()
                val prExerciseIds = mutableSetOf<String>()
                
                for (exerciseId in exerciseIds) {
                    val exercise = exerciseRepository.getExerciseById(exerciseId.toString())
                    if (exercise != null) {
                        val records = personalRecordService.getPersonalRecords(
                            exerciseId.toString(),
                            exercise.name
                        )
                        records.forEach { record ->
                            val recordDate = record.date.toEpochMilli()
                            val workoutDate = workout.startedAt.toEpochMilli()
                            if (kotlin.math.abs(recordDate - workoutDate) < 1000) {
                                prExerciseIds.add(exerciseId.toString())
                            }
                        }
                    }
                }

                val exercises = exerciseIds.mapNotNull { exerciseId ->
                    val exercise = exerciseRepository.getExerciseById(exerciseId.toString())
                    if (exercise != null) {
                        val exerciseSets = sets
                            .filter { it.exerciseId == exerciseId }
                            .sortedBy { it.completedAt }
                            .mapIndexed { index, set ->
                                SetDetail(
                                    setId = set.id.toString(),
                                    setNumber = index + 1,
                                    weight = set.weightKg,
                                    reps = set.reps,
                                    rpe = set.rpe,
                                    isWarmup = set.isWarmup,
                                )
                            }
                        val exerciseVolume = exerciseSets.sumOf { it.weight * it.reps }
                        ExerciseDetail(
                            exerciseId = exerciseId.toString(),
                            exerciseName = exercise.name,
                            muscleGroup = exercise.muscleGroup,
                            sets = exerciseSets,
                            totalVolume = exerciseVolume,
                            hasPR = prExerciseIds.contains(exerciseId.toString()),
                        )
                    } else null
                }

                val volumeByMuscleGroup = exercises
                    .groupBy { it.muscleGroup }
                    .mapValues { (_, exs) -> exs.sumOf { it.totalVolume } }

                val detail = WorkoutDetail(
                    workoutId = workoutId,
                    date = workout.startedAt.toEpochMilli(),
                    durationSeconds = workout.completedAt?.let {
                        (it.toEpochMilli() - workout.startedAt.toEpochMilli()) / 1000
                    } ?: 0L,
                    totalVolume = totalVolume,
                    totalSets = totalSets,
                    totalReps = totalReps,
                    exercises = exercises,
                    prExerciseIds = prExerciseIds,
                    volumeByMuscleGroup = volumeByMuscleGroup,
                )

                _state.value = HistoryDetailState(
                    isLoading = false,
                    errorRes = null,
                    workoutDetail = detail
                )
            } catch (e: Exception) {
                _state.value = HistoryDetailState(
                    isLoading = false,
                    errorRes = R.string.history_load_failed
                )
            }
        }
    }

    private fun saveEdit() {
        viewModelScope.launch {
            val currentState = _state.value
            val setId = currentState.editingSetId ?: return@launch
            
            val weight = currentState.editWeight.toDoubleOrNull() ?: return@launch
            val reps = currentState.editReps.toIntOrNull() ?: return@launch
            val rpe = currentState.editRpe.toDoubleOrNull()
            
            try {
                workoutRepository.updateSet(setId, weight, reps, rpe)
                
                _state.value = _state.value.copy(
                    editingSetId = null,
                    editWeight = "",
                    editReps = "",
                    editRpe = ""
                )
                
                currentState.workoutDetail?.workoutId?.let { loadWorkout(it) }
            } catch (e: Exception) {
                // Handle error - could add error state
            }
        }
    }
}
