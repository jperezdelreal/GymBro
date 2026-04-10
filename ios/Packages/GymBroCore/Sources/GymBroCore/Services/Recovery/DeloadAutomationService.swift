import Foundation
import SwiftData
import os

/// Automates deload detection and recommendations using ACWR (Acute:Chronic Workload Ratio)
/// and readiness-based triggers.
///
/// **Key features:**
/// - ACWR-based overreaching detection (spike risk at >1.5)
/// - Readiness score integration (chronic fatigue detection)
/// - Time-based accumulation tracking (4+ weeks without deload)
/// - State machine: Normal → Overreaching → Deload → Recovery → Normal
/// - Auto-generates deload week recommendations (60-70% volume, maintain intensity)
///
/// **Evidence base:**
/// - Gabbett (2016): ACWR >1.5 = 2-4x injury risk
/// - Helms et al. (2018): Deload every 4-6 weeks optimal for intermediate+ lifters
/// - Schoenfeld & Grgic (2020): Volume reduction > intensity reduction for recovery
public final class DeloadAutomationService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "DeloadAutomation")
    
    // MARK: - Thresholds
    
    /// ACWR above this triggers acute spike warning (1.5)
    private static let acwrSpikeThreshold: Double = 1.5
    
    /// ACWR below this suggests detraining (0.7)
    private static let acwrDetrainingThreshold: Double = 0.7
    
    /// Readiness below this for N consecutive days = chronic fatigue (40)
    private static let chronicFatigueReadinessThreshold: Double = 40.0
    
    /// Consecutive days of low readiness to trigger deload (3)
    private static let chronicFatigueDays: Int = 3
    
    /// Weeks without a deload before auto-recommending one (4)
    private static let weeksBeforeAccumulationDeload: Int = 4
    
    /// Target volume reduction during deload (60-70% of normal)
    private static let deloadVolumeReductionPercent: Int = 35 // Reduce TO 65%
    
    /// Maintain intensity during deload (reduce volume, not weight)
    private static let maintainIntensity: Bool = true
    
    private let trainingLoadCalculator: TrainingLoadCalculator
    
    public init(trainingLoadCalculator: TrainingLoadCalculator = TrainingLoadCalculator()) {
        self.trainingLoadCalculator = trainingLoadCalculator
    }
    
    // MARK: - Analysis
    
    /// Analyze current training state and determine if deload is needed.
    ///
    /// - Parameters:
    ///   - workouts: Recent workout history (recommend 6+ weeks)
    ///   - readinessScores: Recent readiness scores (recommend 7+ days)
    ///   - lastDeloadDate: Date of last completed deload (nil if never)
    ///   - currentDate: Current date (for testing)
    /// - Returns: Deload recommendation with trigger analysis
    public func analyzeDeloadNeed(
        workouts: [Workout],
        readinessScores: [ReadinessScore],
        lastDeloadDate: Date?,
        currentDate: Date = Date()
    ) -> DeloadRecommendation {
        var triggers: [DeloadTrigger] = []
        
        // Calculate training load and ACWR
        let dailyLoads = calculateDailyTrainingLoads(workouts: workouts, upToDate: currentDate)
        let trainingLoad = trainingLoadCalculator.calculate(dailyVolumes: dailyLoads)
        
        Self.logger.info(
            "ACWR: \(trainingLoad.acwr, privacy: .public), TSB: \(trainingLoad.trainingStressBalance, privacy: .public)"
        )
        
        // Trigger 1: ACWR spike (>1.5)
        if trainingLoad.acwr > Self.acwrSpikeThreshold {
            triggers.append(DeloadTrigger(
                type: .acwrSpike,
                severity: .high,
                description: "ACWR at \(String(format: "%.2f", trainingLoad.acwr)) (>1.5). Acute training load spike detected — injury risk elevated.",
                metric: trainingLoad.acwr
            ))
        }
        
        // Trigger 2: Chronic low readiness (< 40 for 3+ days)
        if let chronicFatigueTrigger = detectChronicFatigue(
            readinessScores: readinessScores,
            currentDate: currentDate
        ) {
            triggers.append(chronicFatigueTrigger)
        }
        
        // Trigger 3: Volume accumulation (4+ weeks without deload)
        if let accumulationTrigger = detectVolumeAccumulation(
            lastDeloadDate: lastDeloadDate,
            currentDate: currentDate
        ) {
            triggers.append(accumulationTrigger)
        }
        
        // Determine deload state
        let state = determineDeloadState(triggers: triggers, trainingLoad: trainingLoad)
        
        // Generate recommendation
        let recommendation = buildRecommendation(
            state: state,
            triggers: triggers,
            trainingLoad: trainingLoad,
            currentDate: currentDate
        )
        
        Self.logger.info(
            "Deload state: \(state.rawValue), triggers: \(triggers.count), recommended: \(recommendation.shouldDeload)"
        )
        
        return recommendation
    }
    
    /// Calculate a deload week based on current program.
    ///
    /// - Parameters:
    ///   - normalWeekWorkouts: Workouts from a typical training week
    ///   - deloadIntensity: Target intensity (0.65 = 65% of normal volume)
    /// - Returns: Deload week specification
    public func generateDeloadWeek(
        normalWeekWorkouts: [Workout],
        deloadIntensity: Double = 0.65
    ) -> DeloadWeek {
        let normalVolume = normalWeekWorkouts.reduce(0.0) { $0 + $1.totalVolume }
        let targetVolume = normalVolume * deloadIntensity
        
        // Calculate set reduction per workout
        let normalSets = normalWeekWorkouts.reduce(0) { $0 + $1.totalSets }
        let targetSets = Int(Double(normalSets) * deloadIntensity)
        
        let exercises = normalWeekWorkouts.flatMap { $0.exercises }
        
        return DeloadWeek(
            targetVolumeReduction: Int((1.0 - deloadIntensity) * 100),
            maintainIntensity: Self.maintainIntensity,
            targetVolume: targetVolume,
            targetSets: targetSets,
            exercisesToInclude: exercises.map { $0.name },
            rationale: """
            Deload week targets \(Int(deloadIntensity * 100))% of normal volume (\(targetSets) sets vs \(normalSets) sets).
            
            Guidelines:
            • Keep weights the same or slightly reduce (90-95%)
            • Reduce sets per exercise by 30-40%
            • Stop all sets at RPE 6-7 (leave 3-4 reps in reserve)
            • Focus on quality movement and technique
            • Prioritize sleep and nutrition
            
            This allows recovery while maintaining movement patterns and neural adaptations.
            """
        )
    }
    
    // MARK: - Detection Methods
    
    /// Detect chronic fatigue from consecutive low readiness scores.
    private func detectChronicFatigue(
        readinessScores: [ReadinessScore],
        currentDate: Date
    ) -> DeloadTrigger? {
        guard readinessScores.count >= Self.chronicFatigueDays else { return nil }
        
        let sorted = readinessScores.sorted { $0.date > $1.date }
        let recent = Array(sorted.prefix(Self.chronicFatigueDays))
        
        // Check consecutive days
        let allLow = recent.allSatisfy { $0.overallScore < Self.chronicFatigueReadinessThreshold }
        
        guard allLow else { return nil }
        
        let avgReadiness = recent.map { $0.overallScore }.reduce(0, +) / Double(recent.count)
        
        return DeloadTrigger(
            type: .chronicFatigue,
            severity: .high,
            description: "Readiness below \(Int(Self.chronicFatigueReadinessThreshold)) for \(Self.chronicFatigueDays) consecutive days (avg: \(Int(avgReadiness))). Body not recovering adequately.",
            metric: avgReadiness
        )
    }
    
    /// Detect volume accumulation (time since last deload).
    private func detectVolumeAccumulation(
        lastDeloadDate: Date?,
        currentDate: Date
    ) -> DeloadTrigger? {
        guard let lastDeload = lastDeloadDate else {
            // Never deloaded — check if training history is long enough
            return DeloadTrigger(
                type: .volumeAccumulation,
                severity: .moderate,
                description: "No deload recorded. Consider planning a deload week after 4-6 weeks of consistent training.",
                metric: nil
            )
        }
        
        let weeksSinceDeload = Calendar.current.dateComponents(
            [.weekOfYear],
            from: lastDeload,
            to: currentDate
        ).weekOfYear ?? 0
        
        if weeksSinceDeload >= Self.weeksBeforeAccumulationDeload {
            return DeloadTrigger(
                type: .volumeAccumulation,
                severity: .moderate,
                description: "\(weeksSinceDeload) weeks since last deload. Volume accumulation increases fatigue and plateau risk.",
                metric: Double(weeksSinceDeload)
            )
        }
        
        return nil
    }
    
    // MARK: - State Machine
    
    /// Determine current deload state based on triggers and ACWR.
    private func determineDeloadState(
        triggers: [DeloadTrigger],
        trainingLoad: TrainingLoadCalculator.TrainingLoad
    ) -> DeloadState {
        // High severity triggers = immediate deload needed
        let highSeverityCount = triggers.filter { $0.severity == .high }.count
        
        if highSeverityCount >= 2 {
            return .needsDeload
        }
        
        if highSeverityCount == 1 {
            // Single high-severity trigger (ACWR spike or chronic fatigue)
            return .overreaching
        }
        
        if !triggers.isEmpty {
            // Moderate triggers only (volume accumulation)
            return .overreaching
        }
        
        // Check if in detraining zone
        if trainingLoad.acwr < Self.acwrDetrainingThreshold {
            return .detraining
        }
        
        return .normal
    }
    
    // MARK: - Recommendation Builder
    
    /// Build deload recommendation from state and triggers.
    private func buildRecommendation(
        state: DeloadState,
        triggers: [DeloadTrigger],
        trainingLoad: TrainingLoadCalculator.TrainingLoad,
        currentDate: Date
    ) -> DeloadRecommendation {
        let shouldDeload = state == .needsDeload || state == .overreaching
        
        let urgency: DeloadUrgency
        let actionPlan: String
        
        switch state {
        case .normal:
            urgency = .none
            actionPlan = "No deload needed. Continue training as planned. ACWR: \(String(format: "%.2f", trainingLoad.acwr))"
            
        case .overreaching:
            urgency = .recommended
            actionPlan = """
            Consider scheduling a deload week within the next 7 days.
            
            Triggers:
            \(triggers.map { "• \($0.description)" }.joined(separator: "\n"))
            
            Options:
            1. Full deload week (reduce volume to 60-70%)
            2. Extended rest (2-3 days off, resume with lighter week)
            3. Switch to technique-focused work (50% volume, emphasis on form)
            """
            
        case .needsDeload:
            urgency = .immediate
            actionPlan = """
            Immediate deload recommended. Multiple high-severity signals detected.
            
            Critical triggers:
            \(triggers.filter { $0.severity == .high }.map { "• \($0.description)" }.joined(separator: "\n"))
            
            Action plan:
            1. Start deload week immediately (this week)
            2. Reduce volume to 60-70% of normal
            3. Maintain weights (90-95% of working weights)
            4. All sets to RPE 6-7 maximum
            5. Prioritize sleep (8+ hours) and recovery nutrition
            6. Consider light cardio/mobility on off days
            
            Resume normal training next week if readiness improves.
            """
            
        case .detraining:
            urgency = .none
            actionPlan = """
            ACWR below \(Self.acwrDetrainingThreshold) — training volume has decreased significantly.
            
            This suggests you're already in a deload/recovery phase or training frequency has dropped.
            No additional deload needed. Focus on consistent training to rebuild work capacity.
            """
        }
        
        return DeloadRecommendation(
            analyzedAt: currentDate,
            shouldDeload: shouldDeload,
            state: state,
            urgency: urgency,
            triggers: triggers,
            actionPlan: actionPlan,
            acwr: trainingLoad.acwr,
            trainingStressBalance: trainingLoad.trainingStressBalance
        )
    }
    
    // MARK: - Helpers
    
    /// Calculate daily training loads (tonnage per day) from workout history.
    private func calculateDailyTrainingLoads(
        workouts: [Workout],
        upToDate: Date
    ) -> [Double] {
        // Group by date, sum volume per day
        let calendar = Calendar.current
        let grouped = Dictionary(grouping: workouts.filter { $0.date <= upToDate }) { workout in
            calendar.startOfDay(for: workout.date)
        }
        
        // Create daily array (fill gaps with 0)
        guard let earliestDate = grouped.keys.min(),
              let latestDate = grouped.keys.max() else {
            return []
        }
        
        var dailyLoads: [Double] = []
        var currentDate = earliestDate
        
        while currentDate <= latestDate {
            let dayLoad = grouped[currentDate]?.reduce(0.0) { $0 + $1.totalVolume } ?? 0.0
            dailyLoads.append(dayLoad)
            currentDate = calendar.date(byAdding: .day, value: 1, to: currentDate) ?? currentDate
        }
        
        return dailyLoads
    }
}

// MARK: - Models

/// Deload state machine.
public enum DeloadState: String, Codable, Sendable {
    case normal         // Training load sustainable
    case overreaching   // Approaching fatigue threshold (deload recommended)
    case needsDeload    // Multiple triggers active (deload needed)
    case detraining     // Volume too low (increase training)
    
    public var displayName: String {
        switch self {
        case .normal: return "Normal"
        case .overreaching: return "Overreaching"
        case .needsDeload: return "Needs Deload"
        case .detraining: return "Detraining"
        }
    }
}

/// Deload urgency level.
public enum DeloadUrgency: String, Codable, Sendable {
    case none           // No deload needed
    case recommended    // Should deload soon (within 7 days)
    case immediate      // Deload this week
    
    public var displayName: String {
        switch self {
        case .none: return "No Deload Needed"
        case .recommended: return "Recommended"
        case .immediate: return "Immediate"
        }
    }
}

/// Type of deload trigger.
public enum DeloadTriggerType: String, Codable, Sendable {
    case acwrSpike          // ACWR > 1.5
    case chronicFatigue     // Low readiness 3+ consecutive days
    case volumeAccumulation // 4+ weeks without deload
}

/// Severity of trigger.
public enum DeloadTriggerSeverity: String, Codable, Sendable {
    case moderate
    case high
}

/// Individual deload trigger.
public struct DeloadTrigger: Codable, Sendable {
    public let type: DeloadTriggerType
    public let severity: DeloadTriggerSeverity
    public let description: String
    public let metric: Double?
    
    public init(
        type: DeloadTriggerType,
        severity: DeloadTriggerSeverity,
        description: String,
        metric: Double?
    ) {
        self.type = type
        self.severity = severity
        self.description = description
        self.metric = metric
    }
}

/// Deload recommendation result.
public struct DeloadRecommendation: Codable, Sendable {
    public let analyzedAt: Date
    public let shouldDeload: Bool
    public let state: DeloadState
    public let urgency: DeloadUrgency
    public let triggers: [DeloadTrigger]
    public let actionPlan: String
    public let acwr: Double
    public let trainingStressBalance: Double
    
    public init(
        analyzedAt: Date,
        shouldDeload: Bool,
        state: DeloadState,
        urgency: DeloadUrgency,
        triggers: [DeloadTrigger],
        actionPlan: String,
        acwr: Double,
        trainingStressBalance: Double
    ) {
        self.analyzedAt = analyzedAt
        self.shouldDeload = shouldDeload
        self.state = state
        self.urgency = urgency
        self.triggers = triggers
        self.actionPlan = actionPlan
        self.acwr = acwr
        self.trainingStressBalance = trainingStressBalance
    }
}

/// Deload week specification.
public struct DeloadWeek: Codable, Sendable {
    public let targetVolumeReduction: Int  // Percentage (35 = reduce TO 65%)
    public let maintainIntensity: Bool     // Keep weights similar
    public let targetVolume: Double        // Target total volume for week
    public let targetSets: Int             // Target total sets for week
    public let exercisesToInclude: [String] // Exercises to include
    public let rationale: String           // Explanation for user
    
    public init(
        targetVolumeReduction: Int,
        maintainIntensity: Bool,
        targetVolume: Double,
        targetSets: Int,
        exercisesToInclude: [String],
        rationale: String
    ) {
        self.targetVolumeReduction = targetVolumeReduction
        self.maintainIntensity = maintainIntensity
        self.targetVolume = targetVolume
        self.targetSets = targetSets
        self.exercisesToInclude = exercisesToInclude
        self.rationale = rationale
    }
}
