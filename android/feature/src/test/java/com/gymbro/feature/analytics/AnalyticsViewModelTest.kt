package com.gymbro.feature.analytics

import app.cash.turbine.test
import com.gymbro.core.service.AnalyticsService
import com.gymbro.core.service.ConsistencyMetrics
import com.gymbro.core.service.WeeklySummary
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AnalyticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var analyticsService: AnalyticsService
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setup() {
        analyticsService = mockk(relaxed = true)
        
        coEvery { analyticsService.getWeeklySummary() } returns WeeklySummary(
            thisWeekVolume = 15000.0,
            lastWeekVolume = 12000.0,
            thisWeekWorkouts = 3,
            lastWeekWorkouts = 2,
            thisWeekPRs = 1,
            volumeChange = 25.0
        )
        coEvery { analyticsService.getWeeklyVolumeData(any()) } returns emptyList()
        coEvery { analyticsService.getMuscleGroupDistribution() } returns emptyList()
        coEvery { analyticsService.getConsistencyMetrics() } returns ConsistencyMetrics(
            currentStreak = 5,
            longestStreak = 10,
            averageWorkoutsPerWeek = 3.5,
            consistencyScore = 85,
            workoutDates = emptyList()
        )
        coEvery { analyticsService.getTopExercises(any()) } returns emptyList()
    }

    @Test
    fun `initial state loads analytics data`() = runTest {
        viewModel = AnalyticsViewModel(analyticsService)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.summary)
        assertEquals(3, state.summary?.thisWeekWorkouts)
        assertEquals(15000.0, state.summary?.thisWeekVolume ?: 0.0, 0.1)
    }

    @Test
    fun `refresh data reloads analytics`() = runTest {
        viewModel = AnalyticsViewModel(analyticsService)
        
        viewModel.onEvent(AnalyticsEvent.RefreshData)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `navigate back emits effect`() = runTest {
        viewModel = AnalyticsViewModel(analyticsService)
        
        viewModel.effects.test {
            viewModel.onEvent(AnalyticsEvent.NavigateBack)
            
            val effect = awaitItem()
            assertEquals(AnalyticsEffect.NavigateBack, effect)
        }
    }

    @Test
    fun `consistency metrics loaded correctly`() = runTest {
        viewModel = AnalyticsViewModel(analyticsService)
        
        val state = viewModel.state.value
        assertNotNull(state.consistency)
        assertEquals(5, state.consistency?.currentStreak)
        assertEquals(10, state.consistency?.longestStreak)
    }
}
