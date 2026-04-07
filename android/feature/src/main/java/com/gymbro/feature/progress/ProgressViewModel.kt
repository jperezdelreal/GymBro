package com.gymbro.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.PlateauDetectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val prService: PersonalRecordService,
    private val exerciseRepository: ExerciseRepository,
    private val plateauDetectionService: PlateauDetectionService,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state.asStateFlow()

    private val _effects = Channel<ProgressEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadData()
    }

    fun onEvent(event: ProgressEvent) {
        when (event) {
            is ProgressEvent.SelectExercise -> selectExercise(event.exerciseId)
            is ProgressEvent.RefreshData -> loadData()
            is ProgressEvent.ViewWorkoutDetail -> {
                viewModelScope.launch {
                    _effects.send(ProgressEffect.NavigateToWorkoutDetail(event.workoutId))
                }
            }
            is ProgressEvent.DismissPlateauAlert -> {
                _state.update { 
                    it.copy(
                        plateauAlerts = it.plateauAlerts.filter { alert -> 
                            alert.exerciseId != event.exerciseId 
                        }
                    )
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val history = prService.getWorkoutHistory()
            val exerciseIds = prService.getExerciseIdsWithHistory()

            val exerciseOptions = exerciseIds.mapNotNull { id ->
                val exercise = exerciseRepository.getExerciseById(id)
                exercise?.let { ExerciseOption(id = id, name = it.name) }
            }.sortedBy { it.name }

            val allRecords = exerciseIds.flatMap { id ->
                val exercise = exerciseRepository.getExerciseById(id)
                if (exercise != null) {
                    prService.getPersonalRecords(id, exercise.name)
                } else emptyList()
            }

            val selectedId = _state.value.selectedExerciseId ?: exerciseOptions.firstOrNull()?.id
            val chartData = if (selectedId != null) {
                prService.getE1RMHistory(selectedId)
            } else emptyList()

            val plateauAlerts = plateauDetectionService.detectAllPlateaus(
                exerciseOptions.map { it.id to it.name }
            )

            _state.update {
                it.copy(
                    workoutHistory = history,
                    personalRecords = allRecords,
                    exerciseOptions = exerciseOptions,
                    selectedExerciseId = selectedId,
                    chartData = chartData,
                    plateauAlerts = plateauAlerts,
                    isLoading = false,
                )
            }
        }
    }

    private fun selectExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedExerciseId = exerciseId) }
            val chartData = prService.getE1RMHistory(exerciseId)
            val exercise = exerciseRepository.getExerciseById(exerciseId)
            val records = if (exercise != null) {
                prService.getPersonalRecords(exerciseId, exercise.name)
            } else emptyList()

            _state.update {
                it.copy(
                    chartData = chartData,
                    personalRecords = it.personalRecords
                        .filter { pr -> pr.exerciseId != exerciseId } + records,
                )
            }
        }
    }
}
