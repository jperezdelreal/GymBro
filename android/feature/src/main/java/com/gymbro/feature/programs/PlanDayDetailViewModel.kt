package com.gymbro.feature.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.service.ActivePlanStore
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
class PlanDayDetailViewModel @Inject constructor(
    private val activePlanStore: ActivePlanStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanDayDetailState())
    val state: StateFlow<PlanDayDetailState> = _state.asStateFlow()

    private val _effect = Channel<PlanDayDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentDayNumber: Int = -1
    private var originalWorkoutDay: WorkoutDay? = null

    fun onIntent(intent: PlanDayDetailIntent) {
        when (intent) {
            is PlanDayDetailIntent.LoadDay -> loadDay(intent.dayNumber)
            is PlanDayDetailIntent.Retry -> {
                if (currentDayNumber > 0) loadDay(currentDayNumber)
            }
            is PlanDayDetailIntent.StartWorkout -> startWorkout()
            is PlanDayDetailIntent.ToggleEditMode -> toggleEditMode()
            is PlanDayDetailIntent.RemoveExercise -> removeExercise(intent.exerciseId)
            is PlanDayDetailIntent.UpdateExercise -> updateExercise(intent.exercise)
            is PlanDayDetailIntent.SaveChanges -> saveChanges()
            is PlanDayDetailIntent.DiscardChanges -> discardChanges()
            is PlanDayDetailIntent.AddExercise -> addExercise()
            is PlanDayDetailIntent.ExerciseSelected -> addSelectedExercise(intent.exerciseName)
        }
    }

    private fun loadDay(dayNumber: Int) {
        currentDayNumber = dayNumber
        val plan = activePlanStore.getPlan()

        if (plan == null) {
            _state.value = PlanDayDetailState(
                isLoading = false,
                error = "No active plan found. Please generate a plan first.",
            )
            return
        }

        val workoutDay = plan.workoutDays.find { it.dayNumber == dayNumber }

        if (workoutDay == null) {
            _state.value = PlanDayDetailState(
                isLoading = false,
                error = "Day $dayNumber not found in the current plan.",
            )
            return
        }

        originalWorkoutDay = workoutDay
        _state.value = PlanDayDetailState(
            isLoading = false,
            error = null,
            workoutDay = workoutDay,
            planName = plan.name,
        )
    }

    private fun toggleEditMode() {
        _state.update { it.copy(isEditMode = !it.isEditMode) }
    }

    private fun removeExercise(exerciseId: String) {
        val currentDay = _state.value.workoutDay ?: return
        val updatedExercises = currentDay.exercises.filter { it.id != exerciseId }
        
        _state.update {
            it.copy(
                workoutDay = currentDay.copy(exercises = updatedExercises),
                hasUnsavedChanges = true,
            )
        }
    }

    private fun updateExercise(exercise: PlannedExercise) {
        val currentDay = _state.value.workoutDay ?: return
        val updatedExercises = currentDay.exercises.map { 
            if (it.id == exercise.id) exercise else it 
        }
        
        _state.update {
            it.copy(
                workoutDay = currentDay.copy(exercises = updatedExercises),
                hasUnsavedChanges = true,
            )
        }
    }

    private fun addExercise() {
        viewModelScope.launch {
            _effect.send(PlanDayDetailEffect.NavigateToExercisePicker)
        }
    }

    private fun addSelectedExercise(exerciseName: String) {
        val currentDay = _state.value.workoutDay ?: return
        val newExercise = PlannedExercise(
            exerciseName = exerciseName,
            sets = 3,
            repsRange = "8-12",
            restSeconds = 90,
        )
        val updatedExercises = currentDay.exercises + newExercise
        
        _state.update {
            it.copy(
                workoutDay = currentDay.copy(exercises = updatedExercises),
                hasUnsavedChanges = true,
            )
        }
    }

    private fun saveChanges() {
        val currentDay = _state.value.workoutDay ?: return
        val plan = activePlanStore.getPlan() ?: return
        
        val updatedDays = plan.workoutDays.map { day ->
            if (day.dayNumber == currentDayNumber) currentDay else day
        }
        
        val updatedPlan = plan.copy(
            workoutDays = updatedDays,
            isModified = true,
        ).markAsModified()
        
        activePlanStore.setPlan(updatedPlan)
        originalWorkoutDay = currentDay
        
        _state.update { 
            it.copy(
                isEditMode = false, 
                hasUnsavedChanges = false,
            ) 
        }
        
        viewModelScope.launch {
            _effect.send(PlanDayDetailEffect.ShowSaveSuccess)
        }
    }

    private fun discardChanges() {
        _state.update {
            it.copy(
                workoutDay = originalWorkoutDay,
                isEditMode = false,
                hasUnsavedChanges = false,
            )
        }
    }

    private fun startWorkout() {
        val workoutDay = _state.value.workoutDay ?: return
        activePlanStore.setPendingWorkoutDay(workoutDay)
        viewModelScope.launch {
            _effect.send(PlanDayDetailEffect.NavigateToActiveWorkout)
        }
    }
}
