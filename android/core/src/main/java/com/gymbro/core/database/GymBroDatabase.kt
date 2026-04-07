package com.gymbro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymbro.core.database.entity.ExerciseEntity

@Database(
    entities = [ExerciseEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GymBroDatabase : RoomDatabase() {
    abstract fun exerciseDao(): com.gymbro.core.database.dao.ExerciseDao
}
