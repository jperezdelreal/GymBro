import Foundation
import os

/// Integrates readiness scores with program/workout generation to adjust training based on recovery.
///
/// **Integration points:**
/// - Readiness < 60 + heavy day scheduled → recommend lighter variant
/// - Readiness < 40 → recommend rest day regardless of program
/// - Muscle group fatigue → avoid exercises targeting fatigued muscles
///
/// **Design philosophy:**
/// - Conservative recommendations (err on side of recovery)
/// - User always has final say (recommendations, not mandates)
/// - Maintain program structure when possible (swap variants, don't skip completely)
public final class ReadinessProgramIntegration: Sendable {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ProgramIntegration")
    
    // MARK: - Thresholds
    
    /// Readiness below this suggests lighter variant (60)
    private static let lighterVariantThreshold: Double = 60.0
    
    /// Readiness below this suggests rest day (40)
    private static let restDayThreshold: Double = 40.0
    
    public init() {}
    
    // MARK: - Public API
    
    /// Get readiness-adjusted workout recommendation.
    /// - Parameters:
    ///   - programDay: The originally planned program day
    ///   - readiness: Current readiness score
    ///   - muscleRecovery: Optional muscle recovery map
    /// - Returns: Adjusted recommendation with rationale
    public func adjustWorkout(
        programDay: ProgramDayInfo,
        readiness: ReadinessScore,
        muscleRecovery: [String: MuscleRecoveryStatus]? = nil
    ) -> WorkoutRecommendation {
        // Check for rest day recommendation
        if readiness.overallScore < Self.restDayThreshold {
            Self.logger.info(
                "Recommending rest day — readiness \(readiness.overallScore, privacy: .public) < \(Self.restDayThreshold, privacy: .public)"
            )
            return makeRestDayRecommendation(
                originalDay: programDay,
                readiness: readiness
            )
        }
        
        // Check for lighter variant recommendation
        if readiness.overallScore < Self.lighterVariantThreshold && programDay.isHeavyDay {
            Self.logger.info(
                "Recommending lighter variant — readiness \(readiness.overallScore, privacy: .public), heavy day scheduled"
            )
            return makeLighterVariantRecommendation(
                originalDay: programDay,
                readiness: readiness,
                muscleRecovery: muscleRecovery
            )
        }
        
        // Check muscle-specific recovery constraints
        if let muscleRecovery = muscleRecovery {
            let fatiguedMuscles = muscleRecovery.filter { $0.value.status == .fatigued }
            
            if !fatiguedMuscles.isEmpty {
                let affectedExercises = programDay.exercises.filter { exercise in
                    fatiguedMuscles.keys.contains { exercise.primaryMuscles.contains($0) }
                }
                
                if !affectedExercises.isEmpty {
                    Self.logger.info(
                        "Muscle-specific adjustments needed — fatigued: \(fatiguedMuscles.keys.joined(separator: ", "))"
                    )
                    return makeMuscleSpecificRecommendation(
                        originalDay: programDay,
                        readiness: readiness,
                        fatiguedMuscles: Array(fatiguedMuscles.keys),
                        affectedExercises: affectedExercises
                    )
                }
            }
        }
        
        // No adjustments needed — proceed as planned
        Self.logger.info("No adjustments needed — readiness \(readiness.overallScore, privacy: .public)")
        return WorkoutRecommendation(
            action: .proceedAsPlanned,
            originalDay: programDay,
            adjustedDay: nil,
            rationale: "Your readiness is good. Train as planned.",
            intensityAdjustment: nil,
            volumeAdjustment: nil,
            exerciseReplacements: []
        )
    }
    
    // MARK: - Recommendation Builders
    
    /// Build rest day recommendation.
    private func makeRestDayRecommendation(
        originalDay: ProgramDayInfo,
        readiness: ReadinessScore
    ) -> WorkoutRecommendation {
        let rationale = """
        Your readiness score is \(Int(readiness.overallScore)) (Poor). Your body needs recovery.
        
        Consider:
        • Complete rest day
        • Light walking (20-30 min)
        • Gentle mobility/stretching
        
        Resume training when readiness improves to 50+.
        """
        
        return WorkoutRecommendation(
            action: .restDay,
            originalDay: originalDay,
            adjustedDay: nil,
            rationale: rationale,
            intensityAdjustment: nil,
            volumeAdjustment: nil,
            exerciseReplacements: []
        )
    }
    
    /// Build lighter variant recommendation.
    private func makeLighterVariantRecommendation(
        originalDay: ProgramDayInfo,
        readiness: ReadinessScore,
        muscleRecovery: [String: MuscleRecoveryStatus]?
    ) -> WorkoutRecommendation {
        // Suggest reducing intensity to 60-70% of planned
        let intensityReduction = calculateIntensityReduction(readiness: readiness.overallScore)
        let volumeReduction = calculateVolumeReduction(readiness: readiness.overallScore)
        
        let rationale = """
        Your readiness is \(Int(readiness.overallScore)) (Moderate) and you have a heavy day scheduled.
        
        Recommended adjustments:
        • Reduce working weights to \(100 - intensityReduction)% of planned
        • Reduce total sets by \(volumeReduction)%
        • Focus on technique and controlled tempo
        • Monitor RPE — stop if exceeding RPE 7
        
        This maintains consistency while allowing recovery.
        """
        
        return WorkoutRecommendation(
            action: .lighterVariant,
            originalDay: originalDay,
            adjustedDay: nil, // Client can build adjusted day from parameters
            rationale: rationale,
            intensityAdjustment: -Double(intensityReduction),
            volumeAdjustment: -Double(volumeReduction),
            exerciseReplacements: []
        )
    }
    
    /// Build muscle-specific recommendation.
    private func makeMuscleSpecificRecommendation(
        originalDay: ProgramDayInfo,
        readiness: ReadinessScore,
        fatiguedMuscles: [String],
        affectedExercises: [ProgramExerciseInfo]
    ) -> WorkoutRecommendation {
        let muscleList = fatiguedMuscles.joined(separator: ", ")
        let exerciseList = affectedExercises.map(\.name).joined(separator: ", ")
        
        let rationale = """
        Your \(muscleList) are still recovering from recent training.
        
        Affected exercises: \(exerciseList)
        
        Options:
        1. Skip these exercises and focus on fresh muscle groups
        2. Reduce weight to 50-60% and treat as technique/pump work
        3. Postpone this workout by 24-48h
        
        Overall readiness: \(Int(readiness.overallScore))
        """
        
        // Build replacement suggestions (if available)
        let replacements = affectedExercises.map { exercise in
            ExerciseReplacement(
                original: exercise.name,
                reason: "Primary muscle (\(exercise.primaryMuscles.first ?? "unknown")) is fatigued",
                suggestedAction: "Skip or reduce to 50% intensity"
            )
        }
        
        return WorkoutRecommendation(
            action: .modifyExercises,
            originalDay: originalDay,
            adjustedDay: nil,
            rationale: rationale,
            intensityAdjustment: -40, // -40% for fatigued muscles
            volumeAdjustment: -30,
            exerciseReplacements: replacements
        )
    }
    
    // MARK: - Adjustment Calculations
    
    /// Calculate intensity reduction percentage based on readiness (0-100 scale).
    private func calculateIntensityReduction(readiness: Double) -> Int {
        switch readiness {
        case 55..<60:
            return 20  // Reduce to 80% of planned
        case 50..<55:
            return 30  // Reduce to 70% of planned
        case 40..<50:
            return 40  // Reduce to 60% of planned
        default:
            return 30  // Default to 70%
        }
    }
    
    /// Calculate volume reduction percentage based on readiness.
    private func calculateVolumeReduction(readiness: Double) -> Int {
        switch readiness {
        case 55..<60:
            return 15  // Reduce sets by ~15%
        case 50..<55:
            return 25  // Reduce sets by ~25%
        case 40..<50:
            return 35  // Reduce sets by ~35%
        default:
            return 20  // Default
        }
    }
}

// MARK: - Supporting Types

/// Simplified program day info for integration.
public struct ProgramDayInfo: Sendable {
    public let name: String
    public let isHeavyDay: Bool
    public let exercises: [ProgramExerciseInfo]
    
    public init(name: String, isHeavyDay: Bool, exercises: [ProgramExerciseInfo]) {
        self.name = name
        self.isHeavyDay = isHeavyDay
        self.exercises = exercises
    }
}

/// Simplified exercise info.
public struct ProgramExerciseInfo: Sendable {
    public let name: String
    public let primaryMuscles: [String]
    
    public init(name: String, primaryMuscles: [String]) {
        self.name = name
        self.primaryMuscles = primaryMuscles
    }
}

/// Workout recommendation with readiness adjustments.
public struct WorkoutRecommendation: Sendable {
    public let action: RecommendedAction
    public let originalDay: ProgramDayInfo
    public let adjustedDay: ProgramDayInfo?
    public let rationale: String
    public let intensityAdjustment: Double?  // Percentage (-40 = reduce to 60%)
    public let volumeAdjustment: Double?     // Percentage (-25 = reduce by 25%)
    public let exerciseReplacements: [ExerciseReplacement]
    
    public init(
        action: RecommendedAction,
        originalDay: ProgramDayInfo,
        adjustedDay: ProgramDayInfo?,
        rationale: String,
        intensityAdjustment: Double?,
        volumeAdjustment: Double?,
        exerciseReplacements: [ExerciseReplacement]
    ) {
        self.action = action
        self.originalDay = originalDay
        self.adjustedDay = adjustedDay
        self.rationale = rationale
        self.intensityAdjustment = intensityAdjustment
        self.volumeAdjustment = volumeAdjustment
        self.exerciseReplacements = exerciseReplacements
    }
}

/// Recommended action based on readiness.
public enum RecommendedAction: String, Sendable {
    case proceedAsPlanned   // No changes needed
    case lighterVariant     // Reduce intensity/volume
    case modifyExercises    // Skip or modify specific exercises
    case restDay            // Skip training entirely
    
    public var displayName: String {
        switch self {
        case .proceedAsPlanned: return "Train as Planned"
        case .lighterVariant: return "Lighter Variant"
        case .modifyExercises: return "Modify Exercises"
        case .restDay: return "Rest Day"
        }
    }
}

/// Suggested exercise replacement due to muscle fatigue.
public struct ExerciseReplacement: Sendable {
    public let original: String
    public let reason: String
    public let suggestedAction: String
    
    public init(original: String, reason: String, suggestedAction: String) {
        self.original = original
        self.reason = reason
        self.suggestedAction = suggestedAction
    }
}
