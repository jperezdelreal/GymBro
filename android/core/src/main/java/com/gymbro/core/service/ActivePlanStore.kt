package com.gymbro.core.service

import android.util.Log
import com.google.gson.Gson
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store for the currently active workout plan.
 * Persisted to DataStore so the plan survives app restarts (BUG-001 fix).
 * Shared between ProgramsViewModel (writes) and PlanDayDetailViewModel (reads)
 * so plan data survives navigation between composable destinations.
 */
@Singleton
class ActivePlanStore @Inject constructor(
    private val userPreferences: UserPreferences,
    private val gson: Gson,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activePlan = MutableStateFlow<WorkoutPlan?>(null)
    val activePlan: StateFlow<WorkoutPlan?> = _activePlan.asStateFlow()

    private val _isFromOnboarding = MutableStateFlow(false)
    val isFromOnboarding: StateFlow<Boolean> = _isFromOnboarding.asStateFlow()

    private val _pendingWorkoutDay = MutableStateFlow<WorkoutDay?>(null)
    val pendingWorkoutDay: StateFlow<WorkoutDay?> = _pendingWorkoutDay.asStateFlow()

    init {
        scope.launch {
            try {
                val json = userPreferences.activePlanJson.firstOrNull()
                if (json != null) {
                    val plan = gson.fromJson(json, WorkoutPlan::class.java)
                    _activePlan.value = plan
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore active plan from DataStore", e)
            }
        }
    }

    fun setPlan(plan: WorkoutPlan?) {
        _activePlan.value = plan
        persistPlan(plan)
    }

    fun setPlanFromOnboarding(plan: WorkoutPlan) {
        _activePlan.value = plan
        _isFromOnboarding.value = true
        persistPlan(plan)
    }

    fun clearOnboardingFlag() {
        _isFromOnboarding.value = false
    }

    fun getPlan(): WorkoutPlan? = _activePlan.value

    fun setPendingWorkoutDay(day: WorkoutDay) {
        _pendingWorkoutDay.value = day
    }

    fun consumePendingWorkoutDay(): WorkoutDay? {
        val day = _pendingWorkoutDay.value
        _pendingWorkoutDay.value = null
        return day
    }

    private fun persistPlan(plan: WorkoutPlan?) {
        scope.launch {
            try {
                val json = if (plan != null) gson.toJson(plan) else null
                userPreferences.setActivePlanJson(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist active plan to DataStore", e)
            }
        }
    }

    companion object {
        private const val TAG = "ActivePlanStore"
    }
}
