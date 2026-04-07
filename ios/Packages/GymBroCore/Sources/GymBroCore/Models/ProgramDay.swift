import Foundation
import SwiftData

@Model
public final class ProgramDay {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    public var dayNumber: Int
    public var name: String
    public var dayDescription: String
    
    @Relationship(deleteRule: .nullify)
    public var program: Program?
    
    @Relationship(deleteRule: .cascade, inverse: \PlannedExercise.programDay)
    public var plannedExercises: [PlannedExercise]
    
    @Relationship(deleteRule: .cascade, inverse: \ProgramWeek.programDay)
    public var weeks: [ProgramWeek]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.programDay)
    public var workouts: [Workout]
    
    public init(
        id: UUID = UUID(),
        dayNumber: Int,
        name: String,
        dayDescription: String = "",
        program: Program? = nil
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.dayNumber = dayNumber
        self.name = name
        self.dayDescription = dayDescription
        self.program = program
        self.plannedExercises = []
        self.weeks = []
        self.workouts = []
    }
}
