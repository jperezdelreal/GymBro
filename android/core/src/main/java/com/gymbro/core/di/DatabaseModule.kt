package com.gymbro.core.di

import android.content.Context
import androidx.room.Room
import com.gymbro.core.database.GymBroDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymBroDatabase {
        return Room.databaseBuilder(
            context,
            GymBroDatabase::class.java,
            "gymbro.db",
        ).build()
    }
}
