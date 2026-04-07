import Foundation
import SwiftData

@Model
public final class PlannedExercise {
    public var id: UUID
    public var order: Int
    
    @Relationship(deleteRule: .nullify)
    public var exercise: Exercise?
    
    @Relationship(deleteRule: .nullify)
    public var programDay: ProgramDay?
    
    @Relationship(deleteRule: .nullify)
    public var programWeek: ProgramWeek?
    
    public var targetSets: Int
    public var targetReps: String
    public var targetRPE: Double?
    public var notes: String
    
    public init(
        id: UUID = UUID(),
        order: Int,
        exercise: Exercise? = nil,
        programDay: ProgramDay? = nil,
        programWeek: ProgramWeek? = nil,
        targetSets: Int,
        targetReps: String,
        targetRPE: Double? = nil,
        notes: String = ""
    ) {
        self.id = id
        self.order = order
        self.exercise = exercise
        self.programDay = programDay
        self.programWeek = programWeek
        self.targetSets = targetSets
        self.targetReps = targetReps
        self.targetRPE = targetRPE
        self.notes = notes
    }
}
