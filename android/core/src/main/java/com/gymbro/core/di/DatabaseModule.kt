package com.gymbro.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymbro.core.database.GymBroDatabase
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutTemplateDao
import com.gymbro.core.database.entity.ExerciseEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
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
        )
            .fallbackToDestructiveMigration(true)
            .addCallback(SeedDatabaseCallback(context))
            .build()
    }

    @Provides
    fun provideExerciseDao(database: GymBroDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideWorkoutDao(database: GymBroDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideWorkoutTemplateDao(database: GymBroDatabase): WorkoutTemplateDao {
        return database.workoutTemplateDao()
    }
}

@Serializable
private data class ExerciseSeed(
    val name: String,
    val category: String,
    val equipment: String,
    val instructions: String,
    val muscleGroups: List<MuscleGroupSeed>,
    val videoURL: String? = null,
)

@Serializable
private data class MuscleGroupSeed(
    val name: String,
    val isPrimary: Boolean,
)

private class SeedDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            val exercises = loadExercisesFromAssets(context)
            // Use the DAO for cleaner insertion with proper conflict resolution
            val database = Room.databaseBuilder(
                context,
                GymBroDatabase::class.java,
                "gymbro.db"
            ).build()
            database.exerciseDao().insertAll(exercises)
            database.close()
        }
    }
}

private fun loadExercisesFromAssets(context: Context): List<ExerciseEntity> {
    val json = context.assets.open("exercises-seed.json").bufferedReader().use { it.readText() }
    val seeds = Json { ignoreUnknownKeys = true }.decodeFromString<List<ExerciseSeed>>(json)
    
    return seeds.map { seed ->
        // Pick the first primary muscle group as the main muscleGroup
        val primaryMuscleGroup = seed.muscleGroups
            .firstOrNull { it.isPrimary }
            ?.name
            ?.uppercase()
            ?.replace(" ", "_") 
            ?: "FULL_BODY"
        
        ExerciseEntity(
            id = UUID.nameUUIDFromBytes(seed.name.toByteArray()).toString(),
            name = seed.name,
            muscleGroup = primaryMuscleGroup,
            category = seed.category.uppercase(),
            equipment = seed.equipment.uppercase(),
            description = seed.instructions,
            youtubeUrl = seed.videoURL,
        )
    }
}
