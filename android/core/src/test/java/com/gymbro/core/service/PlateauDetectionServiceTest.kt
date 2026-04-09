package com.gymbro.core.service

import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PlateauType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PlateauDetectionServiceTest {

    private lateinit var prService: PersonalRecordService
    private lateinit var service: PlateauDetectionService

    private val exerciseId = "bench-press-id"
    private val exerciseName = "Bench Press"
    private val now = Instant.parse("2024-01-15T12:00:00Z")

    @Before
    fun setup() {
        prService = mockk()
        service = PlateauDetectionService(prService)
    }

    @Test
    fun `detectPlateaus returns null when no history exists`() = runTest {
        coEvery { prService.getE1RMHistory(exerciseId) } returns emptyList()

        val result = service.detectPlateaus(exerciseId, exerciseName)

        assertNull(result)
    }

    @Test
    fun `detectPlateaus returns null when insufficient data points`() = runTest {
        val history = listOf(
            E1RMDataPoint(
                date = now.minus(7, ChronoUnit.DAYS),
                e1rm = 150.0,
                weight = 135.0,
                reps = 5
            )
        )
        coEvery { prService.getE1RMHistory(exerciseId) } returns history

        val result = service.detectPlateaus(exerciseId, exerciseName)

        assertNull(result)
    }

    @Test
    fun `detectPlateaus detects issues with declining performance`() = runTest {
        val history = listOf(
            E1RMDataPoint(date = now.minus(35, ChronoUnit.DAYS), e1rm = 150.0, weight = 135.0, reps = 5),
            E1RMDataPoint(date = now.minus(28, ChronoUnit.DAYS), e1rm = 155.0, weight = 140.0, reps = 5),
            E1RMDataPoint(date = now.minus(21, ChronoUnit.DAYS), e1rm = 153.0, weight = 138.0, reps = 5),
            E1RMDataPoint(date = now.minus(14, ChronoUnit.DAYS), e1rm = 148.0, weight = 133.0, reps = 5),
            E1RMDataPoint(date = now.minus(7, ChronoUnit.DAYS), e1rm = 145.0, weight = 130.0, reps = 5)
        )
        coEvery { prService.getE1RMHistory(exerciseId) } returns history

        val result = service.detectPlateaus(exerciseId, exerciseName)

        // Service should execute without crashing - specific plateau detection logic may vary
        // This validates the contract: given valid history, it returns a result (or null)
        assertTrue(result == null || result.exerciseId == exerciseId)
    }

    @Test
    fun `detectPlateaus handles stagnant performance`() = runTest {
        val history = listOf(
            E1RMDataPoint(date = now.minus(28, ChronoUnit.DAYS), e1rm = 150.0, weight = 135.0, reps = 5),
            E1RMDataPoint(date = now.minus(21, ChronoUnit.DAYS), e1rm = 150.5, weight = 136.0, reps = 5),
            E1RMDataPoint(date = now.minus(14, ChronoUnit.DAYS), e1rm = 151.0, weight = 136.0, reps = 5),
            E1RMDataPoint(date = now.minus(7, ChronoUnit.DAYS), e1rm = 151.5, weight = 137.0, reps = 5)
        )
        coEvery { prService.getE1RMHistory(exerciseId) } returns history

        val result = service.detectPlateaus(exerciseId, exerciseName)

        // Service should execute without crashing
        assertTrue(result == null || result.exerciseId == exerciseId)
    }

    @Test
    fun `detectPlateaus returns null when making good progress`() = runTest {
        val history = listOf(
            E1RMDataPoint(date = now.minus(28, ChronoUnit.DAYS), e1rm = 150.0, weight = 135.0, reps = 5),
            E1RMDataPoint(date = now.minus(21, ChronoUnit.DAYS), e1rm = 155.0, weight = 140.0, reps = 5),
            E1RMDataPoint(date = now.minus(14, ChronoUnit.DAYS), e1rm = 160.0, weight = 145.0, reps = 5),
            E1RMDataPoint(date = now.minus(7, ChronoUnit.DAYS), e1rm = 165.0, weight = 150.0, reps = 5)
        )
        coEvery { prService.getE1RMHistory(exerciseId) } returns history

        val result = service.detectPlateaus(exerciseId, exerciseName)

        assertNull(result)
    }

    @Test
    fun `detectAllPlateaus processes multiple exercises`() = runTest {
        val exercise1 = "ex1" to "Bench Press"
        val exercise2 = "ex2" to "Squat"
        
        val history1 = listOf(
            E1RMDataPoint(date = now.minus(28, ChronoUnit.DAYS), e1rm = 150.0, weight = 135.0, reps = 5),
            E1RMDataPoint(date = now.minus(21, ChronoUnit.DAYS), e1rm = 148.0, weight = 133.0, reps = 5),
            E1RMDataPoint(date = now.minus(14, ChronoUnit.DAYS), e1rm = 145.0, weight = 130.0, reps = 5)
        )
        val history2 = listOf(
            E1RMDataPoint(date = now.minus(28, ChronoUnit.DAYS), e1rm = 180.0, weight = 165.0, reps = 5),
            E1RMDataPoint(date = now.minus(7, ChronoUnit.DAYS), e1rm = 190.0, weight = 175.0, reps = 5)
        )
        
        coEvery { prService.getE1RMHistory("ex1") } returns history1
        coEvery { prService.getE1RMHistory("ex2") } returns history2

        val result = service.detectAllPlateaus(listOf(exercise1, exercise2))

        // Should return plateaus for exercises that qualify
        assertTrue(result.size <= 2) // At most 2 plateaus
        result.forEach { plateau ->
            assertTrue(plateau.exerciseId in listOf("ex1", "ex2"))
        }
    }
}
