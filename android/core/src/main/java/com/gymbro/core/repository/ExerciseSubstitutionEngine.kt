package com.gymbro.core.repository

import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup

/**
 * Exercise substitution engine that finds alternative exercises based on muscle groups,
 * equipment, and movement patterns.
 *
 * Ranking algorithm:
 * - Base score: 100 for matching muscle group + category
 * - +50 for exact equipment match
 * - +30 for equipment in same family (barbell/dumbbell, cable/machine)
 * - +20 for compound exercises (more comprehensive movements)
 * - -10 per equipment tier difference (bodyweight > barbell/dumbbell > cable/machine > other)
 */
class ExerciseSubstitutionEngine @javax.inject.Inject constructor() {

    data class ScoredExercise(
        val exercise: Exercise,
        val score: Int,
        val matchReasons: List<String>
    )

    /**
     * Finds substitute exercises for the given exercise.
     *
     * @param targetExercise The exercise to find substitutes for
     * @param availableExercises Pool of exercises to search from
     * @param availableEquipment Optional equipment filter - if provided, only returns exercises using this equipment
     * @param limit Maximum number of substitutes to return (default: 5)
     * @return List of substitute exercises ranked by similarity
     */
    fun findSubstitutes(
        targetExercise: Exercise,
        availableExercises: List<Exercise>,
        availableEquipment: Set<Equipment>? = null,
        limit: Int = 5
    ): List<Exercise> {
        val candidates = availableExercises
            .filter { it.id != targetExercise.id }
            .filter { it.muscleGroup == targetExercise.muscleGroup }
            .filter { it.category == targetExercise.category }
            .let { exercises ->
                if (availableEquipment != null) {
                    exercises.filter { it.equipment in availableEquipment }
                } else {
                    exercises
                }
            }

        return candidates
            .map { candidate -> scoreExercise(candidate, targetExercise) }
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.exercise }
    }

    /**
     * Finds substitute exercises with detailed scoring information.
     *
     * @param targetExercise The exercise to find substitutes for
     * @param availableExercises Pool of exercises to search from
     * @param availableEquipment Optional equipment filter
     * @param limit Maximum number of substitutes to return (default: 5)
     * @return List of scored exercises with match reasons
     */
    fun findSubstitutesWithScores(
        targetExercise: Exercise,
        availableExercises: List<Exercise>,
        availableEquipment: Set<Equipment>? = null,
        limit: Int = 5
    ): List<ScoredExercise> {
        val candidates = availableExercises
            .filter { it.id != targetExercise.id }
            .filter { it.muscleGroup == targetExercise.muscleGroup }
            .filter { it.category == targetExercise.category }
            .let { exercises ->
                if (availableEquipment != null) {
                    exercises.filter { it.equipment in availableEquipment }
                } else {
                    exercises
                }
            }

        return candidates
            .map { candidate -> scoreExercise(candidate, targetExercise) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun scoreExercise(
        candidate: Exercise,
        target: Exercise
    ): ScoredExercise {
        var score = 100 // Base score for matching muscle group + category
        val reasons = mutableListOf<String>()

        reasons.add("Same ${target.muscleGroup.displayName} muscle group")
        reasons.add("Same ${target.category.displayName} category")

        // Equipment scoring
        if (candidate.equipment == target.equipment) {
            score += 50
            reasons.add("Exact equipment match (${target.equipment.name})")
        } else {
            val equipmentScore = scoreEquipmentSimilarity(candidate.equipment, target.equipment)
            score += equipmentScore
            if (equipmentScore > 0) {
                reasons.add("Similar equipment (${candidate.equipment.name} vs ${target.equipment.name})")
            }
        }

        // Bonus for compound exercises (more bang for your buck)
        if (candidate.category == ExerciseCategory.COMPOUND) {
            score += 20
            reasons.add("Compound movement")
        }

        return ScoredExercise(candidate, score, reasons)
    }

    private fun scoreEquipmentSimilarity(equipment1: Equipment, equipment2: Equipment): Int {
        // Equipment families
        val freeWeights = setOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.KETTLEBELL)
        val machines = setOf(Equipment.CABLE, Equipment.MACHINE)

        return when {
            // Same equipment family
            equipment1 in freeWeights && equipment2 in freeWeights -> 30
            equipment1 in machines && equipment2 in machines -> 30
            
            // Bodyweight is versatile but not ideal substitution
            equipment1 == Equipment.BODYWEIGHT || equipment2 == Equipment.BODYWEIGHT -> 10
            
            // Different families
            else -> 0
        }
    }

    /**
     * Filters exercises by available equipment.
     * Useful for "I don't have a barbell, what can I do instead?" scenarios.
     */
    fun filterByEquipment(
        exercises: List<Exercise>,
        availableEquipment: Set<Equipment>
    ): List<Exercise> {
        return exercises.filter { it.equipment in availableEquipment }
    }

    /**
     * Groups exercises by muscle group and category for batch substitution.
     */
    fun groupExercises(exercises: List<Exercise>): Map<Pair<MuscleGroup, ExerciseCategory>, List<Exercise>> {
        return exercises.groupBy { Pair(it.muscleGroup, it.category) }
    }
}
