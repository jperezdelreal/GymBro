package com.gymbro.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.service.WorkoutGeneratorService
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
class SmartWorkoutViewModel @Inject constructor(
    private val workoutGeneratorService: WorkoutGeneratorService,
) : ViewModel() {

    private val _state = MutableStateFlow(SmartWorkoutState())
    val state: StateFlow<SmartWorkoutState> = _state.asStateFlow()

    private val _effect = Channel<SmartWorkoutEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        generateWorkout()
    }

    fun handleEvent(event: SmartWorkoutEvent) {
        when (event) {
            SmartWorkoutEvent.RegenerateWorkout -> generateWorkout()
            SmartWorkoutEvent.StartWorkout -> startWorkout()
            SmartWorkoutEvent.NavigateBack -> navigateBack()
        }
    }

    private fun generateWorkout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isGenerating = true, error = null) }
            try {
                val suggestion = workoutGeneratorService.generateSmartWorkout(lookbackDays = 7)
                
                val exerciseUis = suggestion.exercises.map { suggested ->
                    SuggestedExerciseUi(
                        exercise = suggested.exercise,
                        targetSets = suggested.targetSets,
                        targetReps = "${suggested.targetReps.first}-${suggested.targetReps.last}",
                        suggestedWeight = suggested.suggestedWeight?.let { String.format("%.1f kg", it) },
                        progressionHint = suggested.progressionHint,
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        isGenerating = false,
                        exercises = exerciseUis,
                        targetMuscleGroups = suggestion.targetMuscleGroups,
                        reasoning = suggestion.reasoning,
                        recoveryScore = suggestion.recoveryScore,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isGenerating = false,
                        error = e.message ?: "Failed to generate workout",
                    )
                }
            }
        }
    }

    private fun startWorkout() {
        viewModelScope.launch {
            val exercises = _state.value.exercises.map { it.exercise }
            if (exercises.isNotEmpty()) {
                _effect.send(SmartWorkoutEffect.StartWorkoutWithExercises(exercises))
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.send(SmartWorkoutEffect.NavigateBack)
        }
    }
}
