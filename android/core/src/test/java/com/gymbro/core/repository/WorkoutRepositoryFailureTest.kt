package com.gymbro.core.repository

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.model.ExerciseSet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.util.UUID

class WorkoutRepositoryFailureTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk()
        repository = WorkoutRepositoryImpl(workoutDao)
    }

    // ============ startWorkout Failure Tests ============

    @Test
    fun `startWorkout retries on SQLiteDiskIOException then throws`() = runTest {
        // Given - Simulate disk full scenario
        coEvery { workoutDao.insertWorkout(any()) } throws SQLiteDiskIOException("Disk full")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.startWorkout() }
        }

        // Verify retry happened 3 times
        coVerify(exactly = 3) { workoutDao.insertWorkout(any()) }
        assertTrue(exception.message?.contains("Database disk I/O error") == true)
    }

    @Test
    fun `startWorkout retries on IOException then throws`() = runTest {
        // Given - Simulate network timeout or I/O error
        coEvery { workoutDao.insertWorkout(any()) } throws IOException("Network timeout")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.startWorkout() }
        }

        // Verify retry happened 3 times
        coVerify(exactly = 3) { workoutDao.insertWorkout(any()) }
        assertTrue(exception.message?.contains("Network error") == true)
    }

    @Test
    fun `startWorkout retries on SQLiteDatabaseLockedException then throws`() = runTest {
        // Given - Simulate concurrent write conflict
        coEvery { workoutDao.insertWorkout(any()) } throws SQLiteDatabaseLockedException("Database is locked")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.startWorkout() }
        }

        // Verify retry happened 3 times
        coVerify(exactly = 3) { workoutDao.insertWorkout(any()) }
        assertTrue(exception.message?.contains("Database is locked") == true)
    }

    @Test
    fun `startWorkout does not retry on RuntimeException`() = runTest {
        // Given - Simulate corrupted data or logic error
        coEvery { workoutDao.insertWorkout(any()) } throws RuntimeException("Invalid UUID")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.startWorkout() }
        }

        // Verify NO retry (should fail immediately)
        coVerify(exactly = 1) { workoutDao.insertWorkout(any()) }
        assertTrue(exception.message?.contains("Invalid UUID") == true)
    }

    @Test
    fun `startWorkout does not retry on IllegalStateException`() = runTest {
        // Given - Simulate corrupted database state
        coEvery { workoutDao.insertWorkout(any()) } throws IllegalStateException("Database corrupted")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.startWorkout() }
        }

        // Verify NO retry
        coVerify(exactly = 1) { workoutDao.insertWorkout(any()) }
        assertTrue(exception.message?.contains("Database corrupted") == true)
    }

    // ============ addSet Failure Tests ============

    @Test
    fun `addSet retries on SQLiteDiskIOException then throws`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val exerciseSet = ExerciseSet(
            id = UUID.randomUUID(),
            exerciseId = UUID.randomUUID(),
            weightKg = 100.0,
            reps = 5,
            isWarmup = false,
            completedAt = Instant.now()
        )
        coEvery { workoutDao.insertSet(any()) } throws SQLiteDiskIOException("Disk full")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addSet(workoutId, exerciseSet) }
        }

        coVerify(exactly = 3) { workoutDao.insertSet(any()) }
        assertTrue(exception.message?.contains("Database disk I/O error") == true)
    }

    @Test
    fun `addSet does not retry on NumberFormatException`() = runTest {
        // Given - Simulate corrupted numeric data
        val workoutId = UUID.randomUUID().toString()
        val exerciseSet = ExerciseSet(
            id = UUID.randomUUID(),
            exerciseId = UUID.randomUUID(),
            weightKg = 100.0,
            reps = 5,
            isWarmup = false,
            completedAt = Instant.now()
        )
        coEvery { workoutDao.insertSet(any()) } throws NumberFormatException("Invalid number")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.addSet(workoutId, exerciseSet) }
        }

        // Verify NO retry
        coVerify(exactly = 1) { workoutDao.insertSet(any()) }
        assertTrue(exception.message?.contains("Invalid number") == true)
    }

    // ============ removeSet Failure Tests ============

    @Test
    fun `removeSet retries on SQLiteDatabaseLockedException then throws`() = runTest {
        // Given
        val setId = UUID.randomUUID().toString()
        coEvery { workoutDao.deleteSet(setId) } throws SQLiteDatabaseLockedException("Database is locked")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.removeSet(setId) }
        }

        coVerify(exactly = 3) { workoutDao.deleteSet(setId) }
        assertTrue(exception.message?.contains("Database is locked") == true)
    }

    @Test
    fun `removeSet does not retry on IllegalArgumentException`() = runTest {
        // Given - Simulate invalid set ID
        val setId = "invalid-uuid"
        coEvery { workoutDao.deleteSet(setId) } throws IllegalArgumentException("Invalid UUID")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.removeSet(setId) }
        }

        // Verify NO retry
        coVerify(exactly = 1) { workoutDao.deleteSet(setId) }
        assertTrue(exception.message?.contains("Invalid UUID") == true)
    }

    // ============ completeWorkout Failure Tests ============

    @Test
    fun `completeWorkout retries on IOException then throws`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } throws IOException("Network timeout")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.completeWorkout(workoutId, 3600L, "Great session") }
        }

        coVerify(exactly = 3) { workoutDao.getWorkoutWithSets(workoutId) }
        assertTrue(exception.message?.contains("Network error") == true)
    }

    @Test
    fun `completeWorkout does not retry on RuntimeException`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } throws RuntimeException("Corrupted data")

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            runTest { repository.completeWorkout(workoutId, 3600L, "Great session") }
        }

        // Verify NO retry
        coVerify(exactly = 1) { workoutDao.getWorkoutWithSets(workoutId) }
        assertTrue(exception.message?.contains("Corrupted data") == true)
    }

    // ============ getWorkout Failure Tests ============

    @Test
    fun `getWorkout retries on SQLiteDiskIOException then returns null`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } throws SQLiteDiskIOException("Disk I/O error")

        // When
        val result = repository.getWorkout(workoutId)

        // Then - Should return null on failure
        assertNull(result)
        coVerify(exactly = 3) { workoutDao.getWorkoutWithSets(workoutId) }
    }

    @Test
    fun `getWorkout does not retry on SQLiteException and returns null`() = runTest {
        // Given - Non-retryable SQLite error
        val workoutId = UUID.randomUUID().toString()
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } throws SQLiteException("Database corrupted")

        // When
        val result = repository.getWorkout(workoutId)

        // Then
        assertNull(result)
        // Verify NO retry for non-retryable SQLite errors
        coVerify(exactly = 1) { workoutDao.getWorkoutWithSets(workoutId) }
    }

    // ============ observeWorkout Failure Tests ============

    @Test
    fun `observeWorkout emits null on Flow error`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        every { workoutDao.observeWorkoutWithSets(workoutId) } returns flow {
            throw RuntimeException("Flow error")
        }

        // When - Flow catch block should emit null
        val result = repository.observeWorkout(workoutId).first()

        // Then
        assertNull(result)
    }

    // ============ getRecentWorkouts Failure Tests ============

    @Test
    fun `getRecentWorkouts emits empty list on Flow error`() = runTest {
        // Given
        every { workoutDao.getRecentWorkouts(20) } returns flow {
            throw RuntimeException("Flow error")
        }

        // When - Flow catch block should emit empty list
        val result = repository.getRecentWorkouts().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ============ getBestWeight Failure Tests ============

    @Test
    fun `getBestWeight retries on SQLiteDatabaseLockedException then returns null`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        val reps = 5
        coEvery { workoutDao.getBestWeight(exerciseId, reps) } throws SQLiteDatabaseLockedException("Database is locked")

        // When
        val result = repository.getBestWeight(exerciseId, reps)

        // Then
        assertNull(result)
        coVerify(exactly = 3) { workoutDao.getBestWeight(exerciseId, reps) }
    }

    @Test
    fun `getBestWeight does not retry on IllegalStateException and returns null`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        val reps = 5
        coEvery { workoutDao.getBestWeight(exerciseId, reps) } throws IllegalStateException("Invalid state")

        // When
        val result = repository.getBestWeight(exerciseId, reps)

        // Then
        assertNull(result)
        coVerify(exactly = 1) { workoutDao.getBestWeight(exerciseId, reps) }
    }

    // ============ getDaysSinceLastWorkout Failure Tests ============

    @Test
    fun `getDaysSinceLastWorkout retries on IOException then returns null`() = runTest {
        // Given
        coEvery { workoutDao.getLastCompletedTimestamp() } throws IOException("Network timeout")

        // When
        val result = repository.getDaysSinceLastWorkout()

        // Then
        assertNull(result)
        coVerify(exactly = 3) { workoutDao.getLastCompletedTimestamp() }
    }

    @Test
    fun `getDaysSinceLastWorkout does not retry on RuntimeException and returns null`() = runTest {
        // Given
        coEvery { workoutDao.getLastCompletedTimestamp() } throws RuntimeException("Data corruption")

        // When
        val result = repository.getDaysSinceLastWorkout()

        // Then
        assertNull(result)
        coVerify(exactly = 1) { workoutDao.getLastCompletedTimestamp() }
    }

    // ============ Retry Recovery Tests ============

    @Test
    fun `startWorkout succeeds on third retry after transient failures`() = runTest {
        // Given - First two attempts fail, third succeeds
        var attemptCount = 0
        coEvery { workoutDao.insertWorkout(any()) } answers {
            attemptCount++
            if (attemptCount < 3) {
                throw SQLiteDatabaseLockedException("Database is locked")
            } else {
                Unit // Success on third attempt
            }
        }

        // When
        val workout = repository.startWorkout()

        // Then
        assertNotNull(workout)
        coVerify(exactly = 3) { workoutDao.insertWorkout(any()) }
    }

    @Test
    fun `getBestWeight succeeds on second retry after transient failure`() = runTest {
        // Given - First attempt fails, second succeeds
        var attemptCount = 0
        val exerciseId = UUID.randomUUID().toString()
        val expectedWeight = 120.0
        coEvery { workoutDao.getBestWeight(exerciseId, 5) } answers {
            attemptCount++
            if (attemptCount == 1) {
                throw SQLiteDiskIOException("Disk I/O error")
            } else {
                expectedWeight
            }
        }

        // When
        val result = repository.getBestWeight(exerciseId, 5)

        // Then
        assertEquals(expectedWeight, result)
        coVerify(exactly = 2) { workoutDao.getBestWeight(exerciseId, 5) }
    }

    // Helper to assert exception type
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
