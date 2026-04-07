import Foundation
import SwiftData

/// Systemic overtraining detection across all exercises.
///
/// Unlike PlateauDetectionService (per-exercise), this monitors training load across the entire program
/// to detect accumulated fatigue, overreaching, and early warning signs of overtraining syndrome.
///
/// **Evidence-based indicators:**
/// - Multi-exercise plateau correlation (Helms et al., 2018)
/// - Volume ramp rate monitoring (Schoenfeld & Grgic, 2018)
/// - RPE drift detection (Helms RPE scale validation)
/// - Performance decline across multiple lifts (Fry et al., 2010)
/// - Integration with readiness score (Bourdon et al., 2017)
///
/// **Design philosophy:**
/// - Heuristics-first, conservative alerts to build trust
/// - Requires at least 4 weeks of data for meaningful signal
/// - False positive rate < 10% (per AI_ML_APPROACH.md Decision #8)
public final class OvertrainingDetectionService {
    
    // MARK: - Thresholds (evidence-based)
    
    /// Multi-exercise plateau: 3+ exercises stalling = systemic issue
    private static let multiExercisePlateauThreshold: Int = 3
    
    /// Volume ramp rate: weekly volume increase > 10% = injury risk
    /// Source: Gabbett, T. (2016). The training-injury prevention paradox
    private static let dangerousVolumeRampRate: Double = 0.10
    
    /// RPE drift: same weight feeling +1 RPE harder over 3+ weeks
    private static let rpeDriftThreshold: Double = 1.0
    private static let rpeDriftMinWeeks: Int = 3
    
    /// Performance decline: e1RM dropping > 5% across 2+ major lifts
    private static let performanceDeclineThreshold: Double = 0.05
    private static let performanceDeclineMinExercises: Int = 2
    
    /// Chronic low readiness: readiness < 60 for 5+ consecutive days + high volume
    private static let lowReadinessThreshold: Double = 60.0
    private static let lowReadinessDays: Int = 5
    
    /// Minimum data requirement: 4 weeks of training history
    private static let minimumWeeksOfData: Int = 4
    
    private let e1rmCalculator: E1RMCalculator
    
    public init(e1rmCalculator: E1RMCalculator = E1RMCalculator()) {
        self.e1rmCalculator = e1rmCalculator
    }
    
    // MARK: - Core Analysis
    
    /// Analyzes overtraining risk from workout history, plateau analyses, and readiness scores.
    /// Returns nil if insufficient data (< 4 weeks).
    public func analyze(
        workouts: [Workout],
        plateauAnalyses: [PlateauAnalysis],
        recentReadinessScores: [ReadinessScore]
    ) -> OvertrainingAnalysis? {
        
        guard hasMinimumDataRequirement(workouts: workouts) else { return nil }
        
        // Run all detection methods
        let multiPlateauSignal = detectMultiExercisePlateau(plateauAnalyses: plateauAnalyses)
        let volumeRampSignal = detectDangerousVolumeRamp(workouts: workouts)
        let rpeDriftSignal = detectRPEDrift(workouts: workouts)
        let performanceDeclineSignal = detectPerformanceDecline(workouts: workouts)
        let chronicFatigueSignal = detectChronicLowReadiness(
            readinessScores: recentReadinessScores,
            workouts: workouts
        )
        
        // Count active signals
        let signals: [OvertrainingSignal] = [
            multiPlateauSignal,
            volumeRampSignal,
            rpeDriftSignal,
            performanceDeclineSignal,
            chronicFatigueSignal
        ].compactMap { $0 }
        
        // Risk stratification: 0 signals = none, 1-2 = moderate, 3+ = high
        let riskLevel = determineRiskLevel(signalCount: signals.count)
        
        // Generate actionable recommendations
        let recommendations = generateRecommendations(
            riskLevel: riskLevel,
            signals: signals
        )
        
        return OvertrainingAnalysis(
            analyzedAt: Date(),
            riskLevel: riskLevel,
            activeSignals: signals,
            recommendations: recommendations
        )
    }
    
    // MARK: - Detection Methods
    
    /// Detects if 3+ exercises are plateaued simultaneously (systemic, not exercise-specific).
    private func detectMultiExercisePlateau(
        plateauAnalyses: [PlateauAnalysis]
    ) -> OvertrainingSignal? {
        let plateauedExercises = plateauAnalyses.filter { $0.isPlateaued }
        
        guard plateauedExercises.count >= Self.multiExercisePlateauThreshold else { return nil }
        
        let exerciseNames = plateauedExercises.map { $0.exerciseName }.joined(separator: ", ")
        
        return OvertrainingSignal(
            type: .multiExercisePlateau,
            severity: .moderate,
            description: "\(plateauedExercises.count) exercises plateaued simultaneously: \(exerciseNames). This suggests systemic fatigue, not exercise-specific stagnation."
        )
    }
    
    /// Detects dangerous weekly volume ramp rate (> 10% week-over-week increase).
    /// Source: Gabbett's acute:chronic workload ratio research.
    private func detectDangerousVolumeRamp(workouts: [Workout]) -> OvertrainingSignal? {
        let weeklyVolumes = calculateWeeklyVolumes(workouts: workouts)
        
        guard weeklyVolumes.count >= 3 else { return nil }
        
        // Check last 3 weeks for dangerous ramp
        let recent = Array(weeklyVolumes.suffix(3))
        
        for i in 1..<recent.count {
            let previousWeek = recent[i - 1].totalVolume
            let currentWeek = recent[i].totalVolume
            
            guard previousWeek > 0 else { continue }
            
            let rampRate = (currentWeek - previousWeek) / previousWeek
            
            if rampRate > Self.dangerousVolumeRampRate {
                let percentage = Int(rampRate * 100)
                return OvertrainingSignal(
                    type: .volumeRampTooFast,
                    severity: .high,
                    description: "Weekly volume increased \(percentage)% in a single week. Safe progression is ≤10% per week to minimize injury risk."
                )
            }
        }
        
        return nil
    }
    
    /// Detects RPE drift: same weight/reps feeling harder over multiple weeks.
    /// This indicates accumulated fatigue despite no external load change.
    private func detectRPEDrift(workouts: [Workout]) -> OvertrainingSignal? {
        // Group by exercise, track RPE trend for same weight/rep ranges
        let exerciseGroups = Dictionary(grouping: workouts.flatMap { $0.sets }) { set in
            set.exercise?.id
        }
        
        for (_, sets) in exerciseGroups {
            if let drift = detectDriftInExercise(sets: sets) {
                return drift
            }
        }
        
        return nil
    }
    
    private func detectDriftInExercise(sets: [ExerciseSet]) -> OvertrainingSignal? {
        let workingSets = sets
            .filter { $0.setType == .working && $0.rpe != nil }
            .sorted { ($0.workout?.date ?? $0.createdAt) < ($1.workout?.date ?? $1.createdAt) }
        
        guard workingSets.count >= 6 else { return nil }
        
        // Group by week
        let weeklyGroups = Dictionary(grouping: workingSets) { set in
            startOfWeek(for: set.workout?.date ?? set.createdAt)
        }
        
        let weeklyData = weeklyGroups
            .sorted { $0.key < $1.key }
            .compactMap { (weekStart, weekSets) -> (date: Date, avgRPE: Double, avgWeight: Double)? in
                let avgRPE = weekSets.compactMap { $0.rpe }.reduce(0, +) / Double(weekSets.count)
                let avgWeight = weekSets.map { $0.weightKg }.reduce(0, +) / Double(weekSets.count)
                return (weekStart, avgRPE, avgWeight)
            }
        
        guard weeklyData.count >= Self.rpeDriftMinWeeks else { return nil }
        
        // Check if RPE increased while weight stayed roughly constant
        let recent = Array(weeklyData.suffix(3))
        let baseline = weeklyData[weeklyData.count - 4]
        
        let weightChange = abs(recent.last!.avgWeight - baseline.avgWeight) / baseline.avgWeight
        let rpeIncrease = recent.last!.avgRPE - baseline.avgRPE
        
        // Same weight (±5%) but RPE increased by 1+ point
        if weightChange < 0.05 && rpeIncrease >= Self.rpeDriftThreshold {
            return OvertrainingSignal(
                type: .rpeDrift,
                severity: .moderate,
                description: "RPE has increased by \(String(format: "%.1f", rpeIncrease)) points over 3+ weeks despite similar weights. This indicates accumulated fatigue."
            )
        }
        
        return nil
    }
    
    /// Detects performance decline: e1RM dropping > 5% across 2+ major lifts.
    private func detectPerformanceDecline(workouts: [Workout]) -> OvertrainingSignal? {
        let exerciseGroups = Dictionary(grouping: workouts.flatMap { $0.sets }) { set in
            set.exercise?.id
        }
        
        var decliningExercises: [(name: String, decline: Double)] = []
        
        for (_, sets) in exerciseGroups {
            if let decline = detectDeclineInExercise(sets: sets) {
                decliningExercises.append(decline)
            }
        }
        
        guard decliningExercises.count >= Self.performanceDeclineMinExercises else { return nil }
        
        let names = decliningExercises.map { $0.name }.joined(separator: ", ")
        let avgDecline = decliningExercises.map { $0.decline }.reduce(0, +) / Double(decliningExercises.count)
        let percentage = Int(avgDecline * 100)
        
        return OvertrainingSignal(
            type: .performanceDecline,
            severity: .high,
            description: "Strength declined ~\(percentage)% across multiple lifts: \(names). This is a red flag for overtraining."
        )
    }
    
    private func detectDeclineInExercise(sets: [ExerciseSet]) -> (name: String, decline: Double)? {
        let workingSets = sets
            .filter { $0.setType == .working }
            .sorted { ($0.workout?.date ?? $0.createdAt) < ($1.workout?.date ?? $1.createdAt) }
        
        guard workingSets.count >= 8 else { return nil }
        
        // Compare recent 4 sessions vs previous 4 sessions
        let midpoint = workingSets.count / 2
        let older = Array(workingSets[..<midpoint])
        let recent = Array(workingSets[midpoint...])
        
        let olderBestE1RM = older
            .map { e1rmCalculator.calculate(weight: $0.weightKg, reps: $0.reps) }
            .max() ?? 0
        
        let recentBestE1RM = recent
            .map { e1rmCalculator.calculate(weight: $0.weightKg, reps: $0.reps) }
            .max() ?? 0
        
        guard olderBestE1RM > 0 else { return nil }
        
        let decline = (olderBestE1RM - recentBestE1RM) / olderBestE1RM
        
        if decline > Self.performanceDeclineThreshold {
            let exerciseName = workingSets.first?.exercise?.name ?? "Unknown"
            return (exerciseName, decline)
        }
        
        return nil
    }
    
    /// Detects chronic low readiness combined with high training volume.
    /// Low readiness alone is okay (rest day). Low readiness + high volume = overtraining.
    private func detectChronicLowReadiness(
        readinessScores: [ReadinessScore],
        workouts: [Workout]
    ) -> OvertrainingSignal? {
        
        guard readinessScores.count >= Self.lowReadinessDays else { return nil }
        
        let recentScores = Array(readinessScores
            .sorted { $0.date > $1.date }
            .prefix(Self.lowReadinessDays))
        
        // Check if all recent scores are below threshold
        let allLow = recentScores.allSatisfy { $0.overallScore < Self.lowReadinessThreshold }
        
        guard allLow else { return nil }
        
        // Check if training volume was high during this period
        let lowReadinessPeriodStart = recentScores.last!.date
        let recentWorkouts = workouts.filter { $0.date >= lowReadinessPeriodStart }
        
        guard recentWorkouts.count >= 3 else { return nil } // High volume = 3+ workouts in 5 days
        
        return OvertrainingSignal(
            type: .chronicLowReadiness,
            severity: .high,
            description: "Readiness has been below 60 for \(Self.lowReadinessDays) consecutive days despite continued high training volume. Your body is not recovering adequately."
        )
    }
    
    // MARK: - Risk Level
    
    private func determineRiskLevel(signalCount: Int) -> OvertrainingRiskLevel {
        switch signalCount {
        case 0:
            return .none
        case 1...2:
            return .moderate
        default:
            return .high
        }
    }
    
    // MARK: - Recommendations
    
    private func generateRecommendations(
        riskLevel: OvertrainingRiskLevel,
        signals: [OvertrainingSignal]
    ) -> [String] {
        var recommendations: [String] = []
        
        switch riskLevel {
        case .none:
            recommendations.append("No overtraining signals detected. Training load is sustainable.")
            
        case .moderate:
            recommendations.append("⚠️ Early warning signs detected. Consider a deload week (reduce volume 40-50%).")
            
            if signals.contains(where: { $0.type == .volumeRampTooFast }) {
                recommendations.append("Reduce weekly volume increase to ≤10% to allow adaptation.")
            }
            
            if signals.contains(where: { $0.type == .rpeDrift }) {
                recommendations.append("Take 3-5 days of active recovery (light cardio, mobility work).")
            }
            
        case .high:
            recommendations.append("🚨 Multiple overtraining signals present. Immediate deload required.")
            recommendations.append("Reduce volume by 50% for 1 week. Prioritize sleep (8+ hours) and nutrition.")
            recommendations.append("If symptoms persist after deload, consult a coach or sports medicine professional.")
            
            if signals.contains(where: { $0.type == .performanceDecline }) {
                recommendations.append("Strength decline detected. Consider a full deload week (60% intensity, 40% volume).")
            }
            
            if signals.contains(where: { $0.type == .chronicLowReadiness }) {
                recommendations.append("Chronic fatigue detected. Prioritize recovery: sleep quality, stress management, and adequate calories.")
            }
        }
        
        return recommendations
    }
    
    // MARK: - Helpers
    
    private func hasMinimumDataRequirement(workouts: [Workout]) -> Bool {
        guard let earliest = workouts.map({ $0.date }).min() else { return false }
        let weeksOfData = Calendar.current.dateComponents([.weekOfYear], from: earliest, to: Date()).weekOfYear ?? 0
        return weeksOfData >= Self.minimumWeeksOfData
    }
    
    private func calculateWeeklyVolumes(workouts: [Workout]) -> [(weekStart: Date, totalVolume: Double)] {
        let grouped = Dictionary(grouping: workouts) { workout in
            startOfWeek(for: workout.date)
        }
        
        return grouped.map { (weekStart, weekWorkouts) in
            let totalVolume = weekWorkouts
                .flatMap { $0.sets }
                .filter { $0.setType == .working }
                .reduce(0.0) { $0 + $1.volume }
            return (weekStart, totalVolume)
        }
        .sorted { $0.weekStart < $1.weekStart }
    }
    
    private func startOfWeek(for date: Date) -> Date {
        var calendar = Calendar.current
        calendar.firstWeekday = 2 // Monday
        let components = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: date)
        return calendar.date(from: components) ?? date
    }
}

// MARK: - Data Models

/// Overtraining risk stratification.
public enum OvertrainingRiskLevel: String, Codable, Sendable {
    case none
    case moderate
    case high
    
    public var displayName: String {
        switch self {
        case .none: return "No Risk"
        case .moderate: return "Moderate Risk"
        case .high: return "High Risk"
        }
    }
}

/// Type of overtraining signal detected.
public enum OvertrainingSignalType: String, Codable, Sendable {
    case multiExercisePlateau
    case volumeRampTooFast
    case rpeDrift
    case performanceDecline
    case chronicLowReadiness
}

/// Severity of individual signal.
public enum SignalSeverity: String, Codable, Sendable {
    case moderate
    case high
}

/// Individual overtraining indicator with context.
public struct OvertrainingSignal: Codable, Sendable {
    public let type: OvertrainingSignalType
    public let severity: SignalSeverity
    public let description: String
    
    public init(type: OvertrainingSignalType, severity: SignalSeverity, description: String) {
        self.type = type
        self.severity = severity
        self.description = description
    }
}

/// Result of overtraining analysis.
@Model
public final class OvertrainingAnalysis {
    public var id: UUID
    public var analyzedAt: Date
    public var riskLevelRaw: String
    public var activeSignalsData: Data
    public var recommendations: [String]
    
    public init(
        id: UUID = UUID(),
        analyzedAt: Date,
        riskLevel: OvertrainingRiskLevel,
        activeSignals: [OvertrainingSignal],
        recommendations: [String]
    ) {
        self.id = id
        self.analyzedAt = analyzedAt
        self.riskLevelRaw = riskLevel.rawValue
        self.activeSignalsData = (try? JSONEncoder().encode(activeSignals)) ?? Data()
        self.recommendations = recommendations
    }
    
    public var riskLevel: OvertrainingRiskLevel {
        get { OvertrainingRiskLevel(rawValue: riskLevelRaw) ?? .none }
        set { riskLevelRaw = newValue.rawValue }
    }
    
    public var activeSignals: [OvertrainingSignal] {
        get { (try? JSONDecoder().decode([OvertrainingSignal].self, from: activeSignalsData)) ?? [] }
        set { activeSignalsData = (try? JSONEncoder().encode(newValue)) ?? Data() }
    }
}
