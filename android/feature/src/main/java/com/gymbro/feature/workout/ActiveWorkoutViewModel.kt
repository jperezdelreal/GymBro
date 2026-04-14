package com.gymbro.feature.workout

import androidx.lifecycle.viewModelScope
import com.gymbro.core.error.toUserMessage
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.RpeTrendService
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.SmartDefaultsService
import com.gymbro.core.service.WorkoutPlanGenerator
import com.gymbro.feature.common.BaseViewModel
import com.gymbro.feature.common.TooltipManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val rpeTrendService: RpeTrendService,
    private val exerciseRepository: ExerciseRepository,
    private val activePlanStore: ActivePlanStore,
    private val progressionEngine: com.gymbro.core.service.ProgressionEngine,
    private val workoutDao: com.gymbro.core.database.dao.WorkoutDao,
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
        loadFatigueWarnings()
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
            _state.update { it.copy(workoutId = workout.id.toString(), isLoading = false, hasInProgressWorkout = false, errorMessage = null) }
            startElapsedTimer()
            loadPendingWorkoutDay()
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

    private fun loadFatigueWarnings() {
        viewModelScope.launch {
            try {
                val warnings = rpeTrendService.getFatigueWarnings()
                if (warnings.isNotEmpty()) {
                    val warningUis = warnings.mapNotNull { trend ->
                        val exercise = exerciseRepository.getExerciseById(trend.exerciseId)
                        val name = exercise?.name ?: return@mapNotNull null
                        FatigueWarningUi(
                            exerciseId = trend.exerciseId,
                            exerciseName = name,
                            currentAvgRpe = trend.currentAvgRpe,
                            rpeDelta = trend.previousAvgRpe?.let { trend.currentAvgRpe - it },
                        )
                    }
                    _state.update { it.copy(fatigueWarnings = warningUis) }
                }
            } catch (_: Exception) {
                // Non-critical — silently ignore if fatigue data unavailable
            }
        }
    }

    private fun loadPendingWorkoutDay() {
        val pendingDay = activePlanStore.consumePendingWorkoutDay() ?: return
        viewModelScope.launch {
            for (planned in pendingDay.exercises) {
                // Try to look up full Exercise by name from repository
                val exercise = findExerciseByName(planned.exerciseName) ?: Exercise(
                    id = UUID.randomUUID(),
                    name = planned.exerciseName,
                    muscleGroup = MuscleGroup.FULL_BODY,
                )
                val defaults = try {
                    smartDefaultsService.getDefaultsWithFallback(exercise.id.toString(), exercise)
                } catch (_: Exception) {
                    null
                }
                val progressionSuggestion = getProgressionSuggestionUi(exercise.id.toString())
                val sets = (1..planned.sets).map { setNum ->
                    WorkoutSetUi(
                        id = UUID.randomUUID().toString(),
                        setNumber = setNum,
                        weight = defaults?.weight?.toString() ?: "",
                        reps = defaults?.reps?.toString()
                            ?: planned.repsRange.split("-").firstOrNull() ?: "",
                    )
                }
                _state.update { current ->
                    current.copy(
                        exercises = current.exercises + WorkoutExerciseUi(
                            exercise = exercise,
                            sets = sets,
                            progressionSuggestion = progressionSuggestion,
                            beginnerWeightHint = defaults?.beginnerSuggestion,
                        ),
                    )
                }
            }
            // Auto-create superset groups from plan's supersetGroupId
            val supersetMap = mutableMapOf<String, MutableList<Int>>()
            pendingDay.exercises.forEachIndexed { index, planned ->
                val groupId = planned.supersetGroupId ?: return@forEachIndexed
                supersetMap.getOrPut(groupId) { mutableListOf() }.add(index)
            }
            val validGroups = supersetMap.filter { it.value.size >= 2 }
            if (validGroups.isNotEmpty()) {
                _state.update { current ->
                    current.copy(supersetGroups = validGroups)
                }
            }
        }
    }

    private suspend fun findExerciseByName(name: String): Exercise? {
        return try {
            val exercises = exerciseRepository.getFilteredExercises(null, name).first()
            exercises.firstOrNull { it.name.equals(name, ignoreCase = true) }
        } catch (_: Exception) {
            null
        }
    }

    fun onEvent(event: ActiveWorkoutEvent) {
        when (event) {
            is ActiveWorkoutEvent.ResumeWorkout -> resumeWorkout()
            is ActiveWorkoutEvent.StartNewWorkout -> startNewWorkout()
            is ActiveWorkoutEvent.AddExerciseClicked -> {
                viewModelScope.launch {
                    _effect.send(ActiveWorkoutEffect.ShowExercisePicker())
                }
            }
            is ActiveWorkoutEvent.ExercisePicked -> {
                val replacingIndex = _state.value.replacingExerciseIndex
                if (replacingIndex != null) {
                    replaceExercise(replacingIndex, event.exercise)
                } else {
                    addExercise(event.exercise)
                }
            }
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
            is ActiveWorkoutEvent.ReplaceExercise -> {
                val muscleGroup = _state.value.exercises.getOrNull(event.exerciseIndex)?.exercise?.muscleGroup
                _state.update { it.copy(replacingExerciseIndex = event.exerciseIndex) }
                viewModelScope.launch {
                    _effect.send(ActiveWorkoutEffect.ShowExercisePicker(filterMuscleGroup = muscleGroup))
                }
            }
            is ActiveWorkoutEvent.VoiceInput -> {
                updateSetField(event.exerciseIndex, event.setIndex) {
                    val updated = it.copy(weight = event.weight, reps = event.reps)
                    if (event.rpe.isNotBlank()) updated.copy(rpe = event.rpe) else updated
                }
            }
            is ActiveWorkoutEvent.StartRestTimer -> startRestTimer()
            is ActiveWorkoutEvent.SkipRestTimer -> skipRestTimer()
            is ActiveWorkoutEvent.AdjustRestTimer -> adjustRestTimer(event.deltaSeconds)
            is ActiveWorkoutEvent.CompleteWorkout -> completeWorkout()
            is ActiveWorkoutEvent.DiscardWorkout -> discardWorkout()
            is ActiveWorkoutEvent.ClearError -> _state.update { it.copy(errorMessage = null) }
            is ActiveWorkoutEvent.DismissFatigueWarnings -> _state.update { it.copy(fatigueWarnings = emptyList()) }
            is ActiveWorkoutEvent.RetryStartWorkout -> startWorkout()
            is ActiveWorkoutEvent.ToggleExerciseSelection -> toggleExerciseSelection(event.exerciseIndex)
            is ActiveWorkoutEvent.CreateSuperset -> createSuperset()
            is ActiveWorkoutEvent.UnlinkSuperset -> unlinkSuperset(event.groupId)
            is ActiveWorkoutEvent.ShowExerciseDetail -> showExerciseDetail(event.exercise)
            is ActiveWorkoutEvent.DismissExerciseDetail -> _state.update { it.copy(exerciseDetailSheet = null) }
            is ActiveWorkoutEvent.DismissPrCelebration -> _state.update { it.copy(prCelebration = null) }
            is ActiveWorkoutEvent.SetTargetDuration -> adjustExercisesForDuration(event.minutes)
            is ActiveWorkoutEvent.MoveExerciseUp -> moveExercise(event.exerciseIndex, event.exerciseIndex - 1)
            is ActiveWorkoutEvent.MoveExerciseDown -> moveExercise(event.exerciseIndex, event.exerciseIndex + 1)
            is ActiveWorkoutEvent.ReorderExercise -> moveExercise(event.fromIndex, event.toIndex)
        }
    }
    
    private fun toggleExerciseSelection(exerciseIndex: Int) {
        _state.update { current ->
            val selected = current.selectedExercises.toMutableSet()
            if (selected.contains(exerciseIndex)) {
                selected.remove(exerciseIndex)
            } else {
                selected.add(exerciseIndex)
            }
            current.copy(selectedExercises = selected)
        }
    }
    
    private fun createSuperset() {
        _state.update { current ->
            val selected = current.selectedExercises.toList().sorted()
            if (selected.size < 2) return@update current
            
            val groupId = UUID.randomUUID().toString()
            val newGroups = current.supersetGroups.toMutableMap()
            newGroups[groupId] = selected
            
            current.copy(
                supersetGroups = newGroups,
                selectedExercises = emptySet()
            )
        }
    }
    
    private fun unlinkSuperset(groupId: String) {
        _state.update { current ->
            val newGroups = current.supersetGroups.toMutableMap()
            newGroups.remove(groupId)
            current.copy(supersetGroups = newGroups)
        }
    }

    private fun showExerciseDetail(exercise: Exercise) {
        safeLaunch {
            _state.update { it.copy(exerciseDetailSheet = ExerciseDetailSheetState(exercise = exercise, isLoadingHistory = true)) }
            
            val history = workoutRepository.getExerciseHistory(exercise.id.toString(), limit = 10)
            
            val historyUi = history.map { session ->
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
                ExerciseHistorySessionUi(
                    date = session.workoutDate.atZone(java.time.ZoneId.systemDefault()).format(formatter),
                    sets = session.sets.map { set ->
                        ExerciseHistorySetUi(
                            weight = set.weightKg,
                            reps = set.reps,
                            rpe = set.rpe,
                        )
                    }
                )
            }
            
            _state.update { 
                it.copy(
                    exerciseDetailSheet = it.exerciseDetailSheet?.copy(
                        history = historyUi,
                        isLoadingHistory = false,
                    )
                )
            }
        }
    }

    private fun addExercise(exercise: Exercise) {
        safeLaunch {
            val defaults = smartDefaultsService.getDefaultsWithFallback(exercise.id.toString(), exercise)
            val progressionSuggestion = getProgressionSuggestionUi(exercise.id.toString())
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
                    progressionSuggestion = progressionSuggestion,
                    beginnerWeightHint = defaults.beginnerSuggestion,
                )
                current.copy(exercises = current.exercises + newExercise)
            }
            autoSaveState()
        }
    }

    private fun replaceExercise(exerciseIndex: Int, newExercise: Exercise) {
        safeLaunch {
            _state.update { current ->
                if (exerciseIndex !in current.exercises.indices) {
                    return@update current.copy(replacingExerciseIndex = null)
                }
                val exercises = current.exercises.toMutableList()
                val oldExerciseUi = exercises[exerciseIndex]
                exercises[exerciseIndex] = oldExerciseUi.copy(
                    exercise = newExercise,
                    progressionSuggestion = null,
                    beginnerWeightHint = null,
                )
                current.copy(
                    exercises = exercises,
                    replacingExerciseIndex = null,
                )
            }
            autoSaveState()
        }
    }

    private fun adjustExercisesForDuration(minutes: Int) {
        _state.update { current ->
            val exercises = current.exercises
            if (exercises.isEmpty()) return@update current.copy(targetDurationMinutes = minutes)

            val budgetSeconds = WorkoutPlanGenerator.workTimeBudgetSeconds(minutes)
            var accumulated = 0
            var fitCount = 0
            for (ex in exercises) {
                val sets = ex.sets.size.coerceAtLeast(1)
                val time = WorkoutPlanGenerator.estimateExerciseTimeSeconds(ex.exercise.category, sets)
                if (accumulated + time > budgetSeconds && fitCount >= WorkoutPlanGenerator.MIN_EXERCISES) break
                accumulated += time
                fitCount++
            }
            fitCount = fitCount.coerceIn(WorkoutPlanGenerator.MIN_EXERCISES, exercises.size)

            val trimmed = exercises.take(fitCount)
            current.copy(
                exercises = trimmed,
                targetDurationMinutes = minutes,
            )
        }
        autoSaveState()
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

        val weightKg = setUi.weight.toDoubleOrNull()
        if (weightKg == null) {
            _state.update { it.copy(errorMessage = "Please enter a valid weight before completing the set") }
            return
        }
        val reps = setUi.reps.toIntOrNull()
        if (reps == null) {
            _state.update { it.copy(errorMessage = "Please enter valid reps before completing the set") }
            return
        }

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

            // Auto-fill carry-forward: pre-fill subsequent empty sets with this set's weight/reps
            carryForwardToNextSets(exerciseIndex, setUi.weight, setUi.reps)

            // Check for new PRs after this set
            checkForNewPR(exerciseUi, weightKg, reps, exerciseIndex, setIndex)

            // Auto-add next set if all current sets are completed (max 5)
            val updatedExercise = _state.value.exercises.getOrNull(exerciseIndex)
            if (updatedExercise != null &&
                updatedExercise.sets.all { it.isCompleted } &&
                updatedExercise.sets.size < 5
            ) {
                addSet(exerciseIndex)
            }

            recalculateTotals()

            autoSaveState()

            val supersetGroup = currentState.supersetGroups.values.firstOrNull { exerciseIndex in it }
            val shouldStartTimer = if (supersetGroup != null) {
                exerciseIndex == supersetGroup.last()
            } else {
                true
            }
            
            if (shouldStartTimer) {
                startRestTimer()
            }
        }
    }

    /**
     * Intra-session carry-forward: after completing a set, pre-fill subsequent
     * incomplete sets of the same exercise with the just-used weight and reps.
     * Different from SmartDefaults (which uses previous session data).
     */
    private fun carryForwardToNextSets(exerciseIndex: Int, weight: String, reps: String) {
        _state.update { current ->
            val exercises = current.exercises.toMutableList()
            if (exerciseIndex !in exercises.indices) return@update current
            val exerciseUi = exercises[exerciseIndex]
            val sets = exerciseUi.sets.toMutableList()
            var changed = false
            for (i in sets.indices) {
                val s = sets[i]
                if (!s.isCompleted && s.weight.isEmpty()) {
                    sets[i] = s.copy(weight = weight, reps = if (s.reps.isEmpty()) reps else s.reps)
                    changed = true
                }
            }
            if (changed) {
                exercises[exerciseIndex] = exerciseUi.copy(sets = sets)
                current.copy(exercises = exercises)
            } else {
                current
            }
        }
    }

    private suspend fun checkForNewPR(
        exerciseUi: WorkoutExerciseUi,
        weightKg: Double,
        reps: Int,
        exerciseIndex: Int,
        setIndex: Int,
    ) {
        try {
            val prs = personalRecordService.getPersonalRecords(
                exerciseId = exerciseUi.exercise.id.toString(),
                exerciseName = exerciseUi.exercise.name,
            )
            val newPR = prs.firstOrNull { pr ->
                val prev = pr.previousValue
                prev != null && prev < pr.value
            }
            if (newPR != null) {
                updateSetField(exerciseIndex, setIndex) { it.copy(isPR = true) }
                val valueStr = if (newPR.value == newPR.value.toLong().toDouble()) {
                    newPR.value.toLong().toString()
                } else {
                    String.format("%.1f", newPR.value)
                }
                val prevStr = newPR.previousValue?.let { prev ->
                    if (prev == prev.toLong().toDouble()) prev.toLong().toString()
                    else String.format("%.1f", prev)
                }
                val weightStr = if (weightKg == weightKg.toLong().toDouble()) {
                    weightKg.toLong().toString()
                } else {
                    String.format("%.1f", weightKg)
                }
                _state.update {
                    it.copy(
                        prCelebration = PrCelebrationUi(
                            exerciseName = exerciseUi.exercise.name,
                            recordType = newPR.type,
                            value = "$weightStr kg",
                            reps = reps.toString(),
                            previousValue = prevStr,
                        )
                    )
                }
                // Auto-dismiss after 4 seconds
                delay(4000)
                _state.update { it.copy(prCelebration = null) }
            }
        } catch (_: Exception) {
            // Non-critical — don't interrupt workout flow
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

    private fun moveExercise(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val exercises = current.exercises.toMutableList()
            if (fromIndex !in exercises.indices || toIndex !in exercises.indices) return@update current
            java.util.Collections.swap(exercises, fromIndex, toIndex)
            // Update superset group indices to reflect the swap
            val updatedGroups = current.supersetGroups.mapValues { (_, indices) ->
                indices.map { idx ->
                    when (idx) {
                        fromIndex -> toIndex
                        toIndex -> fromIndex
                        else -> idx
                    }
                }.sorted()
            }
            current.copy(exercises = exercises, supersetGroups = updatedGroups)
        }
        autoSaveState()
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

            val exerciseCount = currentState.exercises.count { ex ->
                ex.sets.any { it.isCompleted && !it.isWarmup }
            }
            
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

    private suspend fun getProgressionSuggestionUi(exerciseId: String): ProgressionSuggestionUi? {
        return try {
            val suggestion = progressionEngine.getSuggestion(exerciseId) ?: return null
            
            // Get last workout data for display
            val sets = workoutDao.getSetsByExercise(exerciseId)
            if (sets.isEmpty()) return null
            
            val lastWorkoutId = sets.last().workoutId
            val lastWorkoutSets = sets.filter { it.workoutId == lastWorkoutId && !it.isWarmup }
            if (lastWorkoutSets.isEmpty()) return null
            
            val lastWeight = lastWorkoutSets.maxOf { it.weight }
            val lastReps = lastWorkoutSets.maxOf { it.reps }
            val lastRpe = lastWorkoutSets.mapNotNull { it.rpe }.maxOrNull()
            
            ProgressionSuggestionUi(
                lastWeight = lastWeight,
                lastReps = lastReps,
                lastRpe = lastRpe,
                suggestedWeight = suggestion.suggestedWeightKg,
                reason = suggestion.reason,
            )
        } catch (e: Exception) {
            null
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
