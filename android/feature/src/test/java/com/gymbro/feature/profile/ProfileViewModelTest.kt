package com.gymbro.feature.profile

import android.content.Context
import app.cash.turbine.test
import com.gymbro.core.auth.AuthService
import com.gymbro.core.auth.AuthState
import com.gymbro.core.auth.GymBroUser
import com.gymbro.core.sync.service.OfflineSyncManager
import com.gymbro.core.sync.service.SyncStatus
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockContext: Context
    private lateinit var mockAuthService: AuthService
    private lateinit var mockSyncManager: OfflineSyncManager
    private lateinit var viewModel: ProfileViewModel

    private lateinit var authStateFlow: MutableStateFlow<AuthState>
    private lateinit var syncStatusFlow: MutableStateFlow<SyncStatus>

    private val testUser = GymBroUser(
        uid = "test-uid-123",
        displayName = "Test User",
        email = "test@example.com",
        isAnonymous = false
    )

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAuthService = mockk(relaxed = true)
        mockSyncManager = mockk(relaxed = true)
        authStateFlow = MutableStateFlow<AuthState>(AuthState.Loading)
        syncStatusFlow = MutableStateFlow(SyncStatus.IDLE)

        every { mockAuthService.observeAuthState() } returns authStateFlow
        every { mockSyncManager.syncStatus } returns syncStatusFlow
    }

    @Test
    fun `initial state shows loading`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.isLoading)
            assertFalse(state.isSignedIn)
            assertNull(state.user)
            assertEquals(SyncStatus.IDLE, state.syncStatus)
        }
    }

    @Test
    fun `auth state signed in updates user state`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1) // Initial loading state

            authStateFlow.value = AuthState.SignedIn(testUser)

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.isSignedIn)
            assertNotNull(state.user)
            assertEquals("test-uid-123", state.user!!.uid)
            assertEquals("Test User", state.user!!.displayName)
        }
    }

    @Test
    fun `auth state signed out clears user state`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            authStateFlow.value = AuthState.SignedIn(testUser)
            skipItems(1)

            authStateFlow.value = AuthState.SignedOut

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertFalse(state.isSignedIn)
            assertNull(state.user)
        }
    }

    @Test
    fun `sign in event calls auth service`() = runTest {
        coEvery { mockAuthService.signInAnonymously() } returns Result.success(testUser)
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.effects.test {
            authStateFlow.value = AuthState.SignedIn(testUser)
            viewModel.onEvent(ProfileEvent.SignIn)

            val effect = awaitItem()
            assertTrue(effect is ProfileEffect.ShowMessage)
            assertTrue((effect as ProfileEffect.ShowMessage).message.contains("Signed in"))
        }
    }

    @Test
    fun `sign in failure shows error effect`() = runTest {
        val error = Exception("Authentication failed")
        coEvery { mockAuthService.signInAnonymously() } returns Result.failure(error)
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SignIn)

            val effect = awaitItem()
            assertTrue(effect is ProfileEffect.ShowError)
            assertTrue((effect as ProfileEffect.ShowError).message.contains("Authentication failed"))
        }
    }

    @Test
    fun `sign out event calls auth service`() = runTest {
        coEvery { mockAuthService.signOut() } returns Result.success(Unit)
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SignOut)

            val effect = awaitItem()
            assertTrue(effect is ProfileEffect.ShowMessage)
            assertTrue((effect as ProfileEffect.ShowMessage).message.contains("Signed out"))
        }
    }

    @Test
    fun `sign out failure shows error effect`() = runTest {
        val error = Exception("Sign-out failed")
        coEvery { mockAuthService.signOut() } returns Result.failure(error)
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SignOut)

            val effect = awaitItem()
            assertTrue(effect is ProfileEffect.ShowError)
            assertTrue((effect as ProfileEffect.ShowError).message.contains("Sign-out failed"))
        }
    }

    @Test
    fun `sync status updates are reflected in state`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            syncStatusFlow.value = SyncStatus.SYNCING

            val syncingState = expectMostRecentItem()
            assertEquals(SyncStatus.SYNCING, syncingState.syncStatus)

            syncStatusFlow.value = SyncStatus.SUCCESS

            val successState = expectMostRecentItem()
            assertEquals(SyncStatus.SUCCESS, successState.syncStatus)
            assertNotNull(successState.lastSyncTime)
        }
    }

    @Test
    fun `sync now event triggers sync manager`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.onEvent(ProfileEvent.SyncNow)

        verify { mockSyncManager.syncNow() }
    }

    @Test
    fun `toggle auto sync updates state`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState.autoSyncEnabled)

            viewModel.onEvent(ProfileEvent.ToggleAutoSync(false))

            val disabledState = expectMostRecentItem()
            assertFalse(disabledState.autoSyncEnabled)

            viewModel.onEvent(ProfileEvent.ToggleAutoSync(true))

            val enabledState = expectMostRecentItem()
            assertTrue(enabledState.autoSyncEnabled)
        }
    }

    @Test
    fun `last sync time is updated on successful sync`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            val initialState = expectMostRecentItem()
            assertNull(initialState.lastSyncTime)

            syncStatusFlow.value = SyncStatus.SUCCESS

            val syncedState = expectMostRecentItem()
            assertNotNull(syncedState.lastSyncTime)
            assertTrue(syncedState.lastSyncTime!! > 0)
        }
    }

    @Test
    fun `last sync time is not updated on sync failure`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            syncStatusFlow.value = SyncStatus.SUCCESS
            val successState = expectMostRecentItem()
            val syncTime = successState.lastSyncTime

            syncStatusFlow.value = SyncStatus.ERROR

            val errorState = expectMostRecentItem()
            assertEquals(syncTime, errorState.lastSyncTime)
        }
    }

    @Test
    fun `anonymous user is handled correctly`() = runTest {
        val anonymousUser = testUser.copy(isAnonymous = true, displayName = null, email = null)
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            authStateFlow.value = AuthState.SignedIn(anonymousUser)

            val state = expectMostRecentItem()
            assertTrue(state.isSignedIn)
            assertNotNull(state.user)
            assertTrue(state.user!!.isAnonymous)
            assertNull(state.user!!.displayName)
            assertNull(state.user!!.email)
        }
    }

    @Test
    fun `sync status idle shows no sync activity`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertEquals(SyncStatus.IDLE, state.syncStatus)
        }
    }

    @Test
    fun `sync status offline is reflected in state`() = runTest {
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            syncStatusFlow.value = SyncStatus.OFFLINE

            val state = expectMostRecentItem()
            assertEquals(SyncStatus.OFFLINE, state.syncStatus)
        }
    }

    @Test
    fun `sign in with loading state shows loading`() = runTest {
        coEvery { mockAuthService.signInAnonymously() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(testUser)
        }
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ProfileEvent.SignIn)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
        }
    }

    @Test
    fun `error state is set on sign in failure`() = runTest {
        val errorMessage = "Network error"
        coEvery { mockAuthService.signInAnonymously() } returns Result.failure(Exception(errorMessage))
        viewModel = ProfileViewModel(mockContext, mockAuthService, mockSyncManager)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ProfileEvent.SignIn)

            // Wait for loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            // Wait for error state
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
        }
    }
}
