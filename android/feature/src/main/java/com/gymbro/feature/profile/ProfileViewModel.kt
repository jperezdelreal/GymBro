package com.gymbro.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.R
import com.gymbro.core.auth.AuthService
import com.gymbro.core.auth.AuthState
import com.gymbro.core.sync.service.OfflineSyncManager
import com.gymbro.core.sync.service.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: AuthService,
    private val syncManager: OfflineSyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeAuth()
        observeSyncStatus()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.SignIn -> signIn()
            is ProfileEvent.SignOut -> signOut()
            is ProfileEvent.SyncNow -> syncNow()
            is ProfileEvent.ToggleAutoSync -> toggleAutoSync(event.enabled)
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authService.observeAuthState().collect { authState ->
                _state.update {
                    when (authState) {
                        is AuthState.Loading -> it.copy(isLoading = true)
                        is AuthState.SignedIn -> it.copy(
                            user = authState.user,
                            isSignedIn = true,
                            isLoading = false,
                        )
                        is AuthState.SignedOut -> it.copy(
                            user = null,
                            isSignedIn = false,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncManager.syncStatus.collect { status ->
                _state.update {
                    it.copy(
                        syncStatus = status,
                        lastSyncTime = if (status == SyncStatus.SUCCESS) {
                            System.currentTimeMillis()
                        } else {
                            it.lastSyncTime
                        },
                    )
                }
            }
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            authService.signInAnonymously()
                .onSuccess {
                    _effects.send(ProfileEffect.ShowMessage(context.getString(R.string.profile_signed_in_success)))
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                    _effects.send(ProfileEffect.ShowError(error.message ?: context.getString(R.string.profile_sign_in_failed)))
                }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authService.signOut()
                .onSuccess {
                    _effects.send(ProfileEffect.ShowMessage(context.getString(R.string.profile_signed_out_success)))
                }
                .onFailure { error ->
                    _effects.send(ProfileEffect.ShowError(error.message ?: context.getString(R.string.profile_sign_out_failed)))
                }
        }
    }

    private fun syncNow() {
        syncManager.syncNow()
    }

    private fun toggleAutoSync(enabled: Boolean) {
        _state.update { it.copy(autoSyncEnabled = enabled) }
    }
}
