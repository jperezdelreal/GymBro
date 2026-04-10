package com.gymbro.core.voice

import com.gymbro.core.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class VoiceInputParserTest {

    private lateinit var parser: VoiceInputParser

    @Before
    fun setup() {
        parser = VoiceInputParser()
    }

    @Test
    fun `parse weight and reps in English`() {
        val result = parser.parse("80 kilos 8 reps")
        assertNotNull(result)
        assertEquals(80.0, result!!.weight, 0.01)
        assertEquals(8, result.reps)
        assertEquals(UserPreferences.WeightUnit.KG, result.unit)
    }

    @Test
    fun `parse weight and reps in Spanish`() {
        val result = parser.parse("80 kilos 8 repeticiones")
        assertNotNull(result)
        assertEquals(80.0, result!!.weight, 0.01)
        assertEquals(8, result.reps)
        assertEquals(UserPreferences.WeightUnit.KG, result.unit)
    }

    @Test
    fun `parse x format`() {
        val result = parser.parse("100x5")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
    }

    @Test
    fun `parse for format`() {
        val result = parser.parse("100 for 5")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
    }

    @Test
    fun `parse at format`() {
        val result = parser.parse("5 reps at 100")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
    }

    @Test
    fun `parse pounds unit`() {
        val result = parser.parse("225 pounds 3 reps")
        assertNotNull(result)
        assertEquals(225.0, result!!.weight, 0.01)
        assertEquals(3, result.reps)
        assertEquals(UserPreferences.WeightUnit.LBS, result.unit)
    }

    @Test
    fun `parse lbs unit`() {
        val result = parser.parse("135 lbs 10 reps")
        assertNotNull(result)
        assertEquals(135.0, result!!.weight, 0.01)
        assertEquals(10, result.reps)
        assertEquals(UserPreferences.WeightUnit.LBS, result.unit)
    }

    @Test
    fun `parse uses default unit when none specified`() {
        val result = parser.parse("100 for 5", defaultUnit = UserPreferences.WeightUnit.LBS)
        assertNotNull(result)
        assertEquals(UserPreferences.WeightUnit.LBS, result!!.unit)
    }

    @Test
    fun `invalid input returns null`() {
        assertNull(parser.parse(""))
        assertNull(parser.parse("hello world"))
        assertNull(parser.parse("no numbers here"))
    }

    @Test
    fun `single number with reps context returns reps only — null result`() {
        val result = parser.parse("5 reps")
        assertNull(result)
    }

    @Test
    fun `formatConfirmation formats kg correctly`() {
        val input = ParsedVoiceInput(weight = 100.0, reps = 5, unit = UserPreferences.WeightUnit.KG)
        assertEquals("100kg × 5", parser.formatConfirmation(input))
    }

    @Test
    fun `formatConfirmation formats lbs correctly`() {
        val input = ParsedVoiceInput(weight = 225.0, reps = 3, unit = UserPreferences.WeightUnit.LBS)
        assertEquals("225lbs × 3", parser.formatConfirmation(input))
    }

    @Test
    fun `parse decimal weight`() {
        val result = parser.parse("82.5 kg 6 reps")
        assertNotNull(result)
        assertEquals(82.5, result!!.weight, 0.01)
        assertEquals(6, result.reps)
    }

    @Test
    fun `heuristic assigns larger number as weight`() {
        val result = parser.parse("100 5")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
    }

    // --- RPE voice parsing tests (#419) ---

    @Test
    fun `parse RPE from digit`() {
        val result = parser.parse("100 kilos 5 reps RPE 8")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
        assertEquals(8.0, result.rpe!!, 0.01)
    }

    @Test
    fun `parse RPE from decimal digit`() {
        val result = parser.parse("80 kg 6 reps rpe 8.5")
        assertNotNull(result)
        assertEquals(80.0, result!!.weight, 0.01)
        assertEquals(6, result.reps)
        assertEquals(8.5, result.rpe!!, 0.01)
    }

    @Test
    fun `parse RPE from Spanish word`() {
        val result = parser.parse("100 kilos 5 reps RPE ocho")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
        assertEquals(8.0, result.rpe!!, 0.01)
    }

    @Test
    fun `parse RPE from English word`() {
        val result = parser.parse("100x5 rpe nine")
        assertNotNull(result)
        assertEquals(100.0, result!!.weight, 0.01)
        assertEquals(5, result.reps)
        assertEquals(9.0, result.rpe!!, 0.01)
    }

    @Test
    fun `RPE is null when not mentioned`() {
        val result = parser.parse("100 kg 5 reps")
        assertNotNull(result)
        assertNull(result!!.rpe)
    }

    @Test
    fun `RPE out of range is ignored`() {
        val result = parser.parse("100 kg 5 reps rpe 15")
        assertNotNull(result)
        assertNull(result!!.rpe)
    }

    @Test
    fun `formatConfirmation includes RPE when present`() {
        val input = ParsedVoiceInput(weight = 100.0, reps = 5, unit = UserPreferences.WeightUnit.KG, rpe = 8.0)
        assertEquals("100kg × 5 @ RPE 8", parser.formatConfirmation(input))
    }

    @Test
    fun `formatConfirmation excludes RPE when null`() {
        val input = ParsedVoiceInput(weight = 100.0, reps = 5, unit = UserPreferences.WeightUnit.KG, rpe = null)
        assertEquals("100kg × 5", parser.formatConfirmation(input))
    }
}
