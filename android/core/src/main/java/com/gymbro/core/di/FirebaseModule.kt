package com.gymbro.core.di

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.gymbro.core.auth.AuthService
import com.gymbro.core.auth.FirebaseAuthService
import com.gymbro.core.auth.NoOpAuthService
import com.gymbro.core.sync.service.CloudSyncService
import com.gymbro.core.sync.service.FirestoreSyncService
import com.gymbro.core.sync.service.NoOpCloudSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    private const val TAG = "FirebaseModule"

    private fun isFirebaseInitialized(): Boolean = try {
        FirebaseApp.getInstance()
        true
    } catch (e: IllegalStateException) {
        Log.w(TAG, "Firebase not initialized - running in offline mode")
        false
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth? = try {
        if (isFirebaseInitialized()) {
            Firebase.auth
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get FirebaseAuth", e)
        null
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore? = try {
        if (isFirebaseInitialized()) {
            Firebase.firestore
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get FirebaseFirestore", e)
        null
    }

    @Provides
    @Singleton
    fun provideAuthService(
        firebaseAuth: FirebaseAuth?,
        firebaseAuthService: FirebaseAuthService,
        noOpAuthService: NoOpAuthService,
    ): AuthService = if (firebaseAuth != null) {
        firebaseAuthService
    } else {
        Log.w(TAG, "Using NoOpAuthService - Firebase not configured")
        noOpAuthService
    }

    @Provides
    @Singleton
    fun provideCloudSyncService(
        firebaseAuth: FirebaseAuth?,
        firebaseFirestore: FirebaseFirestore?,
        firestoreSyncService: FirestoreSyncService,
        noOpCloudSyncService: NoOpCloudSyncService,
    ): CloudSyncService = if (firebaseAuth != null && firebaseFirestore != null) {
        firestoreSyncService
    } else {
        Log.w(TAG, "Using NoOpCloudSyncService - Firebase not configured")
        noOpCloudSyncService
    }
}
