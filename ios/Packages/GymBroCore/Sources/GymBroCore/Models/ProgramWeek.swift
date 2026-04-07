import Foundation
import SwiftData

@Model
public final class ProgramWeek {
    public var id: UUID
    public var weekNumber: Int
    
    @Relationship(deleteRule: .nullify)
    public var programDay: ProgramDay?
    
    @Relationship(deleteRule: .cascade, inverse: \PlannedExercise.programWeek)
    public var plannedExercises: [PlannedExercise]
    
    public init(
        id: UUID = UUID(),
        weekNumber: Int,
        programDay: ProgramDay? = nil
    ) {
        self.id = id
        self.weekNumber = weekNumber
        self.programDay = programDay
        self.plannedExercises = []
    }
}
