package com.gymbro.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.WorkoutPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

private const val PLAN_GENERATION_MAX_RETRIES = 5
private const val PLAN_GENERATION_RETRY_DELAY_MS = 500L

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val workoutPlanGenerator: WorkoutPlanGenerator,
    private val activePlanStore: ActivePlanStore,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.PageChanged -> {
                _state.value = _state.value.copy(currentPage = event.page)
            }
            is OnboardingEvent.UnitSelected -> {
                _state.value = _state.value.copy(selectedUnit = event.unit)
            }
            is OnboardingEvent.NameChanged -> {
                _state.value = _state.value.copy(userName = event.name)
            }
            is OnboardingEvent.GoalSelected -> {
                _state.value = _state.value.copy(selectedGoal = event.goal)
            }
            is OnboardingEvent.ExperienceSelected -> {
                _state.value = _state.value.copy(selectedExperience = event.experience)
            }
            is OnboardingEvent.TrainingDaysSelected -> {
                _state.value = _state.value.copy(trainingDaysPerWeek = event.days)
            }
            is OnboardingEvent.TrainingPhaseSelected -> {
                _state.value = _state.value.copy(selectedPhase = event.phase)
            }
            is OnboardingEvent.CompleteOnboarding -> {
                completeOnboarding()
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingPlan = true)

            userPreferences.setWeightUnit(_state.value.selectedUnit)
            val userName = _state.value.userName.takeIf { it.isNotBlank() } ?: ""
            userPreferences.setUserName(userName)
            userPreferences.setTrainingGoal(_state.value.selectedGoal)
            userPreferences.setExperienceLevel(_state.value.selectedExperience)
            userPreferences.setTrainingDaysPerWeek(_state.value.trainingDaysPerWeek)
            userPreferences.setTrainingPhase(_state.value.selectedPhase)
            userPreferences.setOnboardingComplete(true)

            // Retry plan generation — exercise seed data loads asynchronously
            // on first install and may not be available immediately.
            var lastException: Exception? = null
            for (attempt in 1..PLAN_GENERATION_MAX_RETRIES) {
                try {
                    val plan = workoutPlanGenerator.generatePlan(
                        goal = _state.value.selectedGoal,
                        experienceLevel = _state.value.selectedExperience,
                        daysPerWeek = _state.value.trainingDaysPerWeek,
                        trainingPhase = _state.value.selectedPhase,
                    )
                    val personalizedPlan = plan.copy(
                        name = "Your First Program",
                    )
                    activePlanStore.setPlanFromOnboarding(personalizedPlan)
                    lastException = null
                    break
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < PLAN_GENERATION_MAX_RETRIES) {
                        delay(PLAN_GENERATION_RETRY_DELAY_MS)
                    }
                }
            }
            if (lastException != null) {
                android.util.Log.e(
                    "OnboardingViewModel",
                    "Plan generation failed after $PLAN_GENERATION_MAX_RETRIES attempts",
                    lastException,
                )
            }

            _state.value = _state.value.copy(isGeneratingPlan = false)
            _effects.send(OnboardingEffect.NavigateToMain)
        }
    }
}
