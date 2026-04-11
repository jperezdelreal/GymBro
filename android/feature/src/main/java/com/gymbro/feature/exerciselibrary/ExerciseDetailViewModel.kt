package com.gymbro.feature.exerciselibrary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.feature.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : BaseViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _state = MutableStateFlow(ExerciseDetailState())
    val state: StateFlow<ExerciseDetailState> = _state.asStateFlow()

    private val _effect = Channel<ExerciseDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadExercise()
    }

    fun onEvent(event: ExerciseDetailEvent) {
        when (event) {
            is ExerciseDetailEvent.RetryClicked -> loadExercise()
        }
    }

    private fun loadExercise() {
        safeLaunch(
            onError = { error ->
                _state.update {
                    it.copy(isLoading = false, error = "Failed to load exercise")
                }
            }
        ) {
            _state.update { it.copy(isLoading = true, error = null) }
            val exercise = exerciseRepository.getExerciseById(exerciseId)
            if (exercise != null) {
                _state.update { it.copy(exercise = exercise, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Exercise not found") }
            }
        }
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
