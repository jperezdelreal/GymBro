package com.gymbro.core.model

import com.gymbro.core.preferences.UserPreferences
import org.junit.Assert.*
import org.junit.Test

class TrainingSplitTest {

    @Test
    fun `selectOptimalSplit returns Full Body for 2 days per week`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 2,
            goal = UserPreferences.TrainingGoal.HYPERTROPHY
        )
        assertEquals(TrainingSplit.FULL_BODY, split)
    }

    @Test
    fun `selectOptimalSplit returns Full Body for 3 days hypertrophy`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 3,
            goal = UserPreferences.TrainingGoal.HYPERTROPHY
        )
        assertEquals(TrainingSplit.FULL_BODY, split)
    }

    @Test
    fun `selectOptimalSplit returns Powerlifting 3-day for 3 days powerlifting`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 3,
            goal = UserPreferences.TrainingGoal.POWERLIFTING
        )
        assertEquals(TrainingSplit.POWERLIFTING_3DAY, split)
    }

    @Test
    fun `selectOptimalSplit returns Upper Lower for 4 days per week`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 4,
            goal = UserPreferences.TrainingGoal.HYPERTROPHY
        )
        assertEquals(TrainingSplit.UPPER_LOWER, split)
    }

    @Test
    fun `selectOptimalSplit returns PPLUL for 5 days per week`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 5,
            goal = UserPreferences.TrainingGoal.HYPERTROPHY
        )
        assertEquals(TrainingSplit.PPLUL, split)
    }

    @Test
    fun `selectOptimalSplit returns PPL for 6 days per week`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 6,
            goal = UserPreferences.TrainingGoal.HYPERTROPHY
        )
        assertEquals(TrainingSplit.PPL, split)
    }

    @Test
    fun `selectOptimalSplit returns PPL for 7 days per week`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 7,
            goal = UserPreferences.TrainingGoal.HYPERTROPHY
        )
        assertEquals(TrainingSplit.PPL, split)
    }

    @Test
    fun `selectOptimalSplit returns Full Body for 1 day per week`() {
        val split = TrainingSplit.selectOptimalSplit(
            daysPerWeek = 1,
            goal = UserPreferences.TrainingGoal.GENERAL_FITNESS
        )
        assertEquals(TrainingSplit.FULL_BODY, split)
    }

    @Test
    fun `display names are correct`() {
        assertEquals("Full Body", TrainingSplit.FULL_BODY.displayName)
        assertEquals("Upper/Lower", TrainingSplit.UPPER_LOWER.displayName)
        assertEquals("Push/Pull/Legs", TrainingSplit.PPL.displayName)
        assertEquals("PPL + Upper/Lower", TrainingSplit.PPLUL.displayName)
        assertEquals("Powerlifting 3-Day", TrainingSplit.POWERLIFTING_3DAY.displayName)
    }
}
