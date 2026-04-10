package com.gymbro.feature.programs

import androidx.lifecycle.ViewModel
import com.gymbro.core.service.ActivePlanStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlanDayDetailViewModel @Inject constructor(
    private val activePlanStore: ActivePlanStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanDayDetailState())
    val state: StateFlow<PlanDayDetailState> = _state.asStateFlow()

    private var currentDayNumber: Int = -1

    fun onIntent(intent: PlanDayDetailIntent) {
        when (intent) {
            is PlanDayDetailIntent.LoadDay -> loadDay(intent.dayNumber)
            is PlanDayDetailIntent.Retry -> {
                if (currentDayNumber > 0) loadDay(currentDayNumber)
            }
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

        _state.value = PlanDayDetailState(
            isLoading = false,
            error = null,
            workoutDay = workoutDay,
            planName = plan.name,
        )
    }
}
