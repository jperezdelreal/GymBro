package com.gymbro.core.service

import com.gymbro.core.model.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store for the currently active workout plan.
 * Shared between ProgramsViewModel (writes) and PlanDayDetailViewModel (reads)
 * so plan data survives navigation between composable destinations.
 */
@Singleton
class ActivePlanStore @Inject constructor() {

    private val _activePlan = MutableStateFlow<WorkoutPlan?>(null)
    val activePlan: StateFlow<WorkoutPlan?> = _activePlan.asStateFlow()

    private val _isFromOnboarding = MutableStateFlow(false)
    val isFromOnboarding: StateFlow<Boolean> = _isFromOnboarding.asStateFlow()

    fun setPlan(plan: WorkoutPlan?) {
        _activePlan.value = plan
    }

    fun setPlanFromOnboarding(plan: WorkoutPlan) {
        _activePlan.value = plan
        _isFromOnboarding.value = true
    }

    fun clearOnboardingFlag() {
        _isFromOnboarding.value = false
    }

    fun getPlan(): WorkoutPlan? = _activePlan.value
}
