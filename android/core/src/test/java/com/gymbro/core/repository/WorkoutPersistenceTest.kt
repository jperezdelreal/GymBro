package com.gymbro.core.repository

import com.google.gson.Gson
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.InProgressWorkoutEntity
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.InProgressExercise
import com.gymbro.core.model.InProgressSet
import com.gymbro.core.model.InProgressWorkout
import com.gymbro.core.model.MuscleGroup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class WorkoutPersistenceTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var gson: Gson
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        gson = Gson()
        repository = WorkoutRepositoryImpl(workoutDao, gson)
    }

    @Test
    fun `saveInProgressWorkout serializes and persists state`() = runTest {
        val workoutId = UUID.randomUUID().toString()
        val exercise = Exercise(
            id = UUID.randomUUID(),
            name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.BARBELL,
            description = "Test exercise"
        )
        val inProgressWorkout = InProgressWorkout(
            workoutId = workoutId,
            exercises = listOf(
                InProgressExercise(
                    exercise = exercise,
                    sets = listOf(
                        InProgressSet(
                            id = UUID.randomUUID().toString(),
                            setNumber = 1,
                            weight = "100",
                            reps = "8",
                            rpe = "8.5",
                            isWarmup = false,
                            isCompleted = true
                        )
                    )
                )
            ),
            elapsedSeconds = 300L,
            totalVolume = 800.0,
            totalSets = 1,
            restTimerSeconds = 60,
            restTimerTotal = 90,
            isRestTimerActive = false
        )

        repository.saveInProgressWorkout(inProgressWorkout)

        coVerify {
            workoutDao.saveInProgressWorkout(
                withArg { entity ->
                    assertEquals(workoutId, entity.workoutId)
                    assertEquals(300L, entity.elapsedSeconds)
                    assertEquals(800.0, entity.totalVolume, 0.001)
                    assertEquals(1, entity.totalSets)
                    assertNotNull(entity.exercisesJson)
                }
            )
        }
    }

    @Test
    fun `getInProgressWorkout returns null when no workout exists`() = runTest {
        coEvery { workoutDao.getAnyInProgressWorkout() } returns null

        val result = repository.getInProgressWorkout()

        assertNull(result)
    }

    @Test
    fun `getInProgressWorkout deserializes and returns workout`() = runTest {
        val workoutId = UUID.randomUUID().toString()
        val exerciseId = UUID.randomUUID()
        val setId = UUID.randomUUID().toString()
        
        val exercisesJson = gson.toJson(listOf(
            mapOf(
                "exerciseId" to exerciseId.toString(),
                "exerciseName" to "Bench Press",
                "muscleGroup" to "CHEST",
                "sets" to listOf(
                    mapOf(
                        "id" to setId,
                        "setNumber" to 1.0,
                        "weight" to "100",
                        "reps" to "8",
                        "rpe" to "8.5",
                        "isWarmup" to false,
                        "isCompleted" to true
                    )
                )
            )
        ))

        val entity = InProgressWorkoutEntity(
            workoutId = workoutId,
            exercisesJson = exercisesJson,
            elapsedSeconds = 300L,
            totalVolume = 800.0,
            totalSets = 1,
            restTimerSeconds = 60,
            restTimerTotal = 90,
            isRestTimerActive = false
        )

        coEvery { workoutDao.getAnyInProgressWorkout() } returns entity

        val result = repository.getInProgressWorkout()

        assertNotNull(result)
        assertEquals(workoutId, result!!.workoutId)
        assertEquals(300L, result.elapsedSeconds)
        assertEquals(800.0, result.totalVolume, 0.001)
        assertEquals(1, result.exercises.size)
        assertEquals("Bench Press", result.exercises[0].exercise.name)
        assertEquals(1, result.exercises[0].sets.size)
        assertEquals(setId, result.exercises[0].sets[0].id)
        assertEquals("100", result.exercises[0].sets[0].weight)
        assertTrue(result.exercises[0].sets[0].isCompleted)
    }

    @Test
    fun `clearInProgressWorkout removes persisted state`() = runTest {
        val workoutId = UUID.randomUUID().toString()

        repository.clearInProgressWorkout(workoutId)

        coVerify { workoutDao.clearInProgressWorkout(workoutId) }
    }
}
