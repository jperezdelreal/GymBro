package com.gymbro.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.PlateauDetectionService
import com.gymbro.feature.common.TooltipManager
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
    val tooltipManager: TooltipManager,
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
            is ProgressEvent.SelectTimePeriod -> selectTimePeriod(event.period)
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
            is ProgressEvent.GetCoachingAdvice -> {
                viewModelScope.launch {
                    val prompt = "I've plateaued on ${event.exerciseName} for ${event.weeksDuration} weeks. What should I do?"
                    _effects.send(ProgressEffect.NavigateToCoach(prompt))
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
            ).sortedWith(
                compareBy<com.gymbro.core.model.PlateauAlert> { 
                    when (it.severity) {
                        com.gymbro.core.model.PlateauSeverity.SEVERE -> 0
                        com.gymbro.core.model.PlateauSeverity.MODERATE -> 1
                        com.gymbro.core.model.PlateauSeverity.MILD -> 2
                    }
                }.thenByDescending { it.weeksDuration }
            )

            // Calculate KPI metrics
            val now = java.time.Instant.now()
            val (periodStart, periodEnd) = when (_state.value.selectedTimePeriod) {
                TimePeriod.THIS_WEEK -> {
                    val weekStart = now.minusSeconds(7 * 24 * 60 * 60)
                    weekStart to now
                }
                TimePeriod.LAST_WEEK -> {
                    val lastWeekEnd = now.minusSeconds(7 * 24 * 60 * 60)
                    val lastWeekStart = lastWeekEnd.minusSeconds(7 * 24 * 60 * 60)
                    lastWeekStart to lastWeekEnd
                }
                TimePeriod.THIS_MONTH -> {
                    val monthStart = now.minusSeconds(30 * 24 * 60 * 60)
                    monthStart to now
                }
            }

            val periodWorkouts = history.filter { it.date.isAfter(periodStart) && !it.date.isAfter(periodEnd) }
            val totalVolume = periodWorkouts.sumOf { it.totalVolume }
            val workoutsCount = periodWorkouts.size
            
            // Calculate % change vs previous period
            val previousPeriodStart = periodStart.minusSeconds(periodEnd.epochSecond - periodStart.epochSecond)
            val previousPeriodWorkouts = history.filter { 
                it.date.isAfter(previousPeriodStart) && !it.date.isAfter(periodStart)
            }
            val previousVolume = previousPeriodWorkouts.sumOf { it.totalVolume }
            val volumeChangePercent = if (previousVolume > 0) {
                ((totalVolume - previousVolume) / previousVolume * 100)
            } else null

            // Get recent PRs with details
            val twoWeeksAgo = now.minusSeconds(14 * 24 * 60 * 60)
            val recentPRsWithDetails = allRecords.filter { it.date.isAfter(twoWeeksAgo) }
                .sortedByDescending { it.date }
            val recentPRs = recentPRsWithDetails.size

            // Calculate top 5 exercises in period by set count
            val topExercises = periodWorkouts
                .flatMap { workout ->
                    workout.exerciseNames
                }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .map { TopExercise(exerciseName = it.key, setCount = it.value) }

            // Calculate weekly volume data for last 8 weeks
            val weeklyVolumeData = calculateWeeklyVolume(history)

            _state.update {
                it.copy(
                    workoutHistory = history,
                    personalRecords = allRecords,
                    exerciseOptions = exerciseOptions,
                    selectedExerciseId = selectedId,
                    chartData = chartData,
                    plateauAlerts = plateauAlerts,
                    totalVolume = totalVolume,
                    workoutsThisWeek = workoutsCount,
                    recentPRs = recentPRs,
                    weeklyVolumeData = weeklyVolumeData,
                    volumeChangePercent = volumeChangePercent,
                    topExercises = topExercises,
                    recentPRsWithDetails = recentPRsWithDetails,
                    isLoading = false,
                )
            }
        }
    }

    private fun selectTimePeriod(period: TimePeriod) {
        _state.update { it.copy(selectedTimePeriod = period) }
        loadData()
    }

    private fun calculateWeeklyVolume(history: List<com.gymbro.core.model.WorkoutHistoryItem>): List<WeeklyVolume> {
        val now = java.time.Instant.now()
        val eightWeeksAgo = now.minusSeconds(8 * 7 * 24 * 60 * 60)
        
        val recentHistory = history.filter { it.date.isAfter(eightWeeksAgo) }
        
        val weeklyData = mutableMapOf<Int, Double>()
        for (i in 0 until 8) {
            weeklyData[i] = 0.0
        }
        
        recentHistory.forEach { workout ->
            val weekNumber = ((now.epochSecond - workout.date.epochSecond) / (7 * 24 * 60 * 60)).toInt()
            if (weekNumber in 0 until 8) {
                weeklyData[weekNumber] = (weeklyData[weekNumber] ?: 0.0) + workout.totalVolume
            }
        }
        
        return (0 until 8).map { weekNum ->
            WeeklyVolume(weekNumber = 7 - weekNum, volume = weeklyData[weekNum] ?: 0.0)
        }.reversed()
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
