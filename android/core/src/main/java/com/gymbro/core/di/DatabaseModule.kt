package com.gymbro.core.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.gymbro.core.database.GymBroDatabase
import com.gymbro.core.database.Migrations
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutTemplateDao
import com.gymbro.core.database.entity.ExerciseEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
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
        val database = Room.databaseBuilder(
            context,
            GymBroDatabase::class.java,
            "gymbro.db",
        )
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6,
            )
            .build()
        
        // Seed exercises synchronously on first run — must block so exercises
        // are available before the database instance is returned to DI container.
        runBlocking {
            val exerciseCount = database.exerciseDao().count()
            if (exerciseCount == 0) {
                val exercises = loadExercisesFromAssets(context)
                database.exerciseDao().insertAll(exercises)
            }
        }
        
        return database
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

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

@Serializable
private data class InstructionsSeed(
    val setup: String,
    val execution: String,
    val tips: String,
)

@Serializable
private data class ExerciseSeed(
    val name: String,
    val nameEs: String? = null,
    val category: String,
    val equipment: String,
    val instructions: kotlinx.serialization.json.JsonElement,
    val instructionsEs: kotlinx.serialization.json.JsonElement? = null,
    val muscleGroups: List<MuscleGroupSeed>,
    val videoURL: String? = null,
)

@Serializable
private data class MuscleGroupSeed(
    val name: String,
    val isPrimary: Boolean,
)


private fun loadExercisesFromAssets(context: Context): List<ExerciseEntity> {
    val json = context.assets.open("exercises-seed.json").bufferedReader().use { it.readText() }
    val seeds = Json { ignoreUnknownKeys = true }.decodeFromString<List<ExerciseSeed>>(json)
    
    // Check if device locale is Spanish
    val isSpanish = context.resources.configuration.locales[0].language == "es"
    
    return seeds.map { seed ->
        // Pick the first primary muscle group as the main muscleGroup
        val primaryMuscleGroup = seed.muscleGroups
            .firstOrNull { it.isPrimary }
            ?.name
            ?.uppercase()
            ?.replace(" ", "_") 
            ?: "FULL_BODY"
        
        // Parse instructions based on format (structured object or legacy string)
        val description = parseInstructions(
            instructionsElement = if (isSpanish && seed.instructionsEs != null) 
                seed.instructionsEs else seed.instructions
        )
        
        ExerciseEntity(
            id = UUID.nameUUIDFromBytes(seed.name.toByteArray()).toString(),
            name = if (isSpanish && seed.nameEs != null) seed.nameEs else seed.name,
            muscleGroup = primaryMuscleGroup,
            category = seed.category.uppercase(),
            equipment = seed.equipment.uppercase(),
            description = description,
            youtubeUrl = seed.videoURL,
        )
    }
}

private fun parseInstructions(instructionsElement: kotlinx.serialization.json.JsonElement): String {
    val jsonParser = Json { ignoreUnknownKeys = true }
    return when {
        // Structured format: { setup, execution, tips }
        instructionsElement is kotlinx.serialization.json.JsonObject -> {
            try {
                val structured = jsonParser.decodeFromJsonElement(InstructionsSeed.serializer(), instructionsElement)
                buildString {
                    append("##SETUP##\n")
                    append(structured.setup)
                    append("\n\n##EXECUTION##\n")
                    append(structured.execution)
                    append("\n\n##TIPS##\n")
                    append(structured.tips)
                }
            } catch (e: Exception) {
                // Fallback if structured parsing fails
                instructionsElement.toString()
            }
        }
        // Legacy format: plain string
        instructionsElement is kotlinx.serialization.json.JsonPrimitive -> {
            instructionsElement.content
        }
        else -> ""
    }
}
