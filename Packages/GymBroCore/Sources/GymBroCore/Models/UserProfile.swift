import Foundation
import SwiftData

@Model
final class UserProfile {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    
    // User preferences
    var unitSystem: UnitSystem
    var experienceLevel: ExperienceLevel
    
    // Body metrics history
    @Relationship(deleteRule: .cascade, inverse: \BodyweightEntry.userProfile)
    var bodyweightHistory: [BodyweightEntry]
    
    // Default rest timer preferences
    var defaultRestSeconds: Int
    
    init(
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

enum UnitSystem: String, Codable {
    case metric
    case imperial
}

enum ExperienceLevel: String, Codable {
    case beginner
    case intermediate
    case advanced
    case elite
}

@Model
final class BodyweightEntry {
    var id: UUID
    var date: Date
    var weightKg: Double
    
    @Relationship(deleteRule: .nullify)
    var userProfile: UserProfile?
    
    init(id: UUID = UUID(), date: Date, weightKg: Double) {
        self.id = id
        self.date = date
        self.weightKg = weightKg
    }
    
    func weightInUnit(_ unit: UnitSystem) -> Double {
        switch unit {
        case .metric:
            return weightKg
        case .imperial:
            return weightKg * 2.20462
        }
    }
}
