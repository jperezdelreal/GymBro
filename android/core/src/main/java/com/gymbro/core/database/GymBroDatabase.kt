package com.gymbro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutTemplateDao
import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.database.entity.InProgressWorkoutEntity
import com.gymbro.core.database.entity.TemplateExerciseEntity
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.database.entity.WorkoutTemplateEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        InProgressWorkoutEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class GymBroDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
}
