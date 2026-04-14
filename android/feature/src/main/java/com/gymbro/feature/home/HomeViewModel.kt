package com.gymbro.feature.home

import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.PlateauDetectionService
import com.gymbro.feature.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val activePlanStore: ActivePlanStore,
    private val personalRecordService: PersonalRecordService,
    private val plateauDetectionService: PlateauDetectionService,
    private val exerciseRepository: ExerciseRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        collectActivePlan()
        loadRecentWorkouts()
        loadDaysSinceLastWorkout()
        loadWorkoutStreak()
        loadRecentPR()
        loadPlateauAlerts()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.QuickStartWorkout -> {
                val plan = activePlanStore.getPlan()
                if (plan == null) {
                    _state.update { it.copy(showNoPlanDialog = true) }
                } else {
                    val currentWorkout = _state.value.todayWorkout
                    if (currentWorkout != null) {
                        activePlanStore.setPendingWorkoutDay(currentWorkout)
                    }
                    viewModelScope.launch {
                        _effect.send(HomeEffect.NavigateToActiveWorkout)
                    }
                }
            }
            is HomeEvent.CreateFirstProgram -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToPrograms)
                }
            }
            is HomeEvent.StartTodayWorkout -> {
                val todayWorkout = _state.value.todayWorkout
                if (todayWorkout != null) {
                    activePlanStore.setPendingWorkoutDay(todayWorkout)
                }
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToActiveWorkout)
                }
            }
            is HomeEvent.ViewWorkoutDetail -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToWorkoutDetail(event.workoutId))
                }
            }
            is HomeEvent.ViewAllPrograms -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToPrograms)
                }
            }
            is HomeEvent.DismissNoPlanDialog -> {
                _state.update { it.copy(showNoPlanDialog = false) }
            }
            is HomeEvent.NoPlanGoToPrograms -> {
                _state.update { it.copy(showNoPlanDialog = false) }
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToPrograms)
                }
            }
            is HomeEvent.DismissPRBanner -> {
                _state.update { it.copy(showPRCelebration = false) }
            }
            is HomeEvent.DismissMilestoneBanner -> {
                _state.update { it.copy(showMilestoneCelebration = false) }
            }
            is HomeEvent.DismissPlateauAlert -> {
                _state.update { currentState ->
                    currentState.copy(
                        plateauAlerts = currentState.plateauAlerts.filter { 
                            it.exerciseId != event.exerciseId 
                        }
                    )
                }
            }
            is HomeEvent.OpenCoachForPlateau -> {
                val context = when (event.alert.type) {
                    com.gymbro.core.model.PlateauType.STAGNATION -> {
                        "I've hit a plateau on ${event.alert.exerciseName}. No progress in ${event.alert.weeksDuration} weeks. ${event.alert.suggestion}"
                    }
                    com.gymbro.core.model.PlateauType.REGRESSION -> {
                        "I'm regressing on ${event.alert.exerciseName}. Performance declining for ${event.alert.weeksDuration} weeks. ${event.alert.suggestion}"
                    }
                }
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToCoachWithContext(context))
                }
            }
            is HomeEvent.SwapDay -> {
                val plan = _state.value.activePlan ?: return
                if (plan.workoutDays.size <= 1) return
                val defaultIndex = (java.time.LocalDate.now().dayOfWeek.value - 1) % plan.workoutDays.size
                val currentIndex = _state.value.selectedDayIndex ?: defaultIndex
                val nextIndex = (currentIndex + 1) % plan.workoutDays.size
                val nextWorkout = plan.workoutDays[nextIndex]
                _state.update {
                    it.copy(
                        selectedDayIndex = nextIndex,
                        todayWorkout = nextWorkout,
                    )
                }
            }
        }
    }

    private fun collectActivePlan() {
        viewModelScope.launch {
            activePlanStore.activePlan.collect { plan ->
                if (plan != null) {
                    val override = _state.value.selectedDayIndex
                    val dayIndex = if (override != null && override < plan.workoutDays.size) {
                        override
                    } else {
                        (java.time.LocalDate.now().dayOfWeek.value - 1) % plan.workoutDays.size
                    }
                    val todayWorkout = plan.workoutDays.getOrNull(dayIndex)
                    _state.update {
                        it.copy(
                            activePlan = plan,
                            todayWorkout = todayWorkout,
                            selectedDayIndex = if (override != null && override >= plan.workoutDays.size) null else override,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            activePlan = null,
                            todayWorkout = null,
                            selectedDayIndex = null,
                        )
                    }
                }
            }
        }
    }

    private fun loadRecentWorkouts() {
        safeLaunch(
            onError = { error ->
                _state.update { it.copy(isLoading = false) }
                handleError(error) { loadRecentWorkouts() }
            }
        ) {
            _state.update { it.copy(isLoading = true) }

            val historyItems = personalRecordService.getWorkoutHistory()

            val recentItems = historyItems
                .sortedByDescending { it.date }
                .take(3)
                .map { historyItem ->
                    val workout = workoutRepository.getWorkout(historyItem.workoutId)
                    val sets = workout?.sets?.filter { !it.isWarmup } ?: emptyList()

                    RecentWorkoutItem(
                        workoutId = historyItem.workoutId,
                        date = historyItem.date.toEpochMilli(),
                        durationSeconds = historyItem.durationSeconds,
                        exerciseCount = historyItem.exerciseCount,
                        totalSets = sets.size,
                        totalVolume = historyItem.totalVolume,
                    )
                }

            _state.update {
                it.copy(
                    recentWorkouts = recentItems,
                    isLoading = false,
                )
            }
        }
    }

    private fun loadDaysSinceLastWorkout() {
        safeLaunch {
            val days = workoutRepository.getDaysSinceLastWorkout()
            _state.update { it.copy(daysSinceLastWorkout = days) }
        }
    }

    private fun loadWorkoutStreak() {
        safeLaunch {
            val historyItems = personalRecordService.getWorkoutHistory()
            if (historyItems.isEmpty()) {
                _state.update { it.copy(
                    workoutStreak = 0,
                    weeklyStreak = 0,
                    totalWorkouts = 0,
                    nextMilestone = 7
                ) }
                return@safeLaunch
            }

            val totalWorkouts = historyItems.size
            
            val sortedByDate = historyItems.sortedByDescending { it.date }
            var dayStreak = 0
            var currentDate = java.time.LocalDate.now()

            for (item in sortedByDate) {
                val workoutDate = item.date.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(workoutDate, currentDate)

                if (daysDiff == 0L || (daysDiff == 1L && dayStreak > 0)) {
                    if (daysDiff == 1L) {
                        dayStreak++
                        currentDate = workoutDate
                    } else if (daysDiff == 0L && dayStreak == 0) {
                        dayStreak = 1
                    }
                } else {
                    break
                }
            }

            val weeklyStreak = calculateWeeklyStreak(historyItems)
            
            val milestones = listOf(7, 30, 100, 365)
            val nextMilestone = milestones.firstOrNull { it > totalWorkouts }
            
            val lastMilestone = milestones.lastOrNull { it <= totalWorkouts }
            val showMilestoneCelebration = lastMilestone != null && historyItems.size >= 2
            val milestoneCelebration = if (lastMilestone != null && showMilestoneCelebration) {
                when (lastMilestone) {
                    7 -> MilestoneCelebration(7, "🎉", "milestone_7_title", "milestone_7_message")
                    30 -> MilestoneCelebration(30, "🔥", "milestone_30_title", "milestone_30_message")
                    100 -> MilestoneCelebration(100, "💯", "milestone_100_title", "milestone_100_message")
                    365 -> MilestoneCelebration(365, "👑", "milestone_365_title", "milestone_365_message")
                    else -> null
                }
            } else {
                null
            }

            _state.update { it.copy(
                workoutStreak = dayStreak,
                weeklyStreak = weeklyStreak,
                totalWorkouts = totalWorkouts,
                nextMilestone = nextMilestone,
                showMilestoneCelebration = milestoneCelebration != null,
                milestoneCelebration = milestoneCelebration
            ) }
        }
    }
    
    private fun calculateWeeklyStreak(historyItems: List<com.gymbro.core.model.WorkoutHistoryItem>): Int {
        if (historyItems.isEmpty()) return 0
        
        val workoutsByWeek = historyItems.groupBy { item ->
            val date = item.date.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val weekStart = date.minusDays(date.dayOfWeek.value.toLong() - 1)
            weekStart
        }
        
        val currentWeekStart = java.time.LocalDate.now().let { date ->
            date.minusDays(date.dayOfWeek.value.toLong() - 1)
        }
        
        var streak = 0
        var checkWeek = currentWeekStart
        
        while (true) {
            val workoutsThisWeek = workoutsByWeek[checkWeek]?.size ?: 0
            
            if (workoutsThisWeek > 0) {
                streak++
                checkWeek = checkWeek.minusWeeks(1)
            } else {
                break
            }
        }
        
        return streak
    }

    private fun loadRecentPR() {
        safeLaunch {
            val historyItems = personalRecordService.getWorkoutHistory()
            if (historyItems.isEmpty()) return@safeLaunch

            val exerciseIds = personalRecordService.getExerciseIdsWithHistory()
            if (exerciseIds.isEmpty()) return@safeLaunch

            val now = java.time.Instant.now()
            val last24Hours = now.minusSeconds(86400)

            for (exerciseId in exerciseIds) {
                val records = personalRecordService.getPersonalRecords(exerciseId, "")
                val recentPR = records.firstOrNull { record ->
                    record.date.isAfter(last24Hours) && record.previousValue != null
                }

                if (recentPR != null) {
                    _state.update {
                        it.copy(
                            recentPR = recentPR,
                            showPRCelebration = true,
                        )
                    }
                    break
                }
            }
        }
    }

    private fun loadPlateauAlerts() {
        safeLaunch {
            val exerciseIds = personalRecordService.getExerciseIdsWithHistory()
            if (exerciseIds.isEmpty()) return@safeLaunch

            val exercisePairs = exerciseIds.mapNotNull { id ->
                val exercise = exerciseRepository.getExerciseById(id)
                exercise?.let { id to it.name }
            }

            val alerts = plateauDetectionService.detectAllPlateaus(exercisePairs)
            
            _state.update { 
                it.copy(plateauAlerts = alerts.take(3))
            }
        }
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
