import Foundation
import SwiftData

@Observable
public final class SupersetService {
    public static let shared = SupersetService()
    
    private init() {}
    
    /// Create a new superset group linking the given exercises
    public func createSupersetGroup(
        exercises: [Exercise],
        workout: Workout,
        modelContext: ModelContext
    ) -> SupersetGroup {
        let group = SupersetGroup(
            workout: workout,
            exerciseOrder: exercises.map { $0.id }
        )
        
        modelContext.insert(group)
        workout.supersetGroups.append(group)
        
        return group
    }
    
    /// Add a set to an existing superset group
    public func addSetToSuperset(
        set: ExerciseSet,
        group: SupersetGroup
    ) {
        set.supersetGroup = group
        group.sets.append(set)
    }
    
    /// Remove a set from its superset group
    public func removeSetFromSuperset(
        set: ExerciseSet,
        modelContext: ModelContext
    ) {
        if let group = set.supersetGroup {
            group.sets.removeAll { $0.id == set.id }
            set.supersetGroup = nil
            
            // Clean up empty groups
            if group.sets.isEmpty {
                modelContext.delete(group)
            }
        }
    }
    
    /// Find the superset group for a given exercise in a workout
    public func findSupersetGroup(
        for exercise: Exercise,
        in workout: Workout
    ) -> SupersetGroup? {
        workout.supersetGroups.first { group in
            group.exerciseOrder.contains(exercise.id)
        }
    }
    
    /// Get all exercises in the same superset as the given exercise
    public func getSupersetPartners(
        for exercise: Exercise,
        in workout: Workout
    ) -> [Exercise] {
        guard let group = findSupersetGroup(for: exercise, in: workout) else {
            return []
        }
        
        return group.exercises.filter { $0.id != exercise.id }
    }
    
    /// Check if rest timer should start (only after completing full superset round)
    public func shouldStartRestTimer(
        after set: ExerciseSet,
        in workout: Workout
    ) -> Bool {
        // Not in a superset - normal rest timer behavior
        guard let group = set.supersetGroup else {
            return true
        }
        
        // In a superset - only rest after completing the full round
        return group.isRoundComplete(setNumber: set.setNumber)
    }
}
