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
    
    // Onboarding data
    public var hasCompletedOnboarding: Bool
    public var trainingGoals: [String]
    public var trainingFrequency: Int
    public var equipmentAvailability: EquipmentType
    public var injuriesOrLimitations: String?
    
    public init(
        id: UUID = UUID(),
        unitSystem: UnitSystem = .metric,
        experienceLevel: ExperienceLevel = .intermediate,
        defaultRestSeconds: Int = 120,
        hasCompletedOnboarding: Bool = false,
        trainingGoals: [String] = [],
        trainingFrequency: Int = 3,
        equipmentAvailability: EquipmentType = .fullGym,
        injuriesOrLimitations: String? = nil
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.unitSystem = unitSystem
        self.experienceLevel = experienceLevel
        self.bodyweightHistory = []
        self.defaultRestSeconds = defaultRestSeconds
        self.hasCompletedOnboarding = hasCompletedOnboarding
        self.trainingGoals = trainingGoals
        self.trainingFrequency = trainingFrequency
        self.equipmentAvailability = equipmentAvailability
        self.injuriesOrLimitations = injuriesOrLimitations
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
    
    public var description: String {
        switch self {
        case .beginner:
            return "New to strength training (< 1 year)"
        case .intermediate:
            return "Regular lifter (1-3 years)"
        case .advanced:
            return "Experienced athlete (3+ years)"
        case .elite:
            return "Competitive athlete / coach"
        }
    }
}

public enum EquipmentType: String, Codable {
    case fullGym
    case homeGym
    case bodyweightOnly
    
    public var displayName: String {
        switch self {
        case .fullGym:
            return "Full gym access"
        case .homeGym:
            return "Home gym (limited equipment)"
        case .bodyweightOnly:
            return "Bodyweight only"
        }
    }
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
