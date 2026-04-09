package com.gymbro.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymBroDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )
    
    @Test
    @Throws(IOException::class)
    fun migrate1To2_preservesExerciseData() {
        // Create database version 1
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO exercises (id, name, muscleGroup, equipment, instructions)
                VALUES ('ex1', 'Bench Press', 'CHEST', 'BARBELL', 'Lie on bench and press')
            """)
            close()
        }
        
        // Migrate to version 2
        helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2).apply {
            // Verify data was preserved and new columns exist
            query("SELECT * FROM exercises WHERE id = 'ex1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("ex1", cursor.getString(cursor.getColumnIndex("id")))
                assertEquals("Bench Press", cursor.getString(cursor.getColumnIndex("name")))
                assertEquals("CHEST", cursor.getString(cursor.getColumnIndex("muscleGroup")))
                assertEquals("BARBELL", cursor.getString(cursor.getColumnIndex("equipment")))
                assertEquals("Lie on bench and press", cursor.getString(cursor.getColumnIndex("description")))
                // New columns should exist (category defaults to empty, youtubeUrl is null)
                assertNotNull(cursor.getColumnIndex("category"))
                assertNotNull(cursor.getColumnIndex("youtubeUrl"))
            }
            close()
        }
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate2To3_createsWorkoutTables() {
        // Create database version 2
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("""
                INSERT INTO exercises (id, name, muscleGroup, category, equipment, description, youtubeUrl)
                VALUES ('ex1', 'Squat', 'LEGS', 'STRENGTH', 'BARBELL', 'Deep squat', NULL)
            """)
            close()
        }
        
        // Migrate to version 3
        helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3).apply {
            // Verify exercise data still exists
            query("SELECT COUNT(*) FROM exercises").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            
            // Verify new tables exist and can be used
            execSQL("""
                INSERT INTO workouts (id, startedAt, completedAt, durationSeconds, notes, completed)
                VALUES ('w1', 1000000, 1003600, 3600, 'Great workout', 1)
            """)
            
            execSQL("""
                INSERT INTO workout_sets (id, workoutId, exerciseId, setNumber, weight, reps, rpe, isWarmup, completedAt)
                VALUES ('s1', 'w1', 'ex1', 1, 100.0, 5, 8.0, 0, 1001000)
            """)
            
            // Verify foreign key relationships work
            query("SELECT * FROM workout_sets WHERE workoutId = 'w1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("s1", cursor.getString(cursor.getColumnIndex("id")))
                assertEquals("w1", cursor.getString(cursor.getColumnIndex("workoutId")))
                assertEquals("ex1", cursor.getString(cursor.getColumnIndex("exerciseId")))
            }
            close()
        }
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate3To4_createsTemplateTables() {
        // Create database version 3
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("""
                INSERT INTO exercises (id, name, muscleGroup, category, equipment, description, youtubeUrl)
                VALUES ('ex1', 'Deadlift', 'BACK', 'STRENGTH', 'BARBELL', 'Lift from ground', NULL)
            """)
            close()
        }
        
        // Migrate to version 4
        helper.runMigrationsAndValidate(TEST_DB, 4, true, Migrations.MIGRATION_3_4).apply {
            // Verify exercise data still exists
            query("SELECT COUNT(*) FROM exercises").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            
            // Verify new template tables exist and can be used
            execSQL("""
                INSERT INTO workout_templates (id, name, description, createdAt, lastUsedAt, isBuiltIn)
                VALUES ('t1', 'Starting Strength', 'Classic beginner program', 1000000, NULL, 1)
            """)
            
            execSQL("""
                INSERT INTO template_exercises (id, templateId, exerciseId, exerciseName, muscleGroup, targetSets, targetReps, targetWeightKg, orderIndex)
                VALUES ('te1', 't1', 'ex1', 'Deadlift', 'BACK', 3, 5, 100.0, 0)
            """)
            
            // Verify foreign key relationships work
            query("SELECT * FROM template_exercises WHERE templateId = 't1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("te1", cursor.getString(cursor.getColumnIndex("id")))
                assertEquals("t1", cursor.getString(cursor.getColumnIndex("templateId")))
                assertEquals("ex1", cursor.getString(cursor.getColumnIndex("exerciseId")))
            }
            close()
        }
    }
    
    @Test
    @Throws(IOException::class)
    fun migrateAll_preservesData() {
        // Create database version 1 with exercise
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO exercises (id, name, muscleGroup, equipment, instructions)
                VALUES ('ex1', 'Overhead Press', 'SHOULDERS', 'BARBELL', 'Press overhead')
            """)
            close()
        }
        
        // Migrate all the way to version 4
        helper.runMigrationsAndValidate(
            TEST_DB, 
            4, 
            true, 
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4
        ).apply {
            // Verify original exercise data survived all migrations
            query("SELECT * FROM exercises WHERE id = 'ex1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("ex1", cursor.getString(cursor.getColumnIndex("id")))
                assertEquals("Overhead Press", cursor.getString(cursor.getColumnIndex("name")))
                assertEquals("SHOULDERS", cursor.getString(cursor.getColumnIndex("muscleGroup")))
                assertEquals("BARBELL", cursor.getString(cursor.getColumnIndex("equipment")))
                assertEquals("Press overhead", cursor.getString(cursor.getColumnIndex("description")))
            }
            
            // Verify all tables exist
            query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('exercises', 'workouts', 'workout_sets', 'workout_templates', 'template_exercises')").use { cursor ->
                assertEquals(5, cursor.count)
            }
            close()
        }
    }
    
    @Test
    @Throws(IOException::class)
    fun migrateToLatest_canOpenDatabaseWithRoomDirectly() {
        // Create and migrate database
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO exercises (id, name, muscleGroup, equipment, instructions)
                VALUES ('ex1', 'Row', 'BACK', 'CABLE', 'Pull to chest')
            """)
            close()
        }
        
        helper.runMigrationsAndValidate(
            TEST_DB, 
            4, 
            true, 
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4
        ).close()
        
        // Verify Room can open the database after migrations
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            GymBroDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4
            )
            .build()
        
        // Verify data is accessible through Room
        val exercises = database.exerciseDao().getAllExercises()
        database.close()
        
        // This verifies the database schema is valid for Room
        assertNotNull(exercises)
    }
}
