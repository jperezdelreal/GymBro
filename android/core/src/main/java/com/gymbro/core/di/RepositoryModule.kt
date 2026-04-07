package com.gymbro.core.di

import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.repository.ExerciseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository
}
