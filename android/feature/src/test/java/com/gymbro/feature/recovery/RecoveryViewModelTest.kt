package com.gymbro.feature.recovery

import app.cash.turbine.test
import com.gymbro.core.TestFixtures
import com.gymbro.core.health.HealthConnectRepository
import com.gymbro.core.model.SleepData
import com.gymbro.core.model.SleepQuality
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class RecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockHealthConnectRepository: HealthConnectRepository
    private lateinit var viewModel: RecoveryViewModel

    @Before
    fun setup() {
        mockHealthConnectRepository = mockk(relaxed = true)
    }

    @Test
    fun `initial state when health connect available and permissions granted loads data`() = runTest {
        val sleepHistory = listOf(
            SleepData(
                durationMinutes = 510.0, // 8.5 hours
                quality = SleepQuality.EXCELLENT,
                startTime = Instant.now().minusSeconds(86400 + 30600),
                endTime = Instant.now().minusSeconds(86400)
            )
        )

        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns sleepHistory

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.healthConnectAvailable)
            assertTrue(state.permissionsGranted)
            assertFalse(state.isLoading)
            assertNotNull(state.recoveryMetrics)
            assertEquals(8.5, state.recoveryMetrics.sleepHours, 0.01)
            assertEquals(1, state.sleepHistory.size)
            assertNull(state.error)
        }
    }

    @Test
    fun `initial state when health connect not available`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns false

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertFalse(state.healthConnectAvailable)
            assertFalse(state.permissionsGranted)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `initial state when health connect available but permissions not granted`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns false

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.healthConnectAvailable)
            assertFalse(state.permissionsGranted)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `request permissions sends effect`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns false

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.effects.test {
            viewModel.onEvent(RecoveryEvent.RequestPermissions)

            val effect = awaitItem()
            assertTrue(effect is RecoveryEffect.LaunchPermissionRequest)
        }
    }

    @Test
    fun `permissions result with granted loads data`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns false
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onPermissionsResult(true)

            // Skip the permissions granted state
            skipItems(1)

            // Wait for the loaded data state
            val state = awaitItem()
            assertTrue(state.permissionsGranted)
            assertEquals(8.5, state.recoveryMetrics.sleepHours, 0.01)
        }
    }

    @Test
    fun `permissions result with denied does not load data`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns false

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onPermissionsResult(false)

            val state = awaitItem()
            assertFalse(state.permissionsGranted)
            assertEquals(0.0, state.recoveryMetrics.sleepHours, 0.01)
        }
    }

    @Test
    fun `refresh data event reloads recovery metrics`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery andThen TestFixtures.poorRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val initialState = expectMostRecentItem()
            assertEquals(8.5, initialState.recoveryMetrics.sleepHours, 0.01)

            viewModel.onEvent(RecoveryEvent.RefreshData)

            // Skip loading state
            skipItems(1)

            val refreshedState = awaitItem()
            assertEquals(5.0, refreshedState.recoveryMetrics.sleepHours, 0.01)
        }
    }

    @Test
    fun `error during data load sets error state`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } throws Exception("Network error")

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Network error"))
        }
    }

    @Test
    fun `readiness score is calculated correctly for good recovery`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.recoveryMetrics.readinessScore > 70.0)
        }
    }

    @Test
    fun `readiness score is calculated correctly for poor recovery`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.poorRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.recoveryMetrics.readinessScore < 50.0)
        }
    }

    @Test
    fun `unknown recovery metrics handles null values`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.unknownRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertEquals(7.0, state.recoveryMetrics.sleepHours, 0.01)
            assertNull(state.recoveryMetrics.hrv)
            assertNull(state.recoveryMetrics.restingHR)
            assertNull(state.recoveryMetrics.daysSinceLastWorkout)
        }
    }

    @Test
    fun `sleep history loads correctly`() = runTest {
        val now = Instant.now()
        val sleepHistory = listOf(
            SleepData(
                durationMinutes = 480.0, // 8 hours
                quality = SleepQuality.GOOD,
                startTime = now.minusSeconds(86400 + 28800),
                endTime = now.minusSeconds(86400)
            ),
            SleepData(
                durationMinutes = 450.0, // 7.5 hours
                quality = SleepQuality.GOOD,
                startTime = now.minusSeconds(172800 + 27000),
                endTime = now.minusSeconds(172800)
            ),
            SleepData(
                durationMinutes = 540.0, // 9 hours
                quality = SleepQuality.EXCELLENT,
                startTime = now.minusSeconds(259200 + 32400),
                endTime = now.minusSeconds(259200)
            )
        )

        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns sleepHistory

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertEquals(3, state.sleepHistory.size)
            assertEquals(8.0, state.sleepHistory[0].durationHours, 0.01)
            assertEquals(7.5, state.sleepHistory[1].durationHours, 0.01)
            assertEquals(9.0, state.sleepHistory[2].durationHours, 0.01)
        }
    }

    @Test
    fun `empty sleep history is handled`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.sleepHistory.isEmpty())
        }
    }

    @Test
    fun `loading state is cleared after data load completes`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } returns TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `error is cleared on successful refresh`() = runTest {
        coEvery { mockHealthConnectRepository.isAvailable() } returns true
        coEvery { mockHealthConnectRepository.hasPermissions() } returns true
        coEvery { mockHealthConnectRepository.getRecoveryMetrics() } throws Exception("Error") andThen TestFixtures.goodRecovery
        coEvery { mockHealthConnectRepository.getSleepHistory(7) } returns emptyList()

        viewModel = RecoveryViewModel(mockHealthConnectRepository)

        viewModel.state.test {
            val errorState = expectMostRecentItem()
            assertNotNull(errorState.error)

            viewModel.onEvent(RecoveryEvent.RefreshData)

            // Skip loading state
            skipItems(1)

            val successState = awaitItem()
            assertNull(successState.error)
            assertEquals(8.5, successState.recoveryMetrics.sleepHours, 0.01)
        }
    }
}
