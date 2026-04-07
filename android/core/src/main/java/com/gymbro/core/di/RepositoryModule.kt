package com.gymbro.core.di

import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.repository.ExerciseRepositoryImpl
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.repository.WorkoutRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository
}
