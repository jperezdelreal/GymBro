package com.gymbro.core.auth

import kotlinx.coroutines.flow.Flow

/** Minimal user representation for the auth layer. */
data class GymBroUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isAnonymous: Boolean,
)

/** Auth state for reactive observation. */
sealed interface AuthState {
    data object Loading : AuthState
    data class SignedIn(val user: GymBroUser) : AuthState
    data object SignedOut : AuthState
}

/**
 * Authentication contract. MVP uses anonymous auth;
 * email/Google sign-in can be added later without changing callers.
 */
interface AuthService {
    suspend fun signInAnonymously(): Result<GymBroUser>
    suspend fun signOut(): Result<Unit>
    fun getCurrentUser(): GymBroUser?
    fun observeAuthState(): Flow<AuthState>
}
