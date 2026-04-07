import Foundation
import SwiftData

@Model
public final class SupersetGroup {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    @Relationship(deleteRule: .nullify, inverse: \ExerciseSet.supersetGroup)
    public var sets: [ExerciseSet]
    
    @Relationship(deleteRule: .nullify)
    public var workout: Workout?
    
    /// Exercises in this superset, in order
    public var exerciseOrder: [UUID]
    
    public init(
        id: UUID = UUID(),
        workout: Workout? = nil,
        exerciseOrder: [UUID] = []
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.workout = workout
        self.sets = []
        self.exerciseOrder = exerciseOrder
    }
    
    /// Exercises involved in this superset (unique, ordered)
    public var exercises: [Exercise] {
        var seen = Set<UUID>()
        var result: [Exercise] = []
        
        for exerciseId in exerciseOrder {
            if let set = sets.first(where: { $0.exercise?.id == exerciseId }),
               let exercise = set.exercise,
               seen.insert(exercise.id).inserted {
                result.append(exercise)
            }
        }
        
        return result
    }
    
    /// Check if all exercises in the superset have completed their current set
    public func isRoundComplete(setNumber: Int) -> Bool {
        let expectedExerciseCount = exerciseOrder.count
        let completedCount = sets.filter { set in
            set.setNumber == setNumber && 
            set.completedAt != nil &&
            exerciseOrder.contains(set.exercise?.id ?? UUID())
        }.count
        
        return completedCount == expectedExerciseCount
    }
}
