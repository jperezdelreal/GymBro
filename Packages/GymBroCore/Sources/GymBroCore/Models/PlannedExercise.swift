import Foundation
import SwiftData

@Model
final class PlannedExercise {
    var id: UUID
    var order: Int
    
    @Relationship(deleteRule: .nullify)
    var exercise: Exercise?
    
    @Relationship(deleteRule: .nullify)
    var programDay: ProgramDay?
    
    var targetSets: Int
    var targetReps: String
    var targetRPE: Double?
    var notes: String
    
    init(
        id: UUID = UUID(),
        order: Int,
        exercise: Exercise? = nil,
        programDay: ProgramDay? = nil,
        targetSets: Int,
        targetReps: String,
        targetRPE: Double? = nil,
        notes: String = ""
    ) {
        self.id = id
        self.order = order
        self.exercise = exercise
        self.programDay = programDay
        self.targetSets = targetSets
        self.targetReps = targetReps
        self.targetRPE = targetRPE
        self.notes = notes
    }
}
