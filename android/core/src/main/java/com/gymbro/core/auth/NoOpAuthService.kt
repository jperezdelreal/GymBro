package com.gymbro.core.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpAuthService @Inject constructor() : AuthService {

    override suspend fun signInAnonymously(): Result<GymBroUser> =
        Result.failure(Exception("Firebase is not configured. Add google-services.json to enable authentication."))

    override suspend fun signOut(): Result<Unit> =
        Result.failure(Exception("Firebase is not configured."))

    override fun getCurrentUser(): GymBroUser? = null

    override fun observeAuthState(): Flow<AuthState> = flowOf(AuthState.SignedOut)
}
