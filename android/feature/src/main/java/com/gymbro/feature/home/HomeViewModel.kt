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
        loadActivePlan()
        loadRecentWorkouts()
        loadDaysSinceLastWorkout()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.QuickStartWorkout -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToActiveWorkout)
                }
            }
            is HomeEvent.CreateFirstProgram -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToPrograms)
                }
            }
            is HomeEvent.StartTodayWorkout -> {
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
        }
    }

    private fun loadActivePlan() {
        val plan = activePlanStore.getPlan()
        if (plan != null) {
            val dayIndex = (java.time.LocalDate.now().dayOfWeek.value - 1) % plan.workoutDays.size
            val todayWorkout = plan.workoutDays.getOrNull(dayIndex)
            _state.update {
                it.copy(
                    activePlan = plan,
                    todayWorkout = todayWorkout,
                )
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

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
