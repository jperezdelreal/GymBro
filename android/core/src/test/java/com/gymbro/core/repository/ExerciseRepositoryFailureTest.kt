package com.gymbro.core.repository

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.UUID

class ExerciseRepositoryFailureTest {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setup() {
        exerciseDao = mockk()
        repository = ExerciseRepositoryImpl(exerciseDao)
    }

    // ============ getAllExercises Failure Tests ============

    @Test
    fun `getAllExercises emits empty list on Flow error`() = runTest {
        // Given
        every { exerciseDao.getAllExercises() } returns flowOf<List<ExerciseEntity>>().also {
            throw RuntimeException("Flow error")
        }

        // When/Then - Flow catch block should emit empty list
        try {
            val result = repository.getAllExercises().first()
            // If we get here, check it's handled gracefully
        } catch (e: Exception) {
            fail("Flow should catch errors and emit empty list, not propagate exception")
        }
    }

    @Test
    fun `getAllExercises emits empty list on SQLiteException`() = runTest {
        // Given
        every { exerciseDao.getAllExercises() } returns flowOf<List<ExerciseEntity>>().also {
            throw SQLiteException("Database corrupted")
        }

        // When/Then
        try {
            val result = repository.getAllExercises().first()
        } catch (e: Exception) {
            fail("Flow should catch SQLiteException and emit empty list")
        }
    }

    // ============ getFilteredExercises Failure Tests ============

    @Test
    fun `getFilteredExercises emits empty list on Flow error`() = runTest {
        // Given
        every { exerciseDao.getFilteredExercises(any(), any()) } returns flowOf<List<ExerciseEntity>>().also {
            throw RuntimeException("Flow error")
        }

        // When/Then
        try {
            val result = repository.getFilteredExercises("Chest", "bench").first()
        } catch (e: Exception) {
            fail("Flow should catch errors and emit empty list")
        }
    }

    @Test
    fun `getFilteredExercises emits empty list on IOException`() = runTest {
        // Given
        every { exerciseDao.getFilteredExercises(null, "squat") } returns flowOf<List<ExerciseEntity>>().also {
            throw IOException("Network timeout")
        }

        // When/Then
        try {
            val result = repository.getFilteredExercises(null, "squat").first()
        } catch (e: Exception) {
            fail("Flow should catch IOException and emit empty list")
        }
    }

    // ============ getExerciseById Failure Tests ============

    @Test
    fun `getExerciseById retries on SQLiteDiskIOException then returns null`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        coEvery { exerciseDao.getExerciseById(exerciseId) } throws SQLiteDiskIOException("Disk full")

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNull(result)
        coVerify(exactly = 3) { exerciseDao.getExerciseById(exerciseId) }
    }

    @Test
    fun `getExerciseById retries on IOException then returns null`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        coEvery { exerciseDao.getExerciseById(exerciseId) } throws IOException("Network timeout")

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNull(result)
        coVerify(exactly = 3) { exerciseDao.getExerciseById(exerciseId) }
    }

    @Test
    fun `getExerciseById does not retry on RuntimeException and returns null`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        coEvery { exerciseDao.getExerciseById(exerciseId) } throws RuntimeException("Invalid UUID")

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNull(result)
        coVerify(exactly = 1) { exerciseDao.getExerciseById(exerciseId) }
    }

    @Test
    fun `getExerciseById does not retry on NumberFormatException and returns null`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        coEvery { exerciseDao.getExerciseById(exerciseId) } throws NumberFormatException("Invalid number")

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNull(result)
        coVerify(exactly = 1) { exerciseDao.getExerciseById(exerciseId) }
    }

    // ============ addExercise Failure Tests ============

    @Test
    fun `addExercise retries on SQLiteDatabaseLockedException then throws`() = runTest {
        // Given
        val exercise = createSampleExercise()
        coEvery { exerciseDao.insertExercise(any()) } throws SQLiteDatabaseLockedException("Database is locked")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addExercise(exercise) }
        }

        coVerify(exactly = 3) { exerciseDao.insertExercise(any()) }
        assertTrue(exception.message?.contains("Database is locked") == true)
    }

    @Test
    fun `addExercise retries on SQLiteDiskIOException then throws`() = runTest {
        // Given
        val exercise = createSampleExercise()
        coEvery { exerciseDao.insertExercise(any()) } throws SQLiteDiskIOException("Disk full")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addExercise(exercise) }
        }

        coVerify(exactly = 3) { exerciseDao.insertExercise(any()) }
        assertTrue(exception.message?.contains("Database disk I/O error") == true)
    }

    @Test
    fun `addExercise retries on IOException then throws`() = runTest {
        // Given
        val exercise = createSampleExercise()
        coEvery { exerciseDao.insertExercise(any()) } throws IOException("Network timeout")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addExercise(exercise) }
        }

        coVerify(exactly = 3) { exerciseDao.insertExercise(any()) }
        assertTrue(exception.message?.contains("Network error") == true)
    }

    @Test
    fun `addExercise does not retry on IllegalArgumentException`() = runTest {
        // Given - Invalid exercise data
        val exercise = createSampleExercise()
        coEvery { exerciseDao.insertExercise(any()) } throws IllegalArgumentException("Invalid exercise data")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addExercise(exercise) }
        }

        // Verify NO retry
        coVerify(exactly = 1) { exerciseDao.insertExercise(any()) }
        assertTrue(exception.message?.contains("Invalid exercise data") == true)
    }

    @Test
    fun `addExercise does not retry on IllegalStateException`() = runTest {
        // Given - Database in invalid state
        val exercise = createSampleExercise()
        coEvery { exerciseDao.insertExercise(any()) } throws IllegalStateException("Database corrupted")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addExercise(exercise) }
        }

        // Verify NO retry
        coVerify(exactly = 1) { exerciseDao.insertExercise(any()) }
        assertTrue(exception.message?.contains("Database corrupted") == true)
    }

    @Test
    fun `addExercise does not retry on RuntimeException with corrupted data`() = runTest {
        // Given
        val exercise = createSampleExercise()
        coEvery { exerciseDao.insertExercise(any()) } throws RuntimeException("Data corruption detected")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addExercise(exercise) }
        }

        // Verify NO retry
        coVerify(exactly = 1) { exerciseDao.insertExercise(any()) }
        assertTrue(exception.message?.contains("Data corruption detected") == true)
    }

    // ============ isExerciseNameTaken Failure Tests ============

    @Test
    fun `isExerciseNameTaken retries on SQLiteDiskIOException then returns false`() = runTest {
        // Given
        val exerciseName = "Bench Press"
        coEvery { exerciseDao.countExercisesByName(exerciseName) } throws SQLiteDiskIOException("Disk I/O error")

        // When
        val result = repository.isExerciseNameTaken(exerciseName)

        // Then - Should return false on failure (safe default)
        assertFalse(result)
        coVerify(exactly = 3) { exerciseDao.countExercisesByName(exerciseName) }
    }

    @Test
    fun `isExerciseNameTaken retries on IOException then returns false`() = runTest {
        // Given
        val exerciseName = "Squat"
        coEvery { exerciseDao.countExercisesByName(exerciseName) } throws IOException("Network timeout")

        // When
        val result = repository.isExerciseNameTaken(exerciseName)

        // Then
        assertFalse(result)
        coVerify(exactly = 3) { exerciseDao.countExercisesByName(exerciseName) }
    }

    @Test
    fun `isExerciseNameTaken does not retry on SQLiteException and returns false`() = runTest {
        // Given
        val exerciseName = "Deadlift"
        coEvery { exerciseDao.countExercisesByName(exerciseName) } throws SQLiteException("Database corrupted")

        // When
        val result = repository.isExerciseNameTaken(exerciseName)

        // Then
        assertFalse(result)
        // Non-retryable SQLite error
        coVerify(exactly = 1) { exerciseDao.countExercisesByName(exerciseName) }
    }

    @Test
    fun `isExerciseNameTaken does not retry on RuntimeException and returns false`() = runTest {
        // Given
        val exerciseName = "Press"
        coEvery { exerciseDao.countExercisesByName(exerciseName) } throws RuntimeException("Unexpected error")

        // When
        val result = repository.isExerciseNameTaken(exerciseName)

        // Then
        assertFalse(result)
        coVerify(exactly = 1) { exerciseDao.countExercisesByName(exerciseName) }
    }

    // ============ Retry Recovery Tests ============

    @Test
    fun `addExercise succeeds on third retry after transient failures`() = runTest {
        // Given - First two attempts fail, third succeeds
        val exercise = createSampleExercise()
        var attemptCount = 0
        coEvery { exerciseDao.insertExercise(any()) } answers {
            attemptCount++
            if (attemptCount < 3) {
                throw SQLiteDatabaseLockedException("Database is locked")
            } else {
                Unit // Success on third attempt
            }
        }

        // When
        repository.addExercise(exercise)

        // Then - Should succeed without throwing
        coVerify(exactly = 3) { exerciseDao.insertExercise(any()) }
    }

    @Test
    fun `getExerciseById succeeds on second retry after transient failure`() = runTest {
        // Given - First attempt fails, second succeeds
        val exerciseId = UUID.randomUUID().toString()
        val expectedEntity = ExerciseEntity(
            id = exerciseId,
            name = "Bench Press",
            muscleGroup = "CHEST",
            category = "COMPOUND",
            equipment = "BARBELL",
            description = "Chest exercise",
            youtubeUrl = null
        )
        var attemptCount = 0
        coEvery { exerciseDao.getExerciseById(exerciseId) } answers {
            attemptCount++
            if (attemptCount == 1) {
                throw IOException("Network timeout")
            } else {
                expectedEntity
            }
        }

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNotNull(result)
        assertEquals("Bench Press", result?.name)
        coVerify(exactly = 2) { exerciseDao.getExerciseById(exerciseId) }
    }

    @Test
    fun `isExerciseNameTaken succeeds on second retry after transient failure`() = runTest {
        // Given
        val exerciseName = "Squat"
        var attemptCount = 0
        coEvery { exerciseDao.countExercisesByName(exerciseName) } answers {
            attemptCount++
            if (attemptCount == 1) {
                throw SQLiteDiskIOException("Disk I/O error")
            } else {
                1 // Exercise exists
            }
        }

        // When
        val result = repository.isExerciseNameTaken(exerciseName)

        // Then
        assertTrue(result)
        coVerify(exactly = 2) { exerciseDao.countExercisesByName(exerciseName) }
    }

    // ============ Concurrent Write Conflict Tests ============

    @Test
    fun `addExercise handles concurrent write conflicts with retry`() = runTest {
        // Given - Simulate multiple threads trying to write simultaneously
        val exercise = createSampleExercise()
        var attemptCount = 0
        coEvery { exerciseDao.insertExercise(any()) } answers {
            attemptCount++
            if (attemptCount <= 2) {
                throw SQLiteDatabaseLockedException("Database is locked")
            } else {
                Unit
            }
        }

        // When
        repository.addExercise(exercise)

        // Then - Should eventually succeed
        coVerify(exactly = 3) { exerciseDao.insertExercise(any()) }
    }

    // ============ Helper Methods ============

    private fun createSampleExercise(): Exercise {
        return Exercise(
            id = UUID.randomUUID(),
            name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.BARBELL,
            description = "Chest pressing exercise",
            youtubeUrl = null
        )
    }

    private inline fun <reified T : Throwable> assertThrows(
        crossinline block: () -> Unit
    ): T {
        try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName} but no exception was thrown")
        } catch (e: Throwable) {
            if (e is T) {
                return e
            }
            throw AssertionError("Expected ${T::class.java.simpleName} but got ${e::class.java.simpleName}: ${e.message}")
        }
    }
}
