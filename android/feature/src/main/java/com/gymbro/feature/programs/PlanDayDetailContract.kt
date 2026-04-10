package com.gymbro.feature.programs

import com.gymbro.core.model.WorkoutDay

data class PlanDayDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workoutDay: WorkoutDay? = null,
    val planName: String = "",
)

sealed interface PlanDayDetailIntent {
    data class LoadDay(val dayNumber: Int) : PlanDayDetailIntent
    data object Retry : PlanDayDetailIntent
}
