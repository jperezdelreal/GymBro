import Foundation
import SwiftData

@Model
public final class Program {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    public var name: String
    public var programDescription: String
    public var durationWeeks: Int
    public var frequencyPerWeek: Int
    public var periodizationType: PeriodizationType
    public var isActive: Bool
    public var isCustom: Bool
    
    @Relationship(deleteRule: .cascade, inverse: \ProgramDay.program)
    public var days: [ProgramDay]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.program)
    public var workouts: [Workout]
    
    public init(
        id: UUID = UUID(),
        name: String,
        programDescription: String = "",
        durationWeeks: Int,
        frequencyPerWeek: Int,
        periodizationType: PeriodizationType = .linear,
        isActive: Bool = false,
        isCustom: Bool = true
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.name = name
        self.programDescription = programDescription
        self.durationWeeks = durationWeeks
        self.frequencyPerWeek = frequencyPerWeek
        self.periodizationType = periodizationType
        self.isActive = isActive
        self.isCustom = isCustom
        self.days = []
        self.workouts = []
    }
}

public enum PeriodizationType: String, Codable {
    case linear
    case undulating
    case block
    case autoregulated
}
