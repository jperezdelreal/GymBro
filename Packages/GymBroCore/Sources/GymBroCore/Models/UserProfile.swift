import Foundation
import SwiftData

@Model
public final class UserProfile {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    // User preferences
    public var unitSystem: UnitSystem
    public var experienceLevel: ExperienceLevel
    
    @Relationship(deleteRule: .cascade, inverse: \BodyweightEntry.userProfile)
    public var bodyweightHistory: [BodyweightEntry]
    
    // Default rest timer preferences
    public var defaultRestSeconds: Int
    
    public init(
        id: UUID = UUID(),
        unitSystem: UnitSystem = .metric,
        experienceLevel: ExperienceLevel = .intermediate,
        defaultRestSeconds: Int = 120
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.unitSystem = unitSystem
        self.experienceLevel = experienceLevel
        self.bodyweightHistory = []
        self.defaultRestSeconds = defaultRestSeconds
    }
}

public enum UnitSystem: String, Codable {
    case metric
    case imperial
}

public enum ExperienceLevel: String, Codable {
    case beginner
    case intermediate
    case advanced
    case elite
}

@Model
public final class BodyweightEntry {
    public var id: UUID
    public var date: Date
    public var weightKg: Double
    
    @Relationship(deleteRule: .nullify)
    public var userProfile: UserProfile?
    
    public init(id: UUID = UUID(), date: Date, weightKg: Double) {
        self.id = id
        self.date = date
        self.weightKg = weightKg
    }
    
    public func weightInUnit(_ unit: UnitSystem) -> Double {
        switch unit {
        case .metric:
            return weightKg
        case .imperial:
            return weightKg * 2.20462
        }
    }
}
