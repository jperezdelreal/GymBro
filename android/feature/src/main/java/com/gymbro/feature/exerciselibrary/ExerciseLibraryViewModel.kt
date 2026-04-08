package com.gymbro.feature.exerciselibrary

import androidx.lifecycle.viewModelScope
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.feature.common.BaseViewModel
import com.gymbro.feature.common.TooltipManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    val tooltipManager: TooltipManager,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ExerciseLibraryState())
    val state: StateFlow<ExerciseLibraryState> = _state.asStateFlow()

    private val _effect = Channel<ExerciseLibraryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadExercises()
    }

    fun onEvent(event: ExerciseLibraryEvent) {
        when (event) {
            is ExerciseLibraryEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                loadExercises()
            }
            is ExerciseLibraryEvent.MuscleGroupSelected -> {
                _state.update { it.copy(selectedMuscleGroup = event.muscleGroup) }
                loadExercises()
            }
            is ExerciseLibraryEvent.ExerciseClicked -> {
                viewModelScope.launch {
                    _effect.send(
                        ExerciseLibraryEffect.NavigateToDetail(event.exercise.id.toString()),
                    )
                }
            }
        }
    }

    private fun loadExercises() {
        loadJob?.cancel()
        loadJob = safeLaunch(
            onError = { error ->
                _state.update { it.copy(isLoading = false) }
                handleError(error) { loadExercises() }
            }
        ) {
            _state.update { it.copy(isLoading = true) }
            val currentState = _state.value
            val muscleGroup = currentState.selectedMuscleGroup?.name
            val query = currentState.searchQuery.takeIf { it.isNotBlank() }

            exerciseRepository.getFilteredExercises(muscleGroup, query)
                .collect { exercises ->
                    _state.update {
                        it.copy(exercises = exercises, isLoading = false, error = null)
                    }
                }
        }
    }
    
    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
