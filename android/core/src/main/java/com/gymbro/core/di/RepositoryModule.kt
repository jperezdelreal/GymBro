package com.gymbro.core.di

import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.repository.ExerciseRepositoryImpl
import com.gymbro.core.repository.ProgramTemplateRepository
import com.gymbro.core.repository.ProgramTemplateRepositoryImpl
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.repository.WorkoutRepositoryImpl
import com.gymbro.core.repository.WorkoutTemplateRepository
import com.gymbro.core.repository.WorkoutTemplateRepositoryImpl
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

    @Binds
    abstract fun bindWorkoutTemplateRepository(impl: WorkoutTemplateRepositoryImpl): WorkoutTemplateRepository

    @Binds
    abstract fun bindProgramTemplateRepository(impl: ProgramTemplateRepositoryImpl): ProgramTemplateRepository
}
