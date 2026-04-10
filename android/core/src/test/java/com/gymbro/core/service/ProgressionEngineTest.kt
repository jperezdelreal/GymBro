package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.preferences.UserPreferences.TrainingPhase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ProgressionEngineTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var engine: ProgressionEngine

    private val exerciseId = "bench-press-id"
    private val workoutId = "workout-1"

    @Before
    fun setup() {
        workoutDao = mockk()
        engine = ProgressionEngine(workoutDao)
    }

    @Test
    fun `returns null when no history exists`() = runTest {
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()
        assertNull(engine.getSuggestion(exerciseId))
    }

    @Test
    fun `suggests progression when all sets RPE 7 or below`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 6.0),
            createSet(weight = 100.0, rpe = 7.0),
            createSet(weight = 100.0, rpe = 7.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        assertEquals(102.5, result!!.suggestedWeightKg, 0.01)
        assertEquals(ProgressionEngine.ProgressionReason.PROGRESS, result.reason)
    }

    @Test
    fun `suggests regression when last 2 sets at RPE 10`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 10.0),
            createSet(weight = 100.0, rpe = 10.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        assertEquals(95.0, result!!.suggestedWeightKg, 0.01)
        assertEquals(ProgressionEngine.ProgressionReason.REGRESS, result.reason)
    }

    @Test
    fun `suggests maintain when RPE is moderate (8-9)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 8.5),
            createSet(weight = 100.0, rpe = 9.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        assertEquals(100.0, result!!.suggestedWeightKg, 0.01)
        assertEquals(ProgressionEngine.ProgressionReason.MAINTAIN, result.reason)
    }

    @Test
    fun `returns NO_DATA when no RPE logged`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = null),
            createSet(weight = 100.0, rpe = null),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        assertEquals(100.0, result!!.suggestedWeightKg, 0.01)
        assertEquals(ProgressionEngine.ProgressionReason.NO_DATA, result.reason)
    }

    @Test
    fun `only considers most recent workout sets`() = runTest {
        val sets = listOf(
            // Old workout — RPE was high
            createSet(weight = 90.0, rpe = 10.0, workoutId = "workout-old"),
            createSet(weight = 90.0, rpe = 10.0, workoutId = "workout-old"),
            // Recent workout — RPE was easy
            createSet(weight = 100.0, rpe = 6.0, workoutId = workoutId),
            createSet(weight = 100.0, rpe = 7.0, workoutId = workoutId),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.PROGRESS, result!!.reason)
        assertEquals(102.5, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `regression rounds to nearest 2_5 kg`() = runTest {
        val sets = listOf(
            createSet(weight = 87.5, rpe = 10.0),
            createSet(weight = 87.5, rpe = 10.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        // 87.5 * 0.95 = 83.125, rounded to nearest 2.5 = 82.5
        assertEquals(82.5, result!!.suggestedWeightKg, 0.01)
    }

    @Test
    fun `skips warmup sets`() = runTest {
        val sets = listOf(
            createSet(weight = 60.0, rpe = 5.0, isWarmup = true),
            createSet(weight = 100.0, rpe = 7.0),
            createSet(weight = 100.0, rpe = 7.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.PROGRESS, result!!.reason)
    }

    // ── Phase-aware progression tests (Issue #414) ──

    @Test
    fun `BULK phase - progresses at RPE 8 (more aggressive)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 7.5),
            createSet(weight = 100.0, rpe = 8.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.BULK)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.PROGRESS, result!!.reason)
        assertEquals(102.5, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `BULK phase - maintains at RPE 9 (not easy enough to progress)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 9.0),
            createSet(weight = 100.0, rpe = 9.0),
            createSet(weight = 100.0, rpe = 9.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.BULK)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.MAINTAIN, result!!.reason)
        assertEquals(100.0, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `BULK phase - regresses at RPE 10 (last 2 sets)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 10.0),
            createSet(weight = 100.0, rpe = 10.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.BULK)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.REGRESS, result!!.reason)
        assertEquals(95.0, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `CUT phase - progresses only at RPE 6 or below (conservative)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 5.0),
            createSet(weight = 100.0, rpe = 6.0),
            createSet(weight = 100.0, rpe = 6.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.CUT)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.PROGRESS, result!!.reason)
        assertEquals(102.5, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `CUT phase - maintains at RPE 7-8 (would progress in other phases)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 7.0),
            createSet(weight = 100.0, rpe = 7.0),
            createSet(weight = 100.0, rpe = 7.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.CUT)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.MAINTAIN, result!!.reason)
        assertEquals(100.0, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `CUT phase - regresses at RPE 9 (earlier than other phases)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 9.0),
            createSet(weight = 100.0, rpe = 9.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.CUT)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.REGRESS, result!!.reason)
        assertEquals(95.0, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `MAINTENANCE phase - progresses at RPE 7 or below (default behavior)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 6.0),
            createSet(weight = 100.0, rpe = 7.0),
            createSet(weight = 100.0, rpe = 7.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.MAINTENANCE)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.PROGRESS, result!!.reason)
        assertEquals(102.5, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `MAINTENANCE phase - maintains at RPE 8-9`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 8.5),
            createSet(weight = 100.0, rpe = 9.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.MAINTENANCE)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.MAINTAIN, result!!.reason)
        assertEquals(100.0, result.suggestedWeightKg, 0.01)
    }

    @Test
    fun `MAINTENANCE phase - regresses at RPE 10 (last 2 sets)`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, rpe = 8.0),
            createSet(weight = 100.0, rpe = 10.0),
            createSet(weight = 100.0, rpe = 10.0),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = engine.getSuggestion(exerciseId, TrainingPhase.MAINTENANCE)
        assertNotNull(result)
        assertEquals(ProgressionEngine.ProgressionReason.REGRESS, result!!.reason)
        assertEquals(95.0, result.suggestedWeightKg, 0.01)
    }

    private fun createSet(
        weight: Double,
        rpe: Double?,
        workoutId: String = this.workoutId,
        isWarmup: Boolean = false,
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exerciseId = exerciseId,
            setNumber = 1,
            weight = weight,
            reps = 8,
            rpe = rpe,
            isWarmup = isWarmup,
            completedAt = System.currentTimeMillis(),
        )
    }
}
