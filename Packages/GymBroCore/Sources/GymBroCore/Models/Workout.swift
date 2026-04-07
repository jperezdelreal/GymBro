import Foundation
import SwiftData

@Model
public public final class Workout {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    public var date: Date
    public var startTime: Date?
    public var endTime: Date?
    public var notes: String
    public var isActive: Bool
    public var isCancelled: Bool
    
    @Relationship(deleteRule: .cascade, inverse: \ExerciseSet.workout)
    public var sets: [ExerciseSet]
    
    @Relationship(deleteRule: .nullify)
    public var program: Program?
    
    @Relationship(deleteRule: .nullify)
    public var programDay: ProgramDay?
    
    public init(
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
        self.isActive = false
        self.isCancelled = false
        self.sets = []
        self.program = program
        self.programDay = programDay
    }
    
    public var duration: TimeInterval? {
        guard let start = startTime, let end = endTime else { return nil }
        return end.timeIntervalSince(start)
    }
    
    public var totalVolume: Double {
        sets.reduce(0) { $0 + $1.volume }
    }
    
    public var totalSets: Int {
        sets.filter { !$0.isWarmup }.count
    }
    
    /// Unique exercises used in this workout, preserving insertion order.
    public var exercises: [Exercise] {
        var seen = Set<UUID>()
        var result: [Exercise] = []
        for set in sets.sorted(by: { $0.createdAt < $1.createdAt }) {
            if let exercise = set.exercise, seen.insert(exercise.id).inserted {
                result.append(exercise)
            }
        }
        return result
    }
}
