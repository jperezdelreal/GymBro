package com.gymbro.feature.home

import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.PersonalRecordService
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
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.QuickStartWorkout -> {
                val plan = activePlanStore.getPlan()
                if (plan == null) {
                    _state.update { it.copy(showNoPlanDialog = true) }
                } else {
                    val dayIndex = (java.time.LocalDate.now().dayOfWeek.value - 1) % plan.workoutDays.size
                    val todayWorkout = plan.workoutDays.getOrNull(dayIndex)
                    if (todayWorkout != null) {
                        activePlanStore.setPendingWorkoutDay(todayWorkout)
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
        }
    }

    private fun collectActivePlan() {
        viewModelScope.launch {
            activePlanStore.activePlan.collect { plan ->
                if (plan != null) {
                    val dayIndex = (java.time.LocalDate.now().dayOfWeek.value - 1) % plan.workoutDays.size
                    val todayWorkout = plan.workoutDays.getOrNull(dayIndex)
                    _state.update {
                        it.copy(
                            activePlan = plan,
                            todayWorkout = todayWorkout,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            activePlan = null,
                            todayWorkout = null,
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
                _state.update { it.copy(workoutStreak = 0) }
                return@safeLaunch
            }

            val sortedByDate = historyItems.sortedByDescending { it.date }
            var streak = 0
            var currentDate = java.time.LocalDate.now()

            for (item in sortedByDate) {
                val workoutDate = item.date.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(workoutDate, currentDate)

                if (daysDiff == 0L || (daysDiff == 1L && streak > 0)) {
                    if (daysDiff == 1L) {
                        streak++
                        currentDate = workoutDate
                    } else if (daysDiff == 0L && streak == 0) {
                        streak = 1
                    }
                } else {
                    break
                }
            }

            _state.update { it.copy(workoutStreak = streak) }
        }
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

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
