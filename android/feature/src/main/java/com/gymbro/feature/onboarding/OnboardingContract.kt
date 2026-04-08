package com.gymbro.feature.onboarding

import com.gymbro.core.preferences.UserPreferences.WeightUnit

data class OnboardingState(
    val currentPage: Int = 0,
    val selectedUnit: WeightUnit = WeightUnit.KG,
    val userName: String = "",
    val selectedGoal: String = "both",
)

sealed interface OnboardingEvent {
    data class PageChanged(val page: Int) : OnboardingEvent
    data class UnitSelected(val unit: WeightUnit) : OnboardingEvent
    data class NameChanged(val name: String) : OnboardingEvent
    data class GoalSelected(val goal: String) : OnboardingEvent
    data object CompleteOnboarding : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object NavigateToMain : OnboardingEffect
}
