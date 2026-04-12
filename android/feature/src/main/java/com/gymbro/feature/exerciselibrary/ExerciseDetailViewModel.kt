package com.gymbro.feature.exerciselibrary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gymbro.core.R
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.feature.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                    it.copy(isLoading = false, error = context.getString(R.string.exercise_detail_load_error))
                }
            }
        ) {
            _state.update { it.copy(isLoading = true, error = null) }
            val exercise = exerciseRepository.getExerciseById(exerciseId)
            if (exercise != null) {
                _state.update { it.copy(exercise = exercise, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = context.getString(R.string.exercise_detail_not_found)) }
            }
        }
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
