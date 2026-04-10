package com.gymbro.feature.workout

import androidx.lifecycle.viewModelScope
import com.gymbro.core.error.toUserMessage
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.SmartDefaultsService
import com.gymbro.feature.common.BaseViewModel
import com.gymbro.feature.common.TooltipManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val personalRecordService: PersonalRecordService,
    private val smartDefaultsService: SmartDefaultsService,
    val tooltipManager: TooltipManager,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ActiveWorkoutState())
    val state: StateFlow<ActiveWorkoutState> = _state.asStateFlow()

    private val _effect = Channel<ActiveWorkoutEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var shouldAutoSave = true

    init {
        checkForInProgressWorkout()
    }

    private fun checkForInProgressWorkout() {
        safeLaunch(
            onError = { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to check for in-progress workout: ${error.toUserMessage()}"
                    )
                }
            }
        ) {
            val inProgress = workoutRepository.getInProgressWorkout()
            if (inProgress != null) {
                _state.update { 
                    it.copy(
                        hasInProgressWorkout = true,
                        isLoading = false,
                    )
                }
            } else {
                startWorkout()
            }
        }
    }

    private fun startWorkout() {
        safeLaunch(
            onError = { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to start workout: ${error.toUserMessage()}"
                    )
                }
            }
        ) {
            val workout = workoutRepository.startWorkout()
            _state.update { it.copy(workoutId = workout.id.toString(), isLoading = false, errorMessage = null) }
            startElapsedTimer()
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun onEvent(event: ActiveWorkoutEvent) {
        when (event) {
            is ActiveWorkoutEvent.ResumeWorkout -> resumeWorkout()
            is ActiveWorkoutEvent.StartNewWorkout -> startNewWorkout()
            is ActiveWorkoutEvent.AddExerciseClicked -> {
                viewModelScope.launch {
                    _effect.send(ActiveWorkoutEffect.ShowExercisePicker)
                }
            }
            is ActiveWorkoutEvent.ExercisePicked -> addExercise(event.exercise)
            is ActiveWorkoutEvent.AddSet -> addSet(event.exerciseIndex)
            is ActiveWorkoutEvent.UpdateSetWeight -> updateSetField(event.exerciseIndex, event.setIndex) {
                it.copy(weight = event.weight)
            }
            is ActiveWorkoutEvent.UpdateSetReps -> updateSetField(event.exerciseIndex, event.setIndex) {
                it.copy(reps = event.reps)
            }
            is ActiveWorkoutEvent.UpdateSetRpe -> updateSetField(event.exerciseIndex, event.setIndex) {
                it.copy(rpe = event.rpe)
            }
            is ActiveWorkoutEvent.ToggleWarmup -> updateSetField(event.exerciseIndex, event.setIndex) {
                it.copy(isWarmup = !it.isWarmup)
            }
            is ActiveWorkoutEvent.CompleteSet -> completeSet(event.exerciseIndex, event.setIndex)
            is ActiveWorkoutEvent.QuickCompleteSet -> completeSet(event.exerciseIndex, event.setIndex)
            is ActiveWorkoutEvent.RemoveSet -> removeSet(event.exerciseIndex, event.setIndex)
            is ActiveWorkoutEvent.RemoveExercise -> removeExercise(event.exerciseIndex)
            is ActiveWorkoutEvent.VoiceInput -> {
                updateSetField(event.exerciseIndex, event.setIndex) {
                    it.copy(weight = event.weight, reps = event.reps)
                }
            }
            is ActiveWorkoutEvent.StartRestTimer -> startRestTimer()
            is ActiveWorkoutEvent.SkipRestTimer -> skipRestTimer()
            is ActiveWorkoutEvent.AdjustRestTimer -> adjustRestTimer(event.deltaSeconds)
            is ActiveWorkoutEvent.CompleteWorkout -> completeWorkout()
            is ActiveWorkoutEvent.DiscardWorkout -> discardWorkout()
            is ActiveWorkoutEvent.ClearError -> _state.update { it.copy(errorMessage = null) }
            is ActiveWorkoutEvent.RetryStartWorkout -> startWorkout()
        }
    }

    private fun addExercise(exercise: Exercise) {
        safeLaunch {
            val defaults = smartDefaultsService.getDefaults(exercise.id.toString())
            _state.update { current ->
                val newExercise = WorkoutExerciseUi(
                    exercise = exercise,
                    sets = listOf(
                        WorkoutSetUi(
                            id = UUID.randomUUID().toString(),
                            setNumber = 1,
                            weight = defaults.weight?.toString() ?: "",
                            reps = defaults.reps?.toString() ?: "",
                        ),
                    ),
                )
                current.copy(exercises = current.exercises + newExercise)
            }
            autoSaveState()
        }
    }

    private fun addSet(exerciseIndex: Int) {
        _state.update { current ->
            val exercises = current.exercises.toMutableList()
            if (exerciseIndex !in exercises.indices) return@update current
            val exerciseUi = exercises[exerciseIndex]

            // Pre-fill from last set's values
            val lastSet = exerciseUi.sets.lastOrNull()
            val newSet = WorkoutSetUi(
                id = UUID.randomUUID().toString(),
                setNumber = exerciseUi.sets.size + 1,
                weight = lastSet?.weight ?: "",
                reps = lastSet?.reps ?: "",
            )
            exercises[exerciseIndex] = exerciseUi.copy(sets = exerciseUi.sets + newSet)
            current.copy(exercises = exercises)
        }
        autoSaveState()
    }

    private fun updateSetField(exerciseIndex: Int, setIndex: Int, transform: (WorkoutSetUi) -> WorkoutSetUi) {
        _state.update { current ->
            val exercises = current.exercises.toMutableList()
            if (exerciseIndex !in exercises.indices) return@update current
            val exerciseUi = exercises[exerciseIndex]
            val sets = exerciseUi.sets.toMutableList()
            if (setIndex !in sets.indices) return@update current
            sets[setIndex] = transform(sets[setIndex])
            exercises[exerciseIndex] = exerciseUi.copy(sets = sets)
            current.copy(exercises = exercises)
        }
    }

    private fun completeSet(exerciseIndex: Int, setIndex: Int) {
        val currentState = _state.value
        val workoutId = currentState.workoutId ?: return
        val exercises = currentState.exercises
        if (exerciseIndex !in exercises.indices) return
        val exerciseUi = exercises[exerciseIndex]
        if (setIndex !in exerciseUi.sets.indices) return
        val setUi = exerciseUi.sets[setIndex]

        if (setUi.isCompleted) return

        val weightKg = setUi.weight.toDoubleOrNull() ?: return
        val reps = setUi.reps.toIntOrNull() ?: return

        safeLaunch(
            onError = { error ->
                _state.update { 
                    it.copy(errorMessage = "Failed to save set: ${error.toUserMessage()}")
                }
            }
        ) {
            val rpeValue = setUi.rpe.toDoubleOrNull()
            val rirValue = rpeValue?.let { ExerciseSet.rpeToRir(it) }
            val exerciseSet = ExerciseSet(
                id = UUID.fromString(setUi.id),
                exerciseId = exerciseUi.exercise.id,
                weightKg = weightKg,
                reps = reps,
                rpe = rpeValue,
                rir = rirValue,
                isWarmup = setUi.isWarmup,
            )
            workoutRepository.addSet(workoutId, exerciseSet)

            updateSetField(exerciseIndex, setIndex) { it.copy(isCompleted = true) }

            // Recalculate totals
            recalculateTotals()

            // Auto-save state
            autoSaveState()

            // Auto-start rest timer
            startRestTimer()
        }
    }

    private fun removeSet(exerciseIndex: Int, setIndex: Int) {
        _state.update { current ->
            val exercises = current.exercises.toMutableList()
            if (exerciseIndex !in exercises.indices) return@update current
            val exerciseUi = exercises[exerciseIndex]
            val sets = exerciseUi.sets.toMutableList()
            if (setIndex !in sets.indices) return@update current

            val removedSet = sets.removeAt(setIndex)
            // Re-number remaining sets
            val renumbered = sets.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
            exercises[exerciseIndex] = exerciseUi.copy(sets = renumbered)

            if (removedSet.isCompleted) {
                viewModelScope.launch { workoutRepository.removeSet(removedSet.id) }
            }

            current.copy(exercises = exercises)
        }
        recalculateTotals()
    }

    private fun removeExercise(exerciseIndex: Int) {
        _state.update { current ->
            val exercises = current.exercises.toMutableList()
            if (exerciseIndex !in exercises.indices) return@update current
            val removed = exercises.removeAt(exerciseIndex)
            // Remove completed sets from DB
            removed.sets.filter { it.isCompleted }.forEach { set ->
                viewModelScope.launch { workoutRepository.removeSet(set.id) }
            }
            current.copy(exercises = exercises)
        }
        recalculateTotals()
    }

    private fun recalculateTotals() {
        _state.update { current ->
            var totalVolume = 0.0
            var totalSets = 0
            current.exercises.forEach { ex ->
                ex.sets.filter { it.isCompleted && !it.isWarmup }.forEach { set ->
                    val w = set.weight.toDoubleOrNull() ?: 0.0
                    val r = set.reps.toIntOrNull() ?: 0
                    totalVolume += w * r
                    totalSets++
                }
            }
            current.copy(totalVolume = totalVolume, totalSets = totalSets)
        }
    }

    private fun startRestTimer() {
        restTimerJob?.cancel()
        _state.update { it.copy(isRestTimerActive = true, restTimerSeconds = it.restTimerTotal) }
        restTimerJob = viewModelScope.launch {
            while (_state.value.restTimerSeconds > 0) {
                delay(1000)
                _state.update { it.copy(restTimerSeconds = it.restTimerSeconds - 1) }
            }
            _state.update { it.copy(isRestTimerActive = false) }
            _effect.send(ActiveWorkoutEffect.RestTimerFinished)
        }
        autoSaveState()
    }

    private fun skipRestTimer() {
        restTimerJob?.cancel()
        _state.update { it.copy(isRestTimerActive = false, restTimerSeconds = 0) }
    }

    private fun adjustRestTimer(deltaSeconds: Int) {
        _state.update {
            val newTime = (it.restTimerSeconds + deltaSeconds).coerceAtLeast(0)
            val newTotal = (it.restTimerTotal + deltaSeconds).coerceIn(30, 300)
            it.copy(restTimerSeconds = newTime, restTimerTotal = newTotal)
        }
    }

    private fun completeWorkout() {
        val currentState = _state.value
        val workoutId = currentState.workoutId ?: return

        _state.update { it.copy(isCompleting = true) }

        safeLaunch(
            onError = { error ->
                _state.update { 
                    it.copy(
                        isCompleting = false,
                        errorMessage = "Failed to complete workout: ${error.toUserMessage()}"
                    )
                }
            }
        ) {
            workoutRepository.completeWorkout(
                workoutId = workoutId,
                durationSeconds = currentState.elapsedSeconds,
                notes = "",
            )
            
            // Clear in-progress state
            workoutRepository.clearInProgressWorkout(workoutId)
            
            timerJob?.cancel()
            restTimerJob?.cancel()

            val exerciseCount = currentState.exercises.size
            
            // Check for PRs
            val allPRs = mutableListOf<com.gymbro.core.model.PersonalRecord>()
            currentState.exercises.forEach { exerciseUi ->
                val exercisePRs = personalRecordService.getPersonalRecords(
                    exerciseId = exerciseUi.exercise.id.toString(),
                    exerciseName = exerciseUi.exercise.name,
                )
                // Filter to only newly set PRs (where previous value exists and is less than current)
                val newPRs = exercisePRs.filter { pr ->
                    pr.previousValue?.let { it < pr.value } ?: true
                }
                allPRs.addAll(newPRs)
            }
            
            _effect.send(
                ActiveWorkoutEffect.NavigateToSummary(
                    durationSeconds = currentState.elapsedSeconds,
                    totalVolume = currentState.totalVolume,
                    totalSets = currentState.totalSets,
                    exerciseCount = exerciseCount,
                    personalRecords = allPRs,
                ),
            )
        }
    }

    private fun discardWorkout() {
        val workoutId = _state.value.workoutId
        timerJob?.cancel()
        restTimerJob?.cancel()
        viewModelScope.launch {
            if (workoutId != null) {
                workoutRepository.clearInProgressWorkout(workoutId)
            }
            _effect.send(ActiveWorkoutEffect.NavigateBack)
        }
    }

    private fun resumeWorkout() {
        safeLaunch(
            onError = { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to resume workout: ${error.toUserMessage()}"
                    )
                }
            }
        ) {
            val inProgress = workoutRepository.getInProgressWorkout()
            if (inProgress != null) {
                shouldAutoSave = false
                _state.update { current ->
                    current.copy(
                        workoutId = inProgress.workoutId,
                        exercises = inProgress.exercises.map { ex ->
                            WorkoutExerciseUi(
                                exercise = ex.exercise,
                                sets = ex.sets.map { set ->
                                    WorkoutSetUi(
                                        id = set.id,
                                        setNumber = set.setNumber,
                                        weight = set.weight,
                                        reps = set.reps,
                                        rpe = set.rpe,
                                        isWarmup = set.isWarmup,
                                        isCompleted = set.isCompleted,
                                    )
                                }
                            )
                        },
                        elapsedSeconds = inProgress.elapsedSeconds,
                        totalVolume = inProgress.totalVolume,
                        totalSets = inProgress.totalSets,
                        restTimerSeconds = inProgress.restTimerSeconds,
                        restTimerTotal = inProgress.restTimerTotal,
                        isRestTimerActive = inProgress.isRestTimerActive,
                        hasInProgressWorkout = false,
                        isLoading = false,
                    )
                }
                shouldAutoSave = true
                startElapsedTimer()
                if (inProgress.isRestTimerActive) {
                    startRestTimer()
                }
            } else {
                startWorkout()
            }
        }
    }

    private fun startNewWorkout() {
        safeLaunch(
            onError = { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to clear old workout: ${error.toUserMessage()}"
                    )
                }
            }
        ) {
            val inProgress = workoutRepository.getInProgressWorkout()
            if (inProgress != null) {
                workoutRepository.clearInProgressWorkout(inProgress.workoutId)
            }
            startWorkout()
        }
    }

    private fun autoSaveState() {
        if (!shouldAutoSave) return
        val currentState = _state.value
        val workoutId = currentState.workoutId ?: return

        viewModelScope.launch {
            try {
                val inProgressWorkout = com.gymbro.core.model.InProgressWorkout(
                    workoutId = workoutId,
                    exercises = currentState.exercises.map { ex ->
                        com.gymbro.core.model.InProgressExercise(
                            exercise = ex.exercise,
                            sets = ex.sets.map { set ->
                                com.gymbro.core.model.InProgressSet(
                                    id = set.id,
                                    setNumber = set.setNumber,
                                    weight = set.weight,
                                    reps = set.reps,
                                    rpe = set.rpe,
                                    isWarmup = set.isWarmup,
                                    isCompleted = set.isCompleted,
                                )
                            }
                        )
                    },
                    elapsedSeconds = currentState.elapsedSeconds,
                    totalVolume = currentState.totalVolume,
                    totalSets = currentState.totalSets,
                    restTimerSeconds = currentState.restTimerSeconds,
                    restTimerTotal = currentState.restTimerTotal,
                    isRestTimerActive = currentState.isRestTimerActive,
                )
                workoutRepository.saveInProgressWorkout(inProgressWorkout)
            } catch (e: Exception) {
                // Silent failure - don't interrupt user's workout
                android.util.Log.e("ActiveWorkoutViewModel", "Failed to auto-save workout state", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restTimerJob?.cancel()
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
