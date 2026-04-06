import Foundation
import SwiftData

@Model
final class Workout {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    
    var date: Date
    var startTime: Date?
    var endTime: Date?
    var notes: String
    
    @Relationship(deleteRule: .cascade, inverse: \ExerciseSet.workout)
    var sets: [ExerciseSet]
    
    @Relationship(deleteRule: .nullify)
    var program: Program?
    
    @Relationship(deleteRule: .nullify)
    var programDay: ProgramDay?
    
    init(
        id: UUID = UUID(),
        date: Date = Date(),
        notes: String = "",
        program: Program? = nil,
        programDay: ProgramDay? = nil
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.date = date
        self.notes = notes
        self.sets = []
        self.program = program
        self.programDay = programDay
    }
    
    var duration: TimeInterval? {
        guard let start = startTime, let end = endTime else { return nil }
        return end.timeIntervalSince(start)
    }
    
    var totalVolume: Double {
        sets.reduce(0) { $0 + $1.volume }
    }
    
    var totalSets: Int {
        sets.filter { !$0.isWarmup }.count
    }
}
