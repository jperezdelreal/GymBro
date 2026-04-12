package com.gymbro.feature.home

import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.PlateauAlert

data class HomeState(
    val activePlan: WorkoutPlan? = null,
    val todayWorkout: WorkoutDay? = null,
    val recentWorkouts: List<RecentWorkoutItem> = emptyList(),
    val isLoading: Boolean = true,
    val isGeneratingPlan: Boolean = false,
    val daysSinceLastWorkout: Int? = null,
    val showNoPlanDialog: Boolean = false,
    val workoutStreak: Int = 0,
    val recentPR: PersonalRecord? = null,
    val showPRCelebration: Boolean = false,
    val plateauAlerts: List<PlateauAlert> = emptyList(),
)

data class RecentWorkoutItem(
    val workoutId: String,
    val date: Long,
    val durationSeconds: Long,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalVolume: Double,
)

sealed interface HomeEvent {
    data object QuickStartWorkout : HomeEvent
    data object CreateFirstProgram : HomeEvent
    data class StartTodayWorkout(val dayNumber: Int) : HomeEvent
    data class ViewWorkoutDetail(val workoutId: String) : HomeEvent
    data object ViewAllPrograms : HomeEvent
    data object DismissNoPlanDialog : HomeEvent
    data object NoPlanGoToPrograms : HomeEvent
    data object DismissPRBanner : HomeEvent
    data class DismissPlateauAlert(val exerciseId: String) : HomeEvent
    data class OpenCoachForPlateau(val alert: PlateauAlert) : HomeEvent
}

sealed interface HomeEffect {
    data object NavigateToActiveWorkout : HomeEffect
    data object NavigateToPrograms : HomeEffect
    data class NavigateToWorkoutDetail(val workoutId: String) : HomeEffect
    data class NavigateToCoachWithContext(val context: String) : HomeEffect
}
