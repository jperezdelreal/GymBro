package com.gymbro.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.PersonalRecordService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val personalRecordService: PersonalRecordService,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryListState())
    val state: StateFlow<HistoryListState> = _state.asStateFlow()

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    init {
        loadHistory()
    }

    fun onIntent(intent: HistoryListIntent) {
        when (intent) {
            is HistoryListIntent.LoadHistory -> loadHistory()
            is HistoryListIntent.Retry -> loadHistory()
            is HistoryListIntent.WorkoutClicked -> {
                // Navigation handled in composable
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _state.value = HistoryListState(isLoading = true, error = null)
            try {
                val historyItems = personalRecordService.getWorkoutHistory()
                
                val workoutListItems = historyItems.map { historyItem ->
                    val workout = workoutRepository.getWorkout(historyItem.workoutId)
                    val sets = workout?.sets?.filter { !it.isWarmup } ?: emptyList()
                    val exerciseIds = sets.map { it.exerciseId }.distinct()
                    
                    val muscleGroups = exerciseIds.mapNotNull { exerciseId ->
                        exerciseRepository.getExerciseById(exerciseId.toString())?.muscleGroup
                    }.toSet()

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
                                val workoutDate = historyItem.date.toEpochMilli()
                                if (kotlin.math.abs(recordDate - workoutDate) < 1000) {
                                    prExerciseIds.add(exerciseId.toString())
                                }
                            }
                        }
                    }

                    WorkoutListItem(
                        workoutId = historyItem.workoutId,
                        date = historyItem.date.toEpochMilli(),
                        durationSeconds = historyItem.durationSeconds,
                        exerciseCount = historyItem.exerciseCount,
                        totalVolume = historyItem.totalVolume,
                        totalSets = sets.size,
                        muscleGroups = muscleGroups,
                        prCount = prExerciseIds.size,
                    )
                }.sortedByDescending { it.date }

                val groupedWorkouts = workoutListItems.groupBy { workoutItem ->
                    val instant = Instant.ofEpochMilli(workoutItem.date)
                    val zoned = instant.atZone(ZoneId.systemDefault())
                    zoned.format(monthFormatter)
                }.map { (monthYear, workouts) ->
                    WorkoutGroup(monthYear = monthYear, workouts = workouts)
                }

                _state.value = HistoryListState(
                    isLoading = false,
                    error = null,
                    workouts = workoutListItems,
                    groupedWorkouts = groupedWorkouts,
                )
            } catch (e: Exception) {
                _state.value = HistoryListState(
                    isLoading = false,
                    error = e.message ?: "Failed to load workout history"
                )
            }
        }
    }
}
