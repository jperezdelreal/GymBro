import Foundation
import SwiftData

/// Daily readiness score computed from HealthKit signals and training load.
/// Cached in SwiftData so the dashboard can render instantly without recomputing.
@Model
public final class ReadinessScore {
    public var id: UUID
    public var date: Date
    public var overallScore: Double
    public var sleepScore: Double
    public var hrvScore: Double
    public var restingHRScore: Double
    public var trainingLoadScore: Double
    public var subjectiveScore: Double?
    public var recommendation: String
    public var label: ReadinessLabel
    public var createdAt: Date

    public init(
        id: UUID = UUID(),
        date: Date,
        overallScore: Double,
        sleepScore: Double,
        hrvScore: Double,
        restingHRScore: Double,
        trainingLoadScore: Double,
        subjectiveScore: Double? = nil,
        recommendation: String,
        label: ReadinessLabel,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.date = date
        self.overallScore = overallScore
        self.sleepScore = sleepScore
        self.hrvScore = hrvScore
        self.restingHRScore = restingHRScore
        self.trainingLoadScore = trainingLoadScore
        self.subjectiveScore = subjectiveScore
        self.recommendation = recommendation
        self.label = label
        self.createdAt = createdAt
    }
}

/// Readiness classification with thresholds per AI_ML_APPROACH.md.
public enum ReadinessLabel: String, Codable, CaseIterable, Sendable {
    case excellent
    case good
    case moderate
    case poor

    public var displayName: String {
        switch self {
        case .excellent: return "Excellent"
        case .good: return "Good"
        case .moderate: return "Moderate"
        case .poor: return "Poor"
        }
    }

    public static func from(score: Double) -> ReadinessLabel {
        switch score {
        case 90...100: return .excellent
        case 70..<90: return .good
        case 50..<70: return .moderate
        default: return .poor
        }
    }
}

/// Optional daily subjective check-in (1-5 scale per factor).
@Model
public final class SubjectiveCheckIn {
    public var id: UUID
    public var date: Date
    public var energy: Int
    public var soreness: Int
    public var motivation: Int
    public var createdAt: Date

    public init(
        id: UUID = UUID(),
        date: Date,
        energy: Int,
        soreness: Int,
        motivation: Int,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.date = date
        self.energy = min(max(energy, 1), 5)
        self.soreness = min(max(soreness, 1), 5)
        self.motivation = min(max(motivation, 1), 5)
        self.createdAt = createdAt
    }

    /// Normalized 0-100 score from the three subjective factors.
    /// Soreness is inverted (5 = very sore = bad recovery).
    public var normalizedScore: Double {
        let energyNorm = Double(energy - 1) / 4.0
        let sorenessNorm = Double(5 - soreness) / 4.0
        let motivationNorm = Double(motivation - 1) / 4.0
        return ((energyNorm + sorenessNorm + motivationNorm) / 3.0) * 100.0
    }
}
