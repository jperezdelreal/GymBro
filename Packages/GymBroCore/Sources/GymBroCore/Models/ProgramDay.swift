import Foundation
import SwiftData

@Model
final class ProgramDay {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    
    var dayNumber: Int
    var name: String
    var dayDescription: String
    
    @Relationship(deleteRule: .nullify)
    var program: Program?
    
    @Relationship(deleteRule: .cascade, inverse: \PlannedExercise.programDay)
    var plannedExercises: [PlannedExercise]
    
    @Relationship(deleteRule: .cascade, inverse: \ProgramWeek.programDay)
    var weeks: [ProgramWeek]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.programDay)
    var workouts: [Workout]
    
    init(
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
