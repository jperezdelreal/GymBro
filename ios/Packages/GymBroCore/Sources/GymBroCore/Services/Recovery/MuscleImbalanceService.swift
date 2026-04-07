import Foundation
import SwiftData

/// Muscle imbalance detection and volume landmark analysis.
///
/// Analyzes training balance across muscle groups and provides actionable alerts when:
/// - Push/Pull ratio is imbalanced (anterior dominance = shoulder/posture issues)
/// - Per-muscle-group weekly set count deviates from hypertrophy landmarks
/// - Anterior/Posterior chain is imbalanced
///
/// **Evidence-based volume landmarks (Dr. Mike Israetel's work):**
/// - **MEV (Minimum Effective Volume):** Minimum sets/week for growth
/// - **MAV (Maximum Adaptive Volume):** Optimal sets/week for most trainees
/// - **MRV (Maximum Recoverable Volume):** Upper limit before diminishing returns
///
/// **Design philosophy:**
/// - Evidence-based thresholds from peer-reviewed research (Israetel, Schoenfeld, Helms)
/// - Conservative alerts — only warn when imbalance is clinically significant
/// - Actionable recommendations, not just "you're imbalanced"
/// - Graceful degradation with limited data (need 2+ weeks for volume analysis)
public final class MuscleImbalanceService {
    
    // MARK: - Push/Pull Ratio Thresholds
    
    /// Healthy push:pull ratio range (0.8 to 1.2 is balanced)
    /// > 1.5 = anterior dominance (shoulder impingement risk)
    /// < 0.67 = posterior dominance (rare, but possible)
    private static let pushPullRatioHealthyMin: Double = 0.8
    private static let pushPullRatioHealthyMax: Double = 1.2
    private static let pushPullRatioWarningThreshold: Double = 1.5
    
    // MARK: - Volume Landmarks (sets per muscle group per week)
    
    /// Hypertrophy volume landmarks per Dr. Mike Israetel (Renaissance Periodization).
    /// Source: Schoenfeld et al., 2017; Israetel, Hoffmann, Smith, 2020
    private static let volumeLandmarks: [String: VolumeLandmark] = [
        // Upper Body Push
        "Chest": VolumeLandmark(mev: 8, mav: 14, mrv: 22),
        "Front Delts": VolumeLandmark(mev: 0, mav: 6, mrv: 12), // Often trained via chest
        "Side Delts": VolumeLandmark(mev: 6, mav: 12, mrv: 20),
        "Triceps": VolumeLandmark(mev: 4, mav: 10, mrv: 16),
        
        // Upper Body Pull
        "Back": VolumeLandmark(mev: 10, mav: 16, mrv: 25),
        "Rear Delts": VolumeLandmark(mev: 6, mav: 12, mrv: 20),
        "Biceps": VolumeLandmark(mev: 6, mav: 12, mrv: 20),
        "Traps": VolumeLandmark(mev: 0, mav: 6, mrv: 12), // Often trained via back
        
        // Legs
        "Quads": VolumeLandmark(mev: 8, mav: 15, mrv: 25),
        "Hamstrings": VolumeLandmark(mev: 6, mav: 12, mrv: 20),
        "Glutes": VolumeLandmark(mev: 6, mav: 12, mrv: 20),
        "Calves": VolumeLandmark(mev: 8, mav: 14, mrv: 22),
        
        // Core
        "Abs": VolumeLandmark(mev: 0, mav: 12, mrv: 20),
        "Lower Back": VolumeLandmark(mev: 0, mav: 6, mrv: 12)
    ]
    
    /// Muscle groups categorized as "push" (anterior chain).
    private static let pushMuscles: Set<String> = [
        "Chest", "Front Delts", "Side Delts", "Triceps", "Quads"
    ]
    
    /// Muscle groups categorized as "pull" (posterior chain).
    private static let pullMuscles: Set<String> = [
        "Back", "Rear Delts", "Biceps", "Traps", "Hamstrings", "Glutes"
    ]
    
    /// Minimum weeks of data for meaningful volume analysis.
    private static let minimumWeeksOfData: Int = 2
    
    // MARK: - Core Analysis
    
    /// Analyzes muscle balance and volume distribution from recent training.
    /// Returns nil if insufficient data (< 2 weeks).
    public func analyze(workouts: [Workout], timeWindow: TimeWindow = .oneMonth) -> MuscleImbalanceAnalysis? {
        let startDate = timeWindow.startDate()
        
        let filteredWorkouts: [Workout]
        if let start = startDate {
            filteredWorkouts = workouts.filter { $0.date >= start }
        } else {
            filteredWorkouts = workouts
        }
        
        guard hasMinimumDataRequirement(workouts: filteredWorkouts) else { return nil }
        
        // Calculate per-muscle-group weekly set counts
        let weeklySetCounts = calculateWeeklySetCounts(workouts: filteredWorkouts)
        
        // Calculate push/pull ratio
        let pushPullRatio = calculatePushPullRatio(weeklySetCounts: weeklySetCounts)
        
        // Detect volume landmark violations
        let volumeAlerts = detectVolumeLandmarkViolations(weeklySetCounts: weeklySetCounts)
        
        // Detect push/pull imbalance
        let pushPullAlert = detectPushPullImbalance(ratio: pushPullRatio)
        
        // Combine all alerts
        var allAlerts = volumeAlerts
        if let alert = pushPullAlert {
            allAlerts.append(alert)
        }
        
        // Generate recommendations
        let recommendations = generateRecommendations(
            alerts: allAlerts,
            weeklySetCounts: weeklySetCounts,
            pushPullRatio: pushPullRatio
        )
        
        return MuscleImbalanceAnalysis(
            analyzedAt: Date(),
            weeklySetCounts: weeklySetCounts,
            pushPullRatio: pushPullRatio,
            alerts: allAlerts,
            recommendations: recommendations
        )
    }
    
    // MARK: - Volume Calculation
    
    /// Calculates average weekly set count per muscle group.
    /// Uses primary muscle weighting (primary = 1.0, secondary = 0.5).
    private func calculateWeeklySetCounts(workouts: [Workout]) -> [String: Double] {
        let weeks = calculateWeeks(workouts: workouts)
        guard weeks > 0 else { return [:] }
        
        var totalSetsByMuscle: [String: Double] = [:]
        
        for workout in workouts {
            for set in workout.sets where set.setType == .working {
                guard let exercise = set.exercise else { continue }
                
                for muscleGroup in exercise.muscleGroups {
                    let weight: Double = muscleGroup.isPrimary ? 1.0 : 0.5
                    totalSetsByMuscle[muscleGroup.name, default: 0] += weight
                }
            }
        }
        
        // Average per week
        return totalSetsByMuscle.mapValues { $0 / Double(weeks) }
    }
    
    private func calculateWeeks(workouts: [Workout]) -> Int {
        guard let earliest = workouts.map({ $0.date }).min(),
              let latest = workouts.map({ $0.date }).max() else {
            return 0
        }
        
        let weeks = Calendar.current.dateComponents([.weekOfYear], from: earliest, to: latest).weekOfYear ?? 0
        return max(1, weeks)
    }
    
    // MARK: - Push/Pull Ratio
    
    /// Calculates push:pull volume ratio.
    /// Ratio = push volume / pull volume.
    /// Healthy range: 0.8–1.2. Warning: > 1.5 (anterior dominance).
    private func calculatePushPullRatio(weeklySetCounts: [String: Double]) -> Double {
        let pushVolume = weeklySetCounts
            .filter { Self.pushMuscles.contains($0.key) }
            .values
            .reduce(0, +)
        
        let pullVolume = weeklySetCounts
            .filter { Self.pullMuscles.contains($0.key) }
            .values
            .reduce(0, +)
        
        guard pullVolume > 0 else { return 0 }
        
        return pushVolume / pullVolume
    }
    
    private func detectPushPullImbalance(ratio: Double) -> ImbalanceAlert? {
        guard ratio > 0 else { return nil }
        
        if ratio > Self.pushPullRatioWarningThreshold {
            return ImbalanceAlert(
                type: .pushPullImbalance,
                severity: .high,
                muscleGroup: "Push/Pull Balance",
                currentVolume: ratio,
                targetRange: "\(Self.pushPullRatioHealthyMin)–\(Self.pushPullRatioHealthyMax)",
                description: "Push:Pull ratio is \(String(format: "%.2f", ratio)):1 (anterior dominance). This increases shoulder impingement and posture dysfunction risk. Add more back/posterior delt work."
            )
        } else if ratio < (1.0 / Self.pushPullRatioWarningThreshold) {
            return ImbalanceAlert(
                type: .pushPullImbalance,
                severity: .moderate,
                muscleGroup: "Push/Pull Balance",
                currentVolume: ratio,
                targetRange: "\(Self.pushPullRatioHealthyMin)–\(Self.pushPullRatioHealthyMax)",
                description: "Push:Pull ratio is \(String(format: "%.2f", ratio)):1 (posterior dominance). Consider adding more chest/quad work."
            )
        }
        
        return nil
    }
    
    // MARK: - Volume Landmark Detection
    
    /// Detects when muscle groups exceed MRV or fall below MEV.
    private func detectVolumeLandmarkViolations(
        weeklySetCounts: [String: Double]
    ) -> [ImbalanceAlert] {
        var alerts: [ImbalanceAlert] = []
        
        for (muscle, weeklyVolume) in weeklySetCounts {
            guard let landmark = Self.volumeLandmarks[muscle] else { continue }
            
            // Check if exceeding MRV (overtraining that muscle)
            if weeklyVolume > Double(landmark.mrv) {
                alerts.append(ImbalanceAlert(
                    type: .volumeTooHigh,
                    severity: .high,
                    muscleGroup: muscle,
                    currentVolume: weeklyVolume,
                    targetRange: "\(landmark.mev)–\(landmark.mrv)",
                    description: "\(muscle) volume (\(Int(weeklyVolume)) sets/week) exceeds MRV (\(landmark.mrv) sets). Risk of diminishing returns and injury. Reduce to \(landmark.mav)–\(landmark.mrv) sets."
                ))
            }
            
            // Check if below MEV (undertrained muscle — only warn if significantly below)
            else if weeklyVolume < Double(landmark.mev) && landmark.mev > 0 && weeklyVolume < Double(landmark.mev) * 0.5 {
                alerts.append(ImbalanceAlert(
                    type: .volumeTooLow,
                    severity: .moderate,
                    muscleGroup: muscle,
                    currentVolume: weeklyVolume,
                    targetRange: "\(landmark.mev)–\(landmark.mrv)",
                    description: "\(muscle) volume (\(Int(weeklyVolume)) sets/week) is below MEV (\(landmark.mev) sets). Growth stimulus may be insufficient. Aim for \(landmark.mev)+ sets."
                ))
            }
        }
        
        return alerts
    }
    
    // MARK: - Recommendations
    
    private func generateRecommendations(
        alerts: [ImbalanceAlert],
        weeklySetCounts: [String: Double],
        pushPullRatio: Double
    ) -> [String] {
        var recommendations: [String] = []
        
        if alerts.isEmpty {
            recommendations.append("✅ Training volume is well-balanced across all muscle groups.")
            return recommendations
        }
        
        // Group alerts by type
        let tooHighAlerts = alerts.filter { $0.type == .volumeTooHigh }
        let tooLowAlerts = alerts.filter { $0.type == .volumeTooLow }
        let imbalanceAlerts = alerts.filter { $0.type == .pushPullImbalance }
        
        // Address volume violations first
        if !tooHighAlerts.isEmpty {
            let muscles = tooHighAlerts.map { $0.muscleGroup }.joined(separator: ", ")
            recommendations.append("⚠️ Reduce volume for: \(muscles). You're exceeding Maximum Recoverable Volume (MRV).")
        }
        
        if !tooLowAlerts.isEmpty {
            let muscles = tooLowAlerts.map { $0.muscleGroup }.joined(separator: ", ")
            recommendations.append("💡 Increase volume for: \(muscles). You're below Minimum Effective Volume (MEV).")
        }
        
        // Address push/pull imbalance
        if let imbalanceAlert = imbalanceAlerts.first {
            if pushPullRatio > Self.pushPullRatioWarningThreshold {
                let pushVolume = weeklySetCounts
                    .filter { Self.pushMuscles.contains($0.key) }
                    .values
                    .reduce(0, +)
                let pullVolume = weeklySetCounts
                    .filter { Self.pullMuscles.contains($0.key) }
                    .values
                    .reduce(0, +)
                
                let targetPushVolume = pullVolume * 1.1
                let reduction = Int(pushVolume - targetPushVolume)
                
                recommendations.append("🚨 Anterior dominance detected. Add \(reduction)+ weekly sets of back/posterior delt work OR reduce chest/front delt volume by \(reduction) sets.")
                recommendations.append("Priority exercises: Face Pulls, Rows, Rear Delt Flies.")
            } else {
                recommendations.append(imbalanceAlert.description)
            }
        }
        
        // Specific muscle-by-muscle guidance
        for alert in alerts where alert.type != .pushPullImbalance {
            if alert.severity == .high {
                recommendations.append("🔴 \(alert.muscleGroup): \(alert.description)")
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
}

// MARK: - Data Models

/// Volume landmarks for a muscle group (sets per week).
/// Based on Dr. Mike Israetel's hypertrophy research.
public struct VolumeLandmark: Codable, Sendable {
    /// Minimum Effective Volume (sets/week) — minimum for growth
    public let mev: Int
    
    /// Maximum Adaptive Volume (sets/week) — optimal for most trainees
    public let mav: Int
    
    /// Maximum Recoverable Volume (sets/week) — upper limit before diminishing returns
    public let mrv: Int
    
    public init(mev: Int, mav: Int, mrv: Int) {
        self.mev = mev
        self.mav = mav
        self.mrv = mrv
    }
}

/// Type of muscle imbalance detected.
public enum ImbalanceAlertType: String, Codable, Sendable {
    case volumeTooHigh
    case volumeTooLow
    case pushPullImbalance
}

/// Severity of imbalance.
public enum ImbalanceAlertSeverity: String, Codable, Sendable {
    case moderate
    case high
}

/// Individual muscle imbalance alert.
public struct ImbalanceAlert: Codable, Sendable {
    public let type: ImbalanceAlertType
    public let severity: ImbalanceAlertSeverity
    public let muscleGroup: String
    public let currentVolume: Double
    public let targetRange: String
    public let description: String
    
    public init(
        type: ImbalanceAlertType,
        severity: ImbalanceAlertSeverity,
        muscleGroup: String,
        currentVolume: Double,
        targetRange: String,
        description: String
    ) {
        self.type = type
        self.severity = severity
        self.muscleGroup = muscleGroup
        self.currentVolume = currentVolume
        self.targetRange = targetRange
        self.description = description
    }
}

/// Result of muscle imbalance analysis.
@Model
public final class MuscleImbalanceAnalysis {
    public var id: UUID
    public var analyzedAt: Date
    public var weeklySetCountsData: Data
    public var pushPullRatio: Double
    public var alertsData: Data
    public var recommendations: [String]
    
    public init(
        id: UUID = UUID(),
        analyzedAt: Date,
        weeklySetCounts: [String: Double],
        pushPullRatio: Double,
        alerts: [ImbalanceAlert],
        recommendations: [String]
    ) {
        self.id = id
        self.analyzedAt = analyzedAt
        self.weeklySetCountsData = (try? JSONEncoder().encode(weeklySetCounts)) ?? Data()
        self.pushPullRatio = pushPullRatio
        self.alertsData = (try? JSONEncoder().encode(alerts)) ?? Data()
        self.recommendations = recommendations
    }
    
    public var weeklySetCounts: [String: Double] {
        get { (try? JSONDecoder().decode([String: Double].self, from: weeklySetCountsData)) ?? [:] }
        set { weeklySetCountsData = (try? JSONEncoder().encode(newValue)) ?? Data() }
    }
    
    public var alerts: [ImbalanceAlert] {
        get { (try? JSONDecoder().decode([ImbalanceAlert].self, from: alertsData)) ?? [] }
        set { alertsData = (try? JSONEncoder().encode(newValue)) ?? Data() }
    }
}
