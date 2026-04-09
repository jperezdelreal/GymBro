package com.gymbro.feature.onboarding

import app.cash.turbine.test
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
        viewModel = OnboardingViewModel(userPreferences)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.state.value
        assertEquals(0, state.currentPage)
        assertEquals(WeightUnit.KG, state.selectedUnit)
        assertEquals("", state.userName)
        assertNull(state.selectedGoal)
    }

    @Test
    fun `page changed updates state`() = runTest {
        viewModel.onEvent(OnboardingEvent.PageChanged(2))
        
        val state = viewModel.state.value
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `unit selected updates state`() = runTest {
        viewModel.onEvent(OnboardingEvent.UnitSelected(WeightUnit.LBS))
        
        val state = viewModel.state.value
        assertEquals(WeightUnit.LBS, state.selectedUnit)
    }

    @Test
    fun `name changed updates state`() = runTest {
        viewModel.onEvent(OnboardingEvent.NameChanged("John"))
        
        val state = viewModel.state.value
        assertEquals("John", state.userName)
    }

    @Test
    fun `complete onboarding saves preferences and emits navigation effect`() = runTest {
        viewModel.onEvent(OnboardingEvent.UnitSelected(WeightUnit.LBS))
        viewModel.onEvent(OnboardingEvent.NameChanged("John"))
        
        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
            
            val effect = awaitItem()
            assertEquals(OnboardingEffect.NavigateToMain, effect)
            
            coVerify { userPreferences.setWeightUnit(WeightUnit.LBS) }
            coVerify { userPreferences.setUserName("John") }
            coVerify { userPreferences.setOnboardingComplete(true) }
        }
    }
}
