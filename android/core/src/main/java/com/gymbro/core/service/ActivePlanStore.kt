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

    fun setPlan(plan: WorkoutPlan?) {
        _activePlan.value = plan
    }

    fun getPlan(): WorkoutPlan? = _activePlan.value
}
