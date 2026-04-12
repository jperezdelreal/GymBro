package com.gymbro.feature.settings

import android.content.Context
import app.cash.turbine.test
import com.gymbro.core.notification.ReminderScheduler
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferences
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        reminderScheduler = mockk(relaxed = true)
        
        every { userPreferences.weightUnit } returns flowOf(WeightUnit.KG)
        every { userPreferences.defaultRestTimer } returns flowOf(90)
        every { userPreferences.autoStartRestTimer } returns flowOf(true)
        every { userPreferences.notificationsEnabled } returns flowOf(false)
        
        viewModel = SettingsViewModel(context, userPreferences, reminderScheduler)
    }

    @Test
    fun `initial state loads preferences`() = runTest {
        val state = viewModel.state.value
        assertEquals(WeightUnit.KG, state.weightUnit)
        assertEquals(90, state.defaultRestTimer)
        assertEquals(true, state.autoStartRestTimer)
        assertEquals(false, state.notificationsEnabled)
    }

    @Test
    fun `set weight unit updates preferences`() = runTest {
        viewModel.onEvent(SettingsEvent.SetWeightUnit(WeightUnit.LBS))
        
        coVerify { userPreferences.setWeightUnit(WeightUnit.LBS) }
    }

    @Test
    fun `set rest timer updates preferences`() = runTest {
        viewModel.onEvent(SettingsEvent.SetDefaultRestTimer(120))
        
        coVerify { userPreferences.setDefaultRestTimer(120) }
    }

    @Test
    fun `enable notifications schedules reminders`() = runTest {
        viewModel.onEvent(SettingsEvent.SetNotifications(true))
        
        coVerify { userPreferences.setNotificationsEnabled(true) }
        coVerify { reminderScheduler.scheduleWorkoutReminders() }
    }

    @Test
    fun `clear all data emits message effect`() = runTest {
        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ClearAllData)
            
            val effect = awaitItem() as SettingsEffect.ShowMessage
            assertEquals("All data cleared", effect.message)
            
            coVerify { userPreferences.clearAllData() }
        }
    }

    @Test
    fun `weight unit LBS from preferences propagates to state`() = runTest {
        // Create a ViewModel whose upstream emits LBS
        val lbsPreferences = mockk<UserPreferences>(relaxed = true)
        every { lbsPreferences.weightUnit } returns flowOf(WeightUnit.LBS)
        every { lbsPreferences.defaultRestTimer } returns flowOf(90)
        every { lbsPreferences.autoStartRestTimer } returns flowOf(true)
        every { lbsPreferences.notificationsEnabled } returns flowOf(false)

        val lbsViewModel = SettingsViewModel(context, lbsPreferences, reminderScheduler)

        lbsViewModel.state.test {
            val state = expectMostRecentItem()
            assertEquals(WeightUnit.LBS, state.weightUnit)
        }
    }
}
