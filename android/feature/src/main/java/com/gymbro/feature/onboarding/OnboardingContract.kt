package com.gymbro.feature.onboarding

import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.core.preferences.UserPreferences.TrainingGoal
import com.gymbro.core.preferences.UserPreferences.ExperienceLevel
import com.gymbro.core.preferences.UserPreferences.TrainingPhase

data class OnboardingState(
    val currentPage: Int = 0,
    val selectedUnit: WeightUnit = WeightUnit.KG,
    val userName: String = "",
    val selectedGoal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val selectedExperience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val trainingDaysPerWeek: Int = 4,
    val selectedPhase: TrainingPhase = TrainingPhase.MAINTENANCE,
    val sessionDurationMinutes: Int = 60,
    val isGeneratingPlan: Boolean = false,
    val planGenerationError: String? = null,
)

sealed interface OnboardingEvent {
    data class PageChanged(val page: Int) : OnboardingEvent
    data class UnitSelected(val unit: WeightUnit) : OnboardingEvent
    data class NameChanged(val name: String) : OnboardingEvent
    data class GoalSelected(val goal: TrainingGoal) : OnboardingEvent
    data class ExperienceSelected(val experience: ExperienceLevel) : OnboardingEvent
    data class TrainingDaysSelected(val days: Int) : OnboardingEvent
    data class TrainingPhaseSelected(val phase: TrainingPhase) : OnboardingEvent
    data class SessionDurationSelected(val minutes: Int) : OnboardingEvent
    data object CompleteOnboarding : OnboardingEvent
}

sealed interface OnboardingEffect {
    data class NavigateToMain(val planGenerated: Boolean, val daysPerWeek: Int) : OnboardingEffect
    data object ShowPlanGenerationError : OnboardingEffect
}
