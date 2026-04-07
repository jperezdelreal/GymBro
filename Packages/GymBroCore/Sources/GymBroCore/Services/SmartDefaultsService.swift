import Foundation
import SwiftData
import os

/// Enhanced SmartDefaultsService with fatigue, RPE, recovery, and trend awareness.
///
/// Features:
/// - **Intra-session fatigue detection**: Accounts for rep drop-off across sets and exercise position
/// - **Recovery-aware predictions**: Integrates ReadinessScoreService to adjust load based on readiness
/// - **Historical trend analysis**: Uses last 3-5 sessions instead of just the last one
/// - **RPE integration**: Calibrates next-session weights based on RPE data
/// - **Deload recognition**: Detects deload periods and prevents inappropriate progression
/// - **Experience-level scaling**: Adjusts progression increments based on user experience
///
/// Design philosophy: Heuristics-first, conservative predictions, graceful degradation.
public final class SmartDefaultsService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "SmartDefaults")

    private let modelContext: ModelContext
    private let readinessService: ReadinessScoreService?
    
    // Configurable thresholds
    private let minSessionsForTrend = 3
    private let maxSessionsForTrend = 5
    private let fatigueDropoffThreshold = 0.15 // 15% drop in reps = significant fatigue
    private let deloadWeightThreshold = 0.85 // If weight < 85% of recent max, likely deload week
    
    public init(modelContext: ModelContext, readinessService: ReadinessScoreService? = nil) {
        self.modelContext = modelContext
        self.readinessService = readinessService
    }
    
    /// Get smart defaults with full context awareness.
    public func getSmartDefaults(
        for exercise: Exercise,
        setNumber: Int = 1,
        exercisePositionInWorkout: Int = 1,
        currentReadinessScore: Double? = nil
    ) -> (weight: Double, reps: Int) {
        // Fetch user profile for experience level
        let userProfile = fetchUserProfile()
        let experienceLevel = userProfile?.experienceLevel ?? .intermediate
        
        // Fetch recent workout history
        let recentSessions = fetchRecentSessions(for: exercise, limit: maxSessionsForTrend)
        
        guard !recentSessions.isEmpty else {
            return defaultValues(for: exercise, experienceLevel: experienceLevel)
        }
        
        // Analyze historical trends
        let trendAnalysis = analyzeTrend(sessions: recentSessions, exercise: exercise)
        
        // Detect deload week
        let isDeloadWeek = detectDeload(sessions: recentSessions, trendAnalysis: trendAnalysis)
        
        // Get base prediction from last session
        let lastSession = recentSessions[0]
        var predictedWeight = calculateBaseWeight(from: lastSession, exercise: exercise)
        var predictedReps = calculateBaseReps(from: lastSession)
        
        // Apply RPE calibration if data exists
        if let rpeAdjustment = calculateRPEAdjustment(from: lastSession) {
            predictedWeight *= rpeAdjustment
            Self.logger.info("RPE adjustment: \(rpeAdjustment, privacy: .public)x")
        }
        
        // Apply intra-session fatigue adjustment
        let fatigueMultiplier = calculateFatigueMultiplier(
            setNumber: setNumber,
            exercisePosition: exercisePositionInWorkout,
            historicalFatigue: trendAnalysis.averageFatiguePattern
        )
        predictedWeight *= fatigueMultiplier
        predictedReps = max(1, Int(Double(predictedReps) * fatigueMultiplier))
        
        // Apply recovery adjustment based on readiness score
        if let readinessScore = currentReadinessScore {
            let recoveryMultiplier = calculateRecoveryMultiplier(readinessScore: readinessScore)
            predictedWeight *= recoveryMultiplier
            Self.logger.info("Readiness \(readinessScore, privacy: .public) → recovery multiplier \(recoveryMultiplier, privacy: .public)x")
        }
        
        // Apply progression (or hold/reduce if deload)
        if isDeloadWeek {
            Self.logger.info("Deload week detected — holding weight at \(predictedWeight, privacy: .public)kg")
        } else {
            let progression = calculateProgression(
                exercise: exercise,
                experienceLevel: experienceLevel,
                trendStrength: trendAnalysis.trendStrength
            )
            predictedWeight += progression
        }
        
        // Conservative rounding (always round down for safety)
        predictedWeight = roundWeight(predictedWeight, exercise: exercise)
        
        Self.logger.info(
            "Smart defaults for \(exercise.name): \(predictedWeight, privacy: .public)kg × \(predictedReps) (set \(setNumber), pos \(exercisePositionInWorkout))"
        )
        
        return (weight: predictedWeight, reps: predictedReps)
    }
    
    // MARK: - Session Fetching
    
    /// Fetch recent workout sessions for the given exercise, grouped by workout.
    private func fetchRecentSessions(for exercise: Exercise, limit: Int) -> [[ExerciseSet]] {
        let descriptor = FetchDescriptor<ExerciseSet>(
            predicate: #Predicate<ExerciseSet> { set in
                set.exercise?.id == exercise.id &&
                set.setType == .working &&
                set.completedAt != nil
            },
            sortBy: [SortDescriptor(\.completedAt, order: .reverse)]
        )
        
        let allSets: [ExerciseSet]
        do {
            allSets = try modelContext.fetch(descriptor)
        } catch {
            Self.logger.error("Failed to fetch exercise history: \(error.localizedDescription)")
            return []
        }
        
        // Group sets by workout
        var sessions: [[ExerciseSet]] = []
        var currentSession: [ExerciseSet] = []
        var lastWorkoutId: UUID?
        
        for set in allSets {
            guard let workoutId = set.workout?.id else { continue }
            
            if lastWorkoutId != workoutId {
                if !currentSession.isEmpty {
                    sessions.append(currentSession)
                    if sessions.count >= limit {
                        break
                    }
                }
                currentSession = [set]
                lastWorkoutId = workoutId
            } else {
                currentSession.append(set)
            }
        }
        
        if !currentSession.isEmpty && sessions.count < limit {
            sessions.append(currentSession)
        }
        
        return sessions
    }
    
    // MARK: - Trend Analysis
    
    struct TrendAnalysis {
        let trendStrength: Double // -1.0 (declining) to +1.0 (improving)
        let averageFatiguePattern: Double // 0.0 (no fatigue) to 1.0 (severe fatigue)
        let volumeStability: Double // 0.0 (unstable) to 1.0 (very stable)
    }
    
    /// Analyze historical trend from recent sessions.
    private func analyzeTrend(sessions: [[ExerciseSet]], exercise: Exercise) -> TrendAnalysis {
        guard sessions.count >= 2 else {
            return TrendAnalysis(trendStrength: 0.0, averageFatiguePattern: 0.0, volumeStability: 1.0)
        }
        
        // Calculate e1RM trend
        let e1RMs = sessions.map { session in
            session.map { $0.estimatedOneRepMax }.max() ?? 0.0
        }
        let trendStrength = calculateLinearTrend(values: e1RMs)
        
        // Calculate average fatigue pattern (rep drop-off across sets)
        let fatiguePatterns = sessions.map { calculateFatiguePattern(sets: $0) }
        let avgFatigue = fatiguePatterns.reduce(0.0, +) / Double(fatiguePatterns.count)
        
        // Calculate volume stability (coefficient of variation of total volume)
        let volumes = sessions.map { session in
            session.map { $0.volume }.reduce(0.0, +)
        }
        let volumeStability = 1.0 - coefficientOfVariation(values: volumes)
        
        return TrendAnalysis(
            trendStrength: trendStrength,
            averageFatiguePattern: avgFatigue,
            volumeStability: max(0.0, min(1.0, volumeStability))
        )
    }
    
    /// Calculate fatigue pattern: ratio of last set reps to first set reps.
    private func calculateFatiguePattern(sets: [ExerciseSet]) -> Double {
        guard sets.count >= 2 else { return 0.0 }
        
        let sortedSets = sets.sorted { $0.setNumber < $1.setNumber }
        let firstSetReps = Double(sortedSets.first?.reps ?? 1)
        let lastSetReps = Double(sortedSets.last?.reps ?? 1)
        
        // Fatigue = 1 - (lastReps / firstReps)
        // 0.0 = no fatigue, 1.0 = complete failure
        return max(0.0, 1.0 - (lastSetReps / firstSetReps))
    }
    
    /// Simple linear trend calculation (slope).
    private func calculateLinearTrend(values: [Double]) -> Double {
        guard values.count >= 2 else { return 0.0 }
        
        let n = Double(values.count)
        let xMean = (n - 1) / 2.0
        let yMean = values.reduce(0.0, +) / n
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i, y) in values.enumerated() {
            let x = Double(i)
            numerator += (x - xMean) * (y - yMean)
            denominator += (x - xMean) * (x - xMean)
        }
        
        guard denominator > 0 else { return 0.0 }
        
        let slope = numerator / denominator
        let avgValue = yMean
        
        // Normalize slope to -1.0 to +1.0 range
        return max(-1.0, min(1.0, slope / (avgValue * 0.1)))
    }
    
    /// Coefficient of variation (std dev / mean).
    private func coefficientOfVariation(values: [Double]) -> Double {
        guard values.count >= 2 else { return 0.0 }
        
        let mean = values.reduce(0.0, +) / Double(values.count)
        guard mean > 0 else { return 0.0 }
        
        let variance = values.map { pow($0 - mean, 2) }.reduce(0.0, +) / Double(values.count)
        let stdDev = sqrt(variance)
        
        return stdDev / mean
    }
    
    // MARK: - Deload Detection
    
    /// Detect if the user is in a deload week based on recent weight patterns.
    private func detectDeload(sessions: [[ExerciseSet]], trendAnalysis: TrendAnalysis) -> Bool {
        guard sessions.count >= 2 else { return false }
        
        let lastSession = sessions[0]
        let previousSessions = Array(sessions.dropFirst())
        
        let lastMaxWeight = lastSession.map { $0.weightKg }.max() ?? 0.0
        let historicalMaxWeight = previousSessions.flatMap { $0.map { $0.weightKg } }.max() ?? 0.0
        
        guard historicalMaxWeight > 0 else { return false }
        
        let weightRatio = lastMaxWeight / historicalMaxWeight
        
        // If last session used < 85% of historical max, likely deload
        return weightRatio < deloadWeightThreshold
    }
    
    // MARK: - Base Calculations
    
    /// Calculate base weight from last session (top set approach).
    private func calculateBaseWeight(from session: [ExerciseSet], exercise: Exercise) -> Double {
        // Use top set (max weight) from last session
        return session.map { $0.weightKg }.max() ?? 0.0
    }
    
    /// Calculate base reps from last session (average of working sets).
    private func calculateBaseReps(from session: [ExerciseSet]) -> Int {
        let totalReps = session.map { $0.reps }.reduce(0, +)
        return max(1, totalReps / session.count)
    }
    
    // MARK: - RPE Calibration
    
    /// Calculate weight adjustment based on RPE data from last session.
    /// RPE 6-7 = increase, RPE 8-9 = hold, RPE 10 = reduce.
    private func calculateRPEAdjustment(from session: [ExerciseSet]) -> Double? {
        let rpeSets = session.compactMap { $0.rpe }
        guard !rpeSets.isEmpty else { return nil }
        
        let avgRPE = rpeSets.reduce(0.0, +) / Double(rpeSets.count)
        
        // RPE-based calibration:
        // RPE 6-7: +2.5% (user has more in tank)
        // RPE 8-9: 0% (perfect difficulty)
        // RPE 10: -5% (too difficult, back off)
        switch avgRPE {
        case ..<7.0:
            return 1.025 // +2.5%
        case 7.0..<9.5:
            return 1.0 // Hold
        default:
            return 0.95 // -5%
        }
    }
    
    // MARK: - Fatigue Multiplier
    
    /// Calculate intra-session fatigue multiplier based on set number and exercise position.
    private func calculateFatigueMultiplier(
        setNumber: Int,
        exercisePosition: Int,
        historicalFatigue: Double
    ) -> Double {
        // Base fatigue from set number (sets 1-3)
        // Set 1: 1.0, Set 2: 0.975, Set 3: 0.95
        let setFatigue = max(0.9, 1.0 - (Double(setNumber - 1) * 0.025))
        
        // Fatigue from exercise position in workout
        // Exercise 1-2: 1.0, Exercise 3-4: 0.98, Exercise 5+: 0.95
        let positionFatigue: Double
        if exercisePosition <= 2 {
            positionFatigue = 1.0
        } else if exercisePosition <= 4 {
            positionFatigue = 0.98
        } else {
            positionFatigue = 0.95
        }
        
        // Apply historical fatigue pattern
        let historyAdjustment = 1.0 - (historicalFatigue * 0.1)
        
        return setFatigue * positionFatigue * historyAdjustment
    }
    
    // MARK: - Recovery Multiplier
    
    /// Calculate recovery-based weight multiplier from readiness score.
    /// Score >= 80: +2.5% (push), 60-79: 0% (normal), 40-59: -10% (reduce), < 40: -20% (rest day)
    private func calculateRecoveryMultiplier(readinessScore: Double) -> Double {
        switch readinessScore {
        case 80...100:
            return 1.025 // Push intensity
        case 60..<80:
            return 1.0 // Normal
        case 40..<60:
            return 0.90 // Reduce load
        default:
            return 0.80 // Rest day — minimal load
        }
    }
    
    // MARK: - Progression
    
    /// Calculate progression increment based on exercise type, experience level, and trend strength.
    private func calculateProgression(
        exercise: Exercise,
        experienceLevel: ExperienceLevel,
        trendStrength: Double
    ) -> Double {
        // Base progression by exercise category
        let baseProgression: Double
        switch exercise.category {
        case .compound:
            baseProgression = 2.5
        case .isolation:
            baseProgression = 1.25
        case .accessory, .cardio:
            baseProgression = 0.0
        }
        
        // Scale by experience level
        let experienceMultiplier: Double
        switch experienceLevel {
        case .beginner:
            experienceMultiplier = 1.5 // Faster gains
        case .intermediate:
            experienceMultiplier = 1.0 // Normal
        case .advanced:
            experienceMultiplier = 0.5 // Slower gains
        case .elite:
            experienceMultiplier = 0.25 // Very slow gains
        }
        
        // Adjust by trend strength
        // Positive trend: full progression, negative trend: half progression
        let trendMultiplier = trendStrength < 0 ? 0.5 : 1.0
        
        return baseProgression * experienceMultiplier * trendMultiplier
    }
    
    // MARK: - Weight Rounding
    
    /// Round weight conservatively based on exercise type.
    private func roundWeight(_ weight: Double, exercise: Exercise) -> Double {
        let increment: Double
        switch exercise.category {
        case .compound:
            increment = 2.5 // Round to nearest 2.5kg
        case .isolation:
            increment = 1.25 // Round to nearest 1.25kg
        case .accessory, .cardio:
            increment = 0.5 // Round to nearest 0.5kg
        }
        
        // Always round down (conservative)
        return floor(weight / increment) * increment
    }
    
    // MARK: - User Profile
    
    private func fetchUserProfile() -> UserProfile? {
        let descriptor = FetchDescriptor<UserProfile>()
        return try? modelContext.fetch(descriptor).first
    }
    
    // MARK: - Default Values
    
    private func defaultValues(for exercise: Exercise, experienceLevel: ExperienceLevel) -> (weight: Double, reps: Int) {
        let baseDefaults: (weight: Double, reps: Int)
        
        switch exercise.category {
        case .compound:
            switch exercise.equipment {
            case .barbell:
                baseDefaults = (60.0, 5)
            case .dumbbell:
                baseDefaults = (20.0, 8)
            default:
                baseDefaults = (40.0, 8)
            }
        case .isolation:
            baseDefaults = (15.0, 10)
        case .accessory:
            baseDefaults = (10.0, 12)
        case .cardio:
            baseDefaults = (0.0, 1)
        }
        
        // Apply progression to defaults for first-time suggestion
        let progression = calculateProgression(
            exercise: exercise,
            experienceLevel: experienceLevel,
            trendStrength: 0.0
        )
        
        return (weight: baseDefaults.weight + progression, reps: baseDefaults.reps)
    }
}
