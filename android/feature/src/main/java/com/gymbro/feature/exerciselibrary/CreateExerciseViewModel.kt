package com.gymbro.feature.exerciselibrary

import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.Exercise
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.feature.common.BaseViewModel
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
class CreateExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(CreateExerciseState())
    val state: StateFlow<CreateExerciseState> = _state.asStateFlow()

    private val _effect = Channel<CreateExerciseEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: CreateExerciseEvent) {
        when (event) {
            is CreateExerciseEvent.NameChanged -> {
                _state.update { it.copy(exerciseName = event.name, nameError = null) }
            }
            is CreateExerciseEvent.MuscleGroupSelected -> {
                _state.update { it.copy(selectedMuscleGroup = event.muscleGroup) }
            }
            is CreateExerciseEvent.CategorySelected -> {
                _state.update { it.copy(selectedCategory = event.category) }
            }
            is CreateExerciseEvent.EquipmentSelected -> {
                _state.update { it.copy(selectedEquipment = event.equipment) }
            }
            is CreateExerciseEvent.DescriptionChanged -> {
                _state.update { it.copy(description = event.description) }
            }
            is CreateExerciseEvent.SaveClicked -> {
                saveExercise()
            }
            is CreateExerciseEvent.CancelClicked -> {
                viewModelScope.launch {
                    _effect.send(CreateExerciseEffect.NavigateBack)
                }
            }
        }
    }

    private fun saveExercise() {
        val currentState = _state.value
        val name = currentState.exerciseName.trim()

        if (name.isEmpty()) {
            _state.update { it.copy(nameError = "Exercise name is required") }
            return
        }

        safeLaunch(
            onError = { error ->
                _state.update { it.copy(isLoading = false) }
                handleError(error) { saveExercise() }
            }
        ) {
            _state.update { it.copy(isLoading = true) }

            // Check if name already exists
            if (exerciseRepository.isExerciseNameTaken(name)) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        nameError = "An exercise with this name already exists"
                    )
                }
                return@safeLaunch
            }

            val exercise = Exercise(
                name = name,
                muscleGroup = currentState.selectedMuscleGroup,
                category = currentState.selectedCategory,
                equipment = currentState.selectedEquipment,
                description = currentState.description.trim(),
            )

            exerciseRepository.addExercise(exercise)

            _state.update { it.copy(isLoading = false) }
            _effect.send(CreateExerciseEffect.NavigateBack)
        }
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
