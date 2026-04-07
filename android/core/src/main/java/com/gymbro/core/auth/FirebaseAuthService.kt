package com.gymbro.core.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthService {

    override suspend fun signInAnonymously(): Result<GymBroUser> = runCatching {
        val result = firebaseAuth.signInAnonymously().await()
        result.user?.toGymBroUser()
            ?: throw IllegalStateException("Anonymous sign-in returned null user")
    }.onFailure {
        Log.e(TAG, "Anonymous sign-in failed", it)
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): GymBroUser? =
        firebaseAuth.currentUser?.toGymBroUser()

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val state = if (user != null) {
                AuthState.SignedIn(user.toGymBroUser())
            } else {
                AuthState.SignedOut
            }
            trySend(state)
        }

        firebaseAuth.addAuthStateListener(listener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthService"
    }
}

private fun FirebaseUser.toGymBroUser(): GymBroUser =
    GymBroUser(
        uid = uid,
        displayName = displayName,
        email = email,
        isAnonymous = isAnonymous,
    )
