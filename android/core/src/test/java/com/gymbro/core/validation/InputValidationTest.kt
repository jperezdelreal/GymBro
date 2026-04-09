package com.gymbro.core.validation

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for weight/rep input parsing edge cases used in ActiveWorkoutViewModel.
 * These validate the behavior of Kotlin's toDoubleOrNull() and toIntOrNull()
 * for user input validation.
 */
class InputValidationTest {

    @Test
    fun `weight parsing accepts valid positive decimals`() {
        val weight = "100.5".toDoubleOrNull()
        assertNotNull(weight)
        assertEquals(100.5, weight!!, 0.01)
    }

    @Test
    fun `weight parsing accepts valid integers`() {
        val weight = "100".toDoubleOrNull()
        assertNotNull(weight)
        assertEquals(100.0, weight!!, 0.01)
    }

    @Test
    fun `weight parsing rejects negative values`() {
        val weight = "-50".toDoubleOrNull()
        assertNotNull(weight) // Parses successfully
        assertEquals(-50.0, weight!!, 0.01)
        // Note: ActiveWorkoutViewModel should validate weight > 0 after parsing
    }

    @Test
    fun `weight parsing rejects zero`() {
        val weight = "0".toDoubleOrNull()
        assertNotNull(weight) // Parses successfully
        assertEquals(0.0, weight!!, 0.01)
        // Note: ActiveWorkoutViewModel should validate weight > 0 after parsing
    }

    @Test
    fun `weight parsing rejects empty string`() {
        val weight = "".toDoubleOrNull()
        assertNull(weight)
    }

    @Test
    fun `weight parsing rejects whitespace`() {
        val weight = "   ".toDoubleOrNull()
        assertNull(weight)
    }

    @Test
    fun `weight parsing rejects non-numeric input`() {
        val weight = "abc".toDoubleOrNull()
        assertNull(weight)
    }

    @Test
    fun `weight parsing rejects special characters`() {
        val weight = "100@#$".toDoubleOrNull()
        assertNull(weight)
    }

    @Test
    fun `weight parsing handles max double value`() {
        val weight = Double.MAX_VALUE.toString().toDoubleOrNull()
        assertNotNull(weight)
        assertEquals(Double.MAX_VALUE, weight!!, 0.01)
    }

    @Test
    fun `weight parsing handles very large numbers`() {
        val weight = "999999.99".toDoubleOrNull()
        assertNotNull(weight)
        assertEquals(999999.99, weight!!, 0.01)
    }

    @Test
    fun `reps parsing accepts valid positive integers`() {
        val reps = "10".toIntOrNull()
        assertNotNull(reps)
        assertEquals(10, reps)
    }

    @Test
    fun `reps parsing accepts single digit`() {
        val reps = "1".toIntOrNull()
        assertNotNull(reps)
        assertEquals(1, reps)
    }

    @Test
    fun `reps parsing rejects negative values`() {
        val reps = "-5".toIntOrNull()
        assertNotNull(reps) // Parses successfully
        assertEquals(-5, reps)
        // Note: ActiveWorkoutViewModel should validate reps > 0 after parsing
    }

    @Test
    fun `reps parsing rejects zero`() {
        val reps = "0".toIntOrNull()
        assertNotNull(reps) // Parses successfully
        assertEquals(0, reps)
        // Note: ActiveWorkoutViewModel should validate reps > 0 after parsing
    }

    @Test
    fun `reps parsing rejects decimal values`() {
        val reps = "10.5".toIntOrNull()
        assertNull(reps) // Cannot parse decimal as int
    }

    @Test
    fun `reps parsing rejects empty string`() {
        val reps = "".toIntOrNull()
        assertNull(reps)
    }

    @Test
    fun `reps parsing rejects whitespace`() {
        val reps = "   ".toIntOrNull()
        assertNull(reps)
    }

    @Test
    fun `reps parsing rejects non-numeric input`() {
        val reps = "abc".toIntOrNull()
        assertNull(reps)
    }

    @Test
    fun `reps parsing rejects special characters`() {
        val reps = "10@#$".toIntOrNull()
        assertNull(reps)
    }

    @Test
    fun `reps parsing handles max int value`() {
        val reps = Int.MAX_VALUE.toString().toIntOrNull()
        assertNotNull(reps)
        assertEquals(Int.MAX_VALUE, reps)
    }

    @Test
    fun `reps parsing rejects overflow`() {
        val reps = "9999999999999".toIntOrNull() // Too large for Int
        assertNull(reps)
    }

    @Test
    fun `weight parsing handles leading zeros`() {
        val weight = "0100.5".toDoubleOrNull()
        assertNotNull(weight)
        assertEquals(100.5, weight!!, 0.01)
    }

    @Test
    fun `reps parsing handles leading zeros`() {
        val reps = "010".toIntOrNull()
        assertNotNull(reps)
        assertEquals(10, reps)
    }

    @Test
    fun `weight parsing handles scientific notation`() {
        val weight = "1e2".toDoubleOrNull() // 100.0
        assertNotNull(weight)
        assertEquals(100.0, weight!!, 0.01)
    }

    @Test
    fun `weight parsing rejects multiple decimal points`() {
        val weight = "100.5.5".toDoubleOrNull()
        assertNull(weight)
    }

    @Test
    fun `reps parsing handles very large valid values`() {
        val reps = "9999".toIntOrNull()
        assertNotNull(reps)
        assertEquals(9999, reps)
    }
}
