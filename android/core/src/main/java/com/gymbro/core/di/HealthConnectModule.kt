package com.gymbro.core.di

import com.gymbro.core.health.HealthConnectRepository
import com.gymbro.core.health.HealthConnectRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthConnectModule {

    @Binds
    abstract fun bindHealthConnectRepository(
        impl: HealthConnectRepositoryImpl,
    ): HealthConnectRepository
}
