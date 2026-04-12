package com.gymbro.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.R
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.WorkoutPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            is OnboardingEvent.SessionDurationSelected -> {
                _state.value = _state.value.copy(sessionDurationMinutes = event.minutes)
            }
            is OnboardingEvent.CompleteOnboarding -> {
                completeOnboarding()
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingPlan = true, planGenerationError = null)

            userPreferences.setWeightUnit(_state.value.selectedUnit)
            val userName = _state.value.userName.takeIf { it.isNotBlank() } ?: ""
            userPreferences.setUserName(userName)
            userPreferences.setTrainingGoal(_state.value.selectedGoal)
            userPreferences.setExperienceLevel(_state.value.selectedExperience)
            userPreferences.setTrainingDaysPerWeek(_state.value.trainingDaysPerWeek)
            userPreferences.setTrainingPhase(_state.value.selectedPhase)
            userPreferences.setSessionDurationMinutes(_state.value.sessionDurationMinutes)
            userPreferences.setOnboardingComplete(true)

            var planGenerated = false
            try {
                // generatePlan internally waits for seed exercises to be available
                // via Flow, so no retry loop is needed.
                val plan = workoutPlanGenerator.generatePlan(
                    goal = _state.value.selectedGoal,
                    experienceLevel = _state.value.selectedExperience,
                    daysPerWeek = _state.value.trainingDaysPerWeek,
                    trainingPhase = _state.value.selectedPhase,
                    sessionDurationMinutes = _state.value.sessionDurationMinutes,
                )
                val personalizedPlan = plan.copy(
                    name = context.getString(R.string.onboarding_first_program_name),
                )
                activePlanStore.setPlanFromOnboarding(personalizedPlan)
                planGenerated = true
            } catch (e: Exception) {
                android.util.Log.e(
                    "OnboardingViewModel",
                    "Plan generation failed",
                    e,
                )
                _state.value = _state.value.copy(
                    planGenerationError = context.getString(R.string.onboarding_plan_generation_error),
                )
                _effects.send(OnboardingEffect.ShowPlanGenerationError)
            }

            _state.value = _state.value.copy(isGeneratingPlan = false)
            _effects.send(
                OnboardingEffect.NavigateToMain(
                    planGenerated = planGenerated,
                    daysPerWeek = _state.value.trainingDaysPerWeek,
                ),
            )
        }
    }
}
