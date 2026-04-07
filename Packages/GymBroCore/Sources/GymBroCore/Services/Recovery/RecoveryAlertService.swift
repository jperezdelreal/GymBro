import Foundation
import os

/// Monitors readiness scores and triggers recovery alerts when scores drop below thresholds.
///
/// Threshold logic (per issue #54):
///   - Score < 50: suggest deload / lighter workout
///   - Score < 30: suggest complete rest day
///
/// Integrates with ReadinessScoreService for scores and NotificationService for delivery.
public final class RecoveryAlertService: Sendable {
    private static let logger = Logger(subsystem: "com.gymbro", category: "RecoveryAlerts")

    /// Alert severity based on readiness score thresholds.
    public enum AlertLevel: String, Sendable, Equatable {
        case deload   // score < 50 — suggest lighter workout
        case restDay  // score < 30 — suggest full rest day
    }

    /// A recovery alert with context for UI and notifications.
    public struct RecoveryAlert: Sendable, Equatable {
        public let level: AlertLevel
        public let readinessScore: Double
        public let title: String
        public let message: String
        public let recommendation: String
        public let date: Date

        public init(level: AlertLevel, readinessScore: Double, title: String, message: String, recommendation: String, date: Date = Date()) {
            self.level = level
            self.readinessScore = readinessScore
            self.title = title
            self.message = message
            self.recommendation = recommendation
            self.date = date
        }
    }

    // Thresholds
    public static let deloadThreshold: Double = 50.0
    public static let restDayThreshold: Double = 30.0

    private let readinessService: ReadinessScoreService

    public init(readinessService: ReadinessScoreService = ReadinessScoreService()) {
        self.readinessService = readinessService
    }

    // MARK: - Alert Evaluation

    /// Evaluate a readiness score and return an alert if thresholds are breached.
    /// Returns nil when the score is 50 or above (no alert needed).
    public func evaluate(score: ReadinessScore) -> RecoveryAlert? {
        if score.overallScore < Self.restDayThreshold {
            return makeRestDayAlert(score: score)
        } else if score.overallScore < Self.deloadThreshold {
            return makeDeloadAlert(score: score)
        }
        return nil
    }

    /// Convenience: calculate readiness from raw input and evaluate in one step.
    public func evaluateFromInput(_ input: ReadinessScoreService.Input, date: Date = Date()) -> RecoveryAlert? {
        let score = readinessService.calculate(from: input, date: date)
        return evaluate(score: score)
    }

    // MARK: - Alert Construction

    private func makeDeloadAlert(score: ReadinessScore) -> RecoveryAlert {
        Self.logger.info("Deload alert triggered — readiness \(score.overallScore, privacy: .public)")
        return RecoveryAlert(
            level: .deload,
            readinessScore: score.overallScore,
            title: "Low Recovery Today",
            message: "Your readiness score is \(Int(score.overallScore)). Consider a lighter session with reduced intensity.",
            recommendation: "Reduce working weights to 50-70% of your normal loads. Focus on technique and controlled tempo.",
            date: score.date
        )
    }

    private func makeRestDayAlert(score: ReadinessScore) -> RecoveryAlert {
        Self.logger.info("Rest day alert triggered — readiness \(score.overallScore, privacy: .public)")
        return RecoveryAlert(
            level: .restDay,
            readinessScore: score.overallScore,
            title: "Recovery Needed",
            message: "Your readiness score is \(Int(score.overallScore)). Your body needs rest to recover.",
            recommendation: "Take a full rest day. Light walking or gentle mobility work only.",
            date: score.date
        )
    }

    // MARK: - Deload Suggestions

    /// Lighter exercise suggestions based on alert level.
    public static func deloadSuggestions(for level: AlertLevel) -> [DeloadSuggestion] {
        switch level {
        case .deload:
            return [
                DeloadSuggestion(
                    name: "Light Compound Work",
                    description: "Perform your planned compounds at 50-60% 1RM for 3×5. Focus on bar speed and technique.",
                    icon: "figure.strengthtraining.traditional",
                    intensityPercent: 55
                ),
                DeloadSuggestion(
                    name: "Accessory Focus",
                    description: "Skip heavy compounds. Do isolation and accessory work at moderate effort (RPE 5-6).",
                    icon: "dumbbell.fill",
                    intensityPercent: 60
                ),
                DeloadSuggestion(
                    name: "Mobility & Technique",
                    description: "Empty bar work, mobility drills, and movement patterns. Address weak points.",
                    icon: "figure.flexibility",
                    intensityPercent: 30
                ),
            ]
        case .restDay:
            return [
                DeloadSuggestion(
                    name: "Complete Rest",
                    description: "No resistance training. Focus on sleep, nutrition, and stress management.",
                    icon: "bed.double.fill",
                    intensityPercent: 0
                ),
                DeloadSuggestion(
                    name: "Light Walking",
                    description: "20-30 minute easy walk. Promotes blood flow without adding training stress.",
                    icon: "figure.walk",
                    intensityPercent: 10
                ),
                DeloadSuggestion(
                    name: "Gentle Mobility",
                    description: "10-15 minutes of stretching and foam rolling. No loaded movements.",
                    icon: "figure.cooldown",
                    intensityPercent: 15
                ),
            ]
        }
    }
}

/// A lighter exercise suggestion shown in the deload UI.
public struct DeloadSuggestion: Identifiable, Sendable, Equatable {
    public let id = UUID()
    public let name: String
    public let description: String
    public let icon: String
    public let intensityPercent: Int

    public init(name: String, description: String, icon: String, intensityPercent: Int) {
        self.name = name
        self.description = description
        self.icon = icon
        self.intensityPercent = intensityPercent
    }
}
