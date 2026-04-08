package com.gymbro.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
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
            is OnboardingEvent.CompleteOnboarding -> {
                completeOnboarding()
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setWeightUnit(_state.value.selectedUnit)
            val userName = _state.value.userName.takeIf { it.isNotBlank() } ?: ""
            userPreferences.setUserName(userName)
            userPreferences.setOnboardingComplete(true)
            _effects.send(OnboardingEffect.NavigateToMain)
        }
    }
}
