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
    public var targetAudience: String
    public var expectedOutcome: String
    public var progressionScheme: String
    public var startDate: Date?
    
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
        isCustom: Bool = true,
        targetAudience: String = "",
        expectedOutcome: String = "",
        progressionScheme: String = "",
        startDate: Date? = nil
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
        self.targetAudience = targetAudience
        self.expectedOutcome = expectedOutcome
        self.progressionScheme = progressionScheme
        self.startDate = startDate
        self.days = []
        self.workouts = []
    }
    
    /// Current week number based on startDate (1-indexed).
    public var currentWeekNumber: Int {
        guard let startDate else { return 1 }
        let daysSinceStart = Calendar.current.dateComponents([.day], from: startDate, to: Date()).day ?? 0
        return min((daysSinceStart / 7) + 1, durationWeeks)
    }
    
    /// The ProgramDay for today based on the active schedule rotation.
    public var todaysProgramDay: ProgramDay? {
        guard isActive, let startDate else { return nil }
        let daysSinceStart = Calendar.current.dateComponents([.day], from: startDate, to: Date()).day ?? 0
        let sortedDays = days.sorted { $0.dayNumber < $1.dayNumber }
        guard !sortedDays.isEmpty else { return nil }
        let dayIndex = daysSinceStart % sortedDays.count
        return sortedDays[dayIndex]
    }
    
    /// Difficulty level derived from periodization and frequency.
    public var difficulty: String {
        switch (periodizationType, frequencyPerWeek) {
        case (.linear, 1...3): return "Beginner"
        case (.linear, _): return "Intermediate"
        case (.undulating, _): return "Intermediate"
        case (.block, _): return "Advanced"
        case (.autoregulated, _): return "Advanced"
        }
    }
}

public enum PeriodizationType: String, Codable {
    case linear
    case undulating
    case block
    case autoregulated
    
    public var displayName: String {
        switch self {
        case .linear: return "Linear"
        case .undulating: return "Undulating"
        case .block: return "Block"
        case .autoregulated: return "Autoregulated"
        }
    }
}
