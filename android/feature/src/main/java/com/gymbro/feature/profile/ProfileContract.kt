package com.gymbro.feature.profile

import com.gymbro.core.auth.GymBroUser
import com.gymbro.core.sync.service.SyncStatus

data class ProfileState(
    val user: GymBroUser? = null,
    val isSignedIn: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long? = null,
    val autoSyncEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface ProfileEvent {
    data object SignIn : ProfileEvent
    data object SignOut : ProfileEvent
    data object SyncNow : ProfileEvent
    data class ToggleAutoSync(val enabled: Boolean) : ProfileEvent
}

sealed interface ProfileEffect {
    data class ShowError(val message: String) : ProfileEffect
    data class ShowMessage(val message: String) : ProfileEffect
}
