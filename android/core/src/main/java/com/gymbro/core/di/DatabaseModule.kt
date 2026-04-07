package com.gymbro.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymbro.core.database.GymBroDatabase
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.ExerciseEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            .addCallback(SeedDatabaseCallback())
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
}

private class SeedDatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            seedExercises().forEach { exercise ->
                db.execSQL(
                    """INSERT OR IGNORE INTO exercises (id, name, muscleGroup, category, equipment, description, youtubeUrl) 
                       VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf(
                        exercise.id,
                        exercise.name,
                        exercise.muscleGroup,
                        exercise.category,
                        exercise.equipment,
                        exercise.description,
                        exercise.youtubeUrl,
                    ),
                )
            }
        }
    }
}

private fun seedExercises(): List<ExerciseEntity> = listOf(
    // Chest
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("bench-press".toByteArray()).toString(),
        name = "Barbell Bench Press",
        muscleGroup = "CHEST",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Lie on a flat bench, grip the bar slightly wider than shoulder-width, lower to chest, press up.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("incline-bench".toByteArray()).toString(),
        name = "Incline Dumbbell Press",
        muscleGroup = "CHEST",
        category = "COMPOUND",
        equipment = "DUMBBELL",
        description = "Set bench to 30-45 degrees, press dumbbells from chest level to lockout.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("cable-fly".toByteArray()).toString(),
        name = "Cable Fly",
        muscleGroup = "CHEST",
        category = "ISOLATION",
        equipment = "CABLE",
        description = "Stand between cable towers, bring handles together in a hugging motion.",
    ),
    // Back
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("deadlift".toByteArray()).toString(),
        name = "Conventional Deadlift",
        muscleGroup = "BACK",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Hinge at hips, grip bar outside knees, drive through floor to lockout.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("barbell-row".toByteArray()).toString(),
        name = "Barbell Row",
        muscleGroup = "BACK",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Hinge forward ~45 degrees, pull bar to lower chest/upper abdomen.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("lat-pulldown".toByteArray()).toString(),
        name = "Lat Pulldown",
        muscleGroup = "BACK",
        category = "ACCESSORY",
        equipment = "CABLE",
        description = "Pull wide bar to upper chest, squeeze lats at bottom.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("pull-up".toByteArray()).toString(),
        name = "Pull-Up",
        muscleGroup = "BACK",
        category = "COMPOUND",
        equipment = "BODYWEIGHT",
        description = "Hang from bar with overhand grip, pull chin above bar.",
    ),
    // Legs
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("back-squat".toByteArray()).toString(),
        name = "Barbell Back Squat",
        muscleGroup = "QUADRICEPS",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Bar on upper back, squat to parallel or below, drive up through heels.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("leg-press".toByteArray()).toString(),
        name = "Leg Press",
        muscleGroup = "QUADRICEPS",
        category = "COMPOUND",
        equipment = "MACHINE",
        description = "Feet shoulder-width on platform, lower sled until 90-degree knee bend, press up.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("rdl".toByteArray()).toString(),
        name = "Romanian Deadlift",
        muscleGroup = "HAMSTRINGS",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Slight knee bend, hinge at hips lowering bar along shins until hamstring stretch.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("leg-curl".toByteArray()).toString(),
        name = "Lying Leg Curl",
        muscleGroup = "HAMSTRINGS",
        category = "ISOLATION",
        equipment = "MACHINE",
        description = "Lie face down, curl pad toward glutes, control the eccentric.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("leg-extension".toByteArray()).toString(),
        name = "Leg Extension",
        muscleGroup = "QUADRICEPS",
        category = "ISOLATION",
        equipment = "MACHINE",
        description = "Sit on machine, extend legs to full lockout, squeeze quads at top.",
    ),
    // Shoulders
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("ohp".toByteArray()).toString(),
        name = "Overhead Press",
        muscleGroup = "SHOULDERS",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Press barbell from front rack position overhead to lockout.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("lateral-raise".toByteArray()).toString(),
        name = "Lateral Raise",
        muscleGroup = "SHOULDERS",
        category = "ISOLATION",
        equipment = "DUMBBELL",
        description = "Raise dumbbells to sides until arms are parallel to floor.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("face-pull".toByteArray()).toString(),
        name = "Face Pull",
        muscleGroup = "SHOULDERS",
        category = "ACCESSORY",
        equipment = "CABLE",
        description = "Pull rope to face level, externally rotate shoulders at end position.",
    ),
    // Arms
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("barbell-curl".toByteArray()).toString(),
        name = "Barbell Curl",
        muscleGroup = "BICEPS",
        category = "ISOLATION",
        equipment = "BARBELL",
        description = "Curl barbell from full extension to peak contraction, keep elbows stationary.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("hammer-curl".toByteArray()).toString(),
        name = "Hammer Curl",
        muscleGroup = "BICEPS",
        category = "ISOLATION",
        equipment = "DUMBBELL",
        description = "Curl dumbbells with neutral grip (palms facing each other).",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("tricep-pushdown".toByteArray()).toString(),
        name = "Tricep Pushdown",
        muscleGroup = "TRICEPS",
        category = "ISOLATION",
        equipment = "CABLE",
        description = "Push cable attachment down until arms are fully extended, elbows at sides.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("skull-crusher".toByteArray()).toString(),
        name = "Skull Crusher",
        muscleGroup = "TRICEPS",
        category = "ISOLATION",
        equipment = "BARBELL",
        description = "Lie on bench, lower bar to forehead by bending elbows, extend back up.",
    ),
    // Core
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("hanging-leg-raise".toByteArray()).toString(),
        name = "Hanging Leg Raise",
        muscleGroup = "CORE",
        category = "ISOLATION",
        equipment = "BODYWEIGHT",
        description = "Hang from bar, raise legs to parallel or above, control the descent.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("cable-crunch".toByteArray()).toString(),
        name = "Cable Crunch",
        muscleGroup = "CORE",
        category = "ISOLATION",
        equipment = "CABLE",
        description = "Kneel at cable machine, crunch down bringing elbows to knees.",
    ),
    // Glutes
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("hip-thrust".toByteArray()).toString(),
        name = "Barbell Hip Thrust",
        muscleGroup = "GLUTES",
        category = "COMPOUND",
        equipment = "BARBELL",
        description = "Back against bench, drive hips up with barbell across lap, squeeze glutes at top.",
    ),
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes("bulgarian-split-squat".toByteArray()).toString(),
        name = "Bulgarian Split Squat",
        muscleGroup = "QUADRICEPS",
        category = "COMPOUND",
        equipment = "DUMBBELL",
        description = "Rear foot elevated on bench, lunge down until front thigh is parallel.",
    ),
)
