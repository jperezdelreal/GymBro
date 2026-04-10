package com.gymbro.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns with default values to avoid data loss
            db.execSQL("ALTER TABLE exercises ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE exercises ADD COLUMN youtubeUrl TEXT")
            
            // Rename instructions to description by creating new column and copying data
            db.execSQL("ALTER TABLE exercises ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE exercises SET description = instructions")
            
            // Create new table with correct schema
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS exercises_new (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    muscleGroup TEXT NOT NULL,
                    category TEXT NOT NULL,
                    equipment TEXT NOT NULL,
                    description TEXT NOT NULL,
                    youtubeUrl TEXT,
                    PRIMARY KEY(id)
                )
            """.trimIndent())
            
            // Copy data from old table to new table
            db.execSQL("""
                INSERT INTO exercises_new (id, name, muscleGroup, category, equipment, description, youtubeUrl)
                SELECT id, name, muscleGroup, category, equipment, description, youtubeUrl
                FROM exercises
            """.trimIndent())
            
            // Drop old table and rename new table
            db.execSQL("DROP TABLE exercises")
            db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
        }
    }
    
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create workouts table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workouts (
                    id TEXT NOT NULL,
                    startedAt INTEGER NOT NULL,
                    completedAt INTEGER,
                    durationSeconds INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    completed INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
            """.trimIndent())
            
            // Create workout_sets table with foreign keys and indices
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_sets (
                    id TEXT NOT NULL,
                    workoutId TEXT NOT NULL,
                    exerciseId TEXT NOT NULL,
                    setNumber INTEGER NOT NULL,
                    weight REAL NOT NULL,
                    reps INTEGER NOT NULL,
                    rpe REAL,
                    isWarmup INTEGER NOT NULL,
                    completedAt INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(workoutId) REFERENCES workouts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indices for foreign keys
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_workoutId ON workout_sets (workoutId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_exerciseId ON workout_sets (exerciseId)")
        }
    }
    
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create workout_templates table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_templates (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    lastUsedAt INTEGER,
                    isBuiltIn INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
            """.trimIndent())
            
            // Create template_exercises table with foreign keys and indices
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS template_exercises (
                    id TEXT NOT NULL,
                    templateId TEXT NOT NULL,
                    exerciseId TEXT NOT NULL,
                    exerciseName TEXT NOT NULL,
                    muscleGroup TEXT NOT NULL,
                    targetSets INTEGER NOT NULL,
                    targetReps INTEGER NOT NULL,
                    targetWeightKg REAL,
                    orderIndex INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indices for foreign keys
            db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_templateId ON template_exercises (templateId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_exerciseId ON template_exercises (exerciseId)")
        }
    }
    
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create in_progress_workouts table for persisting active workout state
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS in_progress_workouts (
                    workoutId TEXT NOT NULL,
                    exercisesJson TEXT NOT NULL,
                    elapsedSeconds INTEGER NOT NULL,
                    totalVolume REAL NOT NULL,
                    totalSets INTEGER NOT NULL,
                    restTimerSeconds INTEGER NOT NULL,
                    restTimerTotal INTEGER NOT NULL,
                    isRestTimerActive INTEGER NOT NULL,
                    lastSavedAt INTEGER NOT NULL,
                    PRIMARY KEY(workoutId)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add RIR (Reps in Reserve) column to workout_sets for RPE-based progression
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN rir INTEGER")
        }
    }
}
