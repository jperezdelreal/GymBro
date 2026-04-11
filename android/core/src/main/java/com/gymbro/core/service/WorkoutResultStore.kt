package com.gymbro.core.service

import com.gymbro.core.model.PersonalRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store for workout result data (PRs) that must survive
 * navigation between ActiveWorkout → WorkoutSummary.
 *
 * Using savedStateHandle on previousBackStackEntry fails because
 * popUpTo("active_workout") { inclusive = true } removes that entry
 * before WorkoutSummary reads it.
 */
@Singleton
class WorkoutResultStore @Inject constructor() {

    private var personalRecords: List<PersonalRecord> = emptyList()

    fun setPersonalRecords(prs: List<PersonalRecord>) {
        personalRecords = prs
    }

    fun consumePersonalRecords(): List<PersonalRecord> {
        val prs = personalRecords
        personalRecords = emptyList()
        return prs
    }
}
