import Foundation
import SwiftData
import os

/// Orchestrates a complete workout session by combining program templates, muscle recovery,
/// readiness scores, overtraining signals, and exercise history.
///
/// **Design principles:**
/// - Orchestrator pattern: composes existing services, never reinvents them
/// - Every suggestion is explainable (reasoning trail for UI)
/// - Graceful degradation: works with zero history (beginner full-body)
/// - Conservative: better to under-program than over-program
///
/// **Service dependencies:**
/// - MuscleRecoveryService → per-muscle fatigue status
/// - ReadinessProgramIntegration → readiness-based volume/intensity adjustments
/// - OvertrainingDetectionService → systemic overtraining risk
/// - SmartDefaultsService → weight/rep predictions per exercise
public final class WorkoutGeneratorService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WorkoutGenerator")

    // MARK: - Time Budget Constants

    /// Estimated minutes per exercise (including rest between sets).
    private static let minutesPerExercise: Double = 8.0
    /// Warm-up overhead in minutes.
    private static let warmupMinutes: Double = 5.0

    // MARK: - Dependencies

    private let muscleRecoveryService: MuscleRecoveryService
    private let readinessProgramIntegration: ReadinessProgramIntegration
    private let overtrainingDetectionService: OvertrainingDetectionService

    public init(
        muscleRecoveryService: MuscleRecoveryService = MuscleRecoveryService(),
        readinessProgramIntegration: ReadinessProgramIntegration = ReadinessProgramIntegration(),
        overtrainingDetectionService: OvertrainingDetectionService = OvertrainingDetectionService()
    ) {
        self.muscleRecoveryService = muscleRecoveryService
        self.readinessProgramIntegration = readinessProgramIntegration
        self.overtrainingDetectionService = overtrainingDetectionService
    }

    // MARK: - Public API

    /// Generate a complete workout plan for today.
    ///
    /// - Parameters:
    ///   - activeProgram: The user's active program (nil = auto-generate)
    ///   - workoutHistory: Recent workouts (14 days recommended)
    ///   - readinessScore: Today's readiness score (nil = assume Good/75)
    ///   - userProfile: User profile with experience/goals/frequency
    ///   - availableExercises: All exercises in the database for swap candidates
    ///   - timeConstraintMinutes: Target session length (30, 45, 60, 90)
    ///   - currentDate: For testing; defaults to now
    /// - Returns: A ``GeneratedWorkout`` with exercises, reasoning, and metadata
    public func generateWorkout(
        activeProgram: Program?,
        workoutHistory: [Workout],
        readinessScore: ReadinessScore?,
        userProfile: UserProfile?,
        availableExercises: [Exercise],
        timeConstraintMinutes: Int = 60,
        currentDate: Date = Date()
    ) -> GeneratedWorkout {
        var reasoning: [ReasoningStep] = []
        let experienceLevel = userProfile?.experienceLevel ?? .intermediate

        // Step 1: Compute muscle recovery map
        let recoveryMap = muscleRecoveryService.calculateRecoveryMap(
            workouts: workoutHistory,
            currentDate: currentDate
        )
        let fatiguedMuscles = Set(recoveryMap.filter { $0.value.status == .fatigued }.keys)
        let recoveringMuscles = Set(recoveryMap.filter { $0.value.status == .recovering }.keys)

        if !fatiguedMuscles.isEmpty {
            reasoning.append(ReasoningStep(
                factor: .muscleRecovery,
                summary: "Avoiding fatigued muscles: \(fatiguedMuscles.sorted().joined(separator: ", "))",
                impact: .exerciseSelection
            ))
        }

        // Step 2: Evaluate readiness
        let effectiveReadiness = readinessScore ?? defaultReadiness(date: currentDate)
        reasoning.append(ReasoningStep(
            factor: .readiness,
            summary: "Readiness: \(Int(effectiveReadiness.overallScore)) (\(effectiveReadiness.label.displayName))",
            impact: effectiveReadiness.overallScore < 40 ? .restDay : .volumeIntensity
        ))

        // Step 3: Check overtraining risk
        let overtrainingAnalysis = overtrainingDetectionService.analyze(
            workouts: workoutHistory,
            plateauAnalyses: [],
            recentReadinessScores: readinessScore.map { [$0] } ?? []
        )
        if let analysis = overtrainingAnalysis, analysis.riskLevel != .none {
            reasoning.append(ReasoningStep(
                factor: .overtraining,
                summary: "Overtraining risk: \(analysis.riskLevel.rawValue). \(analysis.recommendations.first ?? "")",
                impact: analysis.riskLevel == .high ? .restDay : .volumeIntensity
            ))
        }

        // Step 4: Resolve today's exercises from program or generate ad-hoc
        let candidateExercises: [PlannedExerciseSlot]
        let programDayName: String

        if let program = activeProgram, let todayDay = resolveToday(program: program, workoutHistory: workoutHistory) {
            candidateExercises = slotsFromProgramDay(todayDay)
            programDayName = todayDay.name
            reasoning.append(ReasoningStep(
                factor: .programTemplate,
                summary: "Following program \"\(program.name)\" — \(todayDay.name)",
                impact: .exerciseSelection
            ))
        } else {
            candidateExercises = generateAdHocExercises(
                availableExercises: availableExercises,
                workoutHistory: workoutHistory,
                fatiguedMuscles: fatiguedMuscles,
                experienceLevel: experienceLevel,
                currentDate: currentDate
            )
            programDayName = "Auto-Generated"
            reasoning.append(ReasoningStep(
                factor: .programTemplate,
                summary: "No active program — generating balanced session from exercise library",
                impact: .exerciseSelection
            ))
        }

        // Step 5: Apply readiness-based adjustments via ReadinessProgramIntegration
        let programDayInfo = ProgramDayInfo(
            name: programDayName,
            isHeavyDay: candidateExercises.contains { $0.targetRPE ?? 0 >= 8.0 },
            exercises: candidateExercises.compactMap { slot in
                guard let exercise = slot.exercise else { return nil }
                return ProgramExerciseInfo(
                    name: exercise.name,
                    primaryMuscles: exercise.muscleGroups.filter(\.isPrimary).map(\.name)
                )
            }
        )

        let recommendation = readinessProgramIntegration.adjustWorkout(
            programDay: programDayInfo,
            readiness: effectiveReadiness,
            muscleRecovery: recoveryMap.isEmpty ? nil : recoveryMap
        )

        // If rest day is recommended, return early
        if recommendation.action == .restDay {
            reasoning.append(ReasoningStep(
                factor: .readiness,
                summary: "Rest day recommended — readiness too low for productive training",
                impact: .restDay
            ))
            return GeneratedWorkout(
                exercises: [],
                reasoning: reasoning,
                recommendation: recommendation,
                estimatedDurationMinutes: 0,
                sessionName: "Rest Day",
                isRestDay: true
            )
        }

        // Step 6: Filter out exercises for fatigued muscles
        var filteredSlots = candidateExercises.filter { slot in
            guard let exercise = slot.exercise else { return false }
            let primaryMuscles = Set(exercise.muscleGroups.filter(\.isPrimary).map(\.name))
            let hasFatiguedPrimary = !primaryMuscles.intersection(fatiguedMuscles).isEmpty
            return !hasFatiguedPrimary
        }

        // Step 7: Filter yesterday's exercises to avoid back-to-back repeats
        let yesterdayExerciseIDs = exercisesTrainedYesterday(
            workoutHistory: workoutHistory,
            currentDate: currentDate
        )
        if !yesterdayExerciseIDs.isEmpty {
            let beforeCount = filteredSlots.count
            filteredSlots = filteredSlots.filter { slot in
                guard let exercise = slot.exercise else { return false }
                return !yesterdayExerciseIDs.contains(exercise.id)
            }
            let removed = beforeCount - filteredSlots.count
            if removed > 0 {
                reasoning.append(ReasoningStep(
                    factor: .exerciseHistory,
                    summary: "Excluded \(removed) exercise(s) trained yesterday to avoid back-to-back repetition",
                    impact: .exerciseSelection
                ))
            }
        }

        // Step 8: Fit to time budget
        let maxExercises = maxExercisesForDuration(minutes: timeConstraintMinutes)
        if filteredSlots.count > maxExercises {
            filteredSlots = prioritizeExercises(filteredSlots, limit: maxExercises)
            reasoning.append(ReasoningStep(
                factor: .timeConstraint,
                summary: "Trimmed to \(maxExercises) exercises to fit \(timeConstraintMinutes)-minute window",
                impact: .exerciseSelection
            ))
        }

        // Step 9: Apply intensity/volume adjustments from readiness integration
        var adjustedSlots = filteredSlots
        if let intensityAdj = recommendation.intensityAdjustment, intensityAdj < 0 {
            adjustedSlots = adjustedSlots.map { slot in
                var copy = slot
                let factor = 1.0 + (intensityAdj / 100.0)
                copy.adjustedWeightMultiplier = factor
                return copy
            }
            reasoning.append(ReasoningStep(
                factor: .readiness,
                summary: "Intensity reduced to \(Int(100 + (intensityAdj)))% based on readiness",
                impact: .volumeIntensity
            ))
        }
        if let volumeAdj = recommendation.volumeAdjustment, volumeAdj < 0 {
            adjustedSlots = adjustedSlots.map { slot in
                var copy = slot
                let factor = 1.0 + (volumeAdj / 100.0)
                copy.adjustedSetsMultiplier = max(0.5, factor)
                return copy
            }
            reasoning.append(ReasoningStep(
                factor: .readiness,
                summary: "Volume reduced by \(Int(-volumeAdj))% based on readiness",
                impact: .volumeIntensity
            ))
        }

        // Step 10: Handle recovering muscles — reduce volume but don't exclude
        adjustedSlots = adjustedSlots.map { slot in
            guard let exercise = slot.exercise else { return slot }
            let primaryMuscles = Set(exercise.muscleGroups.filter(\.isPrimary).map(\.name))
            if !primaryMuscles.intersection(recoveringMuscles).isEmpty {
                var copy = slot
                copy.adjustedSetsMultiplier = (copy.adjustedSetsMultiplier ?? 1.0) * 0.75
                return copy
            }
            return slot
        }

        // Build final exercise list
        let generatedExercises = adjustedSlots.enumerated().map { index, slot in
            let adjustedSets = Int(ceil(Double(slot.targetSets) * (slot.adjustedSetsMultiplier ?? 1.0)))
            let clampedSets = max(2, min(adjustedSets, slot.targetSets))

            return GeneratedExercise(
                exercise: slot.exercise,
                exerciseName: slot.exercise?.name ?? "Unknown",
                order: index + 1,
                targetSets: clampedSets,
                targetReps: slot.targetReps,
                targetRPE: slot.targetRPE,
                weightMultiplier: slot.adjustedWeightMultiplier ?? 1.0,
                notes: slot.notes
            )
        }

        let estimatedDuration = Self.warmupMinutes + Double(generatedExercises.count) * Self.minutesPerExercise

        Self.logger.info(
            "Generated workout: \(generatedExercises.count) exercises, ~\(Int(estimatedDuration)) min, readiness \(Int(effectiveReadiness.overallScore))"
        )

        return GeneratedWorkout(
            exercises: generatedExercises,
            reasoning: reasoning,
            recommendation: recommendation,
            estimatedDurationMinutes: Int(estimatedDuration),
            sessionName: programDayName,
            isRestDay: false
        )
    }

    /// Find a swap candidate for a given exercise, avoiding fatigued muscles and yesterday's exercises.
    public func findSwapCandidate(
        for exercise: Exercise,
        in availableExercises: [Exercise],
        workoutHistory: [Workout],
        currentDate: Date = Date()
    ) -> Exercise? {
        let recoveryMap = muscleRecoveryService.calculateRecoveryMap(
            workouts: workoutHistory,
            currentDate: currentDate
        )
        let fatiguedMuscles = Set(recoveryMap.filter { $0.value.status == .fatigued }.keys)
        let yesterdayIDs = exercisesTrainedYesterday(workoutHistory: workoutHistory, currentDate: currentDate)

        let primaryMuscles = Set(exercise.muscleGroups.filter(\.isPrimary).map(\.name))

        let candidates = availableExercises.filter { candidate in
            guard candidate.id != exercise.id else { return false }
            guard !yesterdayIDs.contains(candidate.id) else { return false }

            let candidatePrimary = Set(candidate.muscleGroups.filter(\.isPrimary).map(\.name))
            let sharesMuscle = !candidatePrimary.intersection(primaryMuscles).isEmpty
            let hitsFatigued = !candidatePrimary.intersection(fatiguedMuscles).isEmpty

            return sharesMuscle && !hitsFatigued
        }

        // Prefer same category and equipment
        let sorted = candidates.sorted { a, b in
            let aScore = (a.category == exercise.category ? 2 : 0) + (a.equipment == exercise.equipment ? 1 : 0)
            let bScore = (b.category == exercise.category ? 2 : 0) + (b.equipment == exercise.equipment ? 1 : 0)
            return aScore > bScore
        }

        return sorted.first
    }

    // MARK: - Program Day Resolution

    /// Determine which ProgramDay to use today based on workout history rotation.
    func resolveToday(program: Program, workoutHistory: [Workout]) -> ProgramDay? {
        let sortedDays = program.days.sorted { $0.dayNumber < $1.dayNumber }
        guard !sortedDays.isEmpty else { return nil }

        // Find last completed workout for this program
        let programWorkouts = workoutHistory
            .filter { $0.program?.id == program.id && !$0.isCancelled }
            .sorted { $0.date > $1.date }

        guard let lastWorkout = programWorkouts.first,
              let lastDay = lastWorkout.programDay else {
            return sortedDays.first
        }

        // Advance to next day in rotation
        let lastDayIndex = sortedDays.firstIndex(where: { $0.id == lastDay.id }) ?? 0
        let nextIndex = (lastDayIndex + 1) % sortedDays.count
        return sortedDays[nextIndex]
    }

    // MARK: - Ad-Hoc Generation (No Program)

    /// Generate a balanced session when no program is active.
    func generateAdHocExercises(
        availableExercises: [Exercise],
        workoutHistory: [Workout],
        fatiguedMuscles: Set<String>,
        experienceLevel: ExperienceLevel,
        currentDate: Date
    ) -> [PlannedExerciseSlot] {
        let yesterdayIDs = exercisesTrainedYesterday(workoutHistory: workoutHistory, currentDate: currentDate)

        // Prioritize fresh muscle groups; exclude fatigued
        let eligible = availableExercises.filter { exercise in
            let primaryMuscles = Set(exercise.muscleGroups.filter(\.isPrimary).map(\.name))
            let hitsFatigued = !primaryMuscles.intersection(fatiguedMuscles).isEmpty
            return !hitsFatigued && !yesterdayIDs.contains(exercise.id)
        }

        // Score exercises: prefer compounds first, then isolation, then accessory
        let scored = eligible.map { exercise -> (Exercise, Int) in
            var score = 0
            switch exercise.category {
            case .compound: score += 10
            case .isolation: score += 5
            case .accessory: score += 2
            }
            return (exercise, score)
        }.sorted { $0.1 > $1.1 }

        // Select diverse muscle coverage
        var selectedExercises: [Exercise] = []
        var coveredMuscles: Set<String> = []
        let targetCount = exerciseCountForExperience(experienceLevel)

        for (exercise, _) in scored {
            let primaryMuscles = Set(exercise.muscleGroups.filter(\.isPrimary).map(\.name))
            let newCoverage = !primaryMuscles.isSubset(of: coveredMuscles)

            if newCoverage || selectedExercises.count < targetCount {
                selectedExercises.append(exercise)
                coveredMuscles.formUnion(primaryMuscles)
            }

            if selectedExercises.count >= targetCount { break }
        }

        return selectedExercises.enumerated().map { index, exercise in
            let (sets, reps, rpe) = defaultPrescription(for: exercise.category, experience: experienceLevel)
            return PlannedExerciseSlot(
                exercise: exercise,
                order: index + 1,
                targetSets: sets,
                targetReps: reps,
                targetRPE: rpe,
                notes: ""
            )
        }
    }

    // MARK: - Helpers

    /// Convert ProgramDay planned exercises into slots.
    private func slotsFromProgramDay(_ day: ProgramDay) -> [PlannedExerciseSlot] {
        let exercises = day.plannedExercises.sorted { $0.order < $1.order }
        return exercises.map { planned in
            PlannedExerciseSlot(
                exercise: planned.exercise,
                order: planned.order,
                targetSets: planned.targetSets,
                targetReps: planned.targetReps,
                targetRPE: planned.targetRPE,
                notes: planned.notes
            )
        }
    }

    /// Get exercise IDs trained yesterday (within 24h before start of today).
    private func exercisesTrainedYesterday(
        workoutHistory: [Workout],
        currentDate: Date
    ) -> Set<UUID> {
        let calendar = Calendar.current
        let startOfToday = calendar.startOfDay(for: currentDate)
        let startOfYesterday = calendar.date(byAdding: .day, value: -1, to: startOfToday) ?? startOfToday

        var ids = Set<UUID>()
        for workout in workoutHistory where !workout.isCancelled {
            if workout.date >= startOfYesterday && workout.date < startOfToday {
                for exercise in workout.exercises {
                    ids.insert(exercise.id)
                }
            }
        }
        return ids
    }

    /// Maximum exercises that fit within a time budget.
    private func maxExercisesForDuration(minutes: Int) -> Int {
        let available = Double(minutes) - Self.warmupMinutes
        return max(2, Int(available / Self.minutesPerExercise))
    }

    /// Prioritize: compounds first, then isolation, then accessory.
    private func prioritizeExercises(_ slots: [PlannedExerciseSlot], limit: Int) -> [PlannedExerciseSlot] {
        let sorted = slots.sorted { a, b in
            let aCat = a.exercise?.category ?? .accessory
            let bCat = b.exercise?.category ?? .accessory
            return categoryPriority(aCat) > categoryPriority(bCat)
        }
        return Array(sorted.prefix(limit))
    }

    private func categoryPriority(_ category: ExerciseCategory) -> Int {
        switch category {
        case .compound: return 3
        case .isolation: return 2
        case .accessory: return 1
        }
    }

    /// Default exercise count based on experience level.
    private func exerciseCountForExperience(_ level: ExperienceLevel) -> Int {
        switch level {
        case .beginner: return 4
        case .intermediate: return 5
        case .advanced: return 6
        case .elite: return 7
        }
    }

    /// Default sets/reps/RPE for ad-hoc generation.
    private func defaultPrescription(
        for category: ExerciseCategory,
        experience: ExperienceLevel
    ) -> (sets: Int, reps: String, rpe: Double) {
        switch category {
        case .compound:
            switch experience {
            case .beginner: return (3, "8-10", 7.0)
            case .intermediate: return (4, "6-8", 7.5)
            case .advanced: return (4, "4-6", 8.0)
            case .elite: return (5, "3-5", 8.5)
            }
        case .isolation:
            return (3, "10-12", 7.0)
        case .accessory:
            return (3, "12-15", 6.5)
        }
    }

    /// Default readiness when no score is available (neutral 75).
    private func defaultReadiness(date: Date) -> ReadinessScore {
        ReadinessScore(
            date: date,
            overallScore: 75.0,
            sleepScore: 75.0,
            hrvScore: 75.0,
            restingHRScore: 75.0,
            trainingLoadScore: 75.0,
            recommendation: "No readiness data available — training with moderate defaults.",
            label: .good
        )
    }
}

// MARK: - Generated Workout Types

/// A fully generated workout session with reasoning trail.
public struct GeneratedWorkout {
    /// The exercises in this session, in order.
    public let exercises: [GeneratedExercise]
    /// Step-by-step reasoning for why these exercises were chosen.
    public let reasoning: [ReasoningStep]
    /// The readiness-based recommendation (proceed, lighter variant, rest day, etc.)
    public let recommendation: WorkoutRecommendation
    /// Estimated session duration in minutes.
    public let estimatedDurationMinutes: Int
    /// Display name for this session (program day name or "Auto-Generated").
    public let sessionName: String
    /// Whether the generator recommends a rest day instead of training.
    public let isRestDay: Bool

    public init(
        exercises: [GeneratedExercise],
        reasoning: [ReasoningStep],
        recommendation: WorkoutRecommendation,
        estimatedDurationMinutes: Int,
        sessionName: String,
        isRestDay: Bool
    ) {
        self.exercises = exercises
        self.reasoning = reasoning
        self.recommendation = recommendation
        self.estimatedDurationMinutes = estimatedDurationMinutes
        self.sessionName = sessionName
        self.isRestDay = isRestDay
    }

    /// Human-readable explanation of the generation reasoning.
    public var reasoningText: String {
        reasoning.map { "• \($0.summary)" }.joined(separator: "\n")
    }
}

/// A single exercise in the generated workout.
public struct GeneratedExercise: Identifiable {
    public let id = UUID()
    public let exercise: Exercise?
    public let exerciseName: String
    public let order: Int
    public let targetSets: Int
    public let targetReps: String
    public let targetRPE: Double?
    /// Multiplier applied to predicted weight (1.0 = no change, 0.8 = 20% reduction).
    public let weightMultiplier: Double
    public let notes: String

    public init(
        exercise: Exercise?,
        exerciseName: String,
        order: Int,
        targetSets: Int,
        targetReps: String,
        targetRPE: Double?,
        weightMultiplier: Double,
        notes: String
    ) {
        self.exercise = exercise
        self.exerciseName = exerciseName
        self.order = order
        self.targetSets = targetSets
        self.targetReps = targetReps
        self.targetRPE = targetRPE
        self.weightMultiplier = weightMultiplier
        self.notes = notes
    }
}

/// One reasoning step explaining a generation decision.
public struct ReasoningStep: Sendable {
    public let factor: ReasoningFactor
    public let summary: String
    public let impact: ReasoningImpact

    public init(factor: ReasoningFactor, summary: String, impact: ReasoningImpact) {
        self.factor = factor
        self.summary = summary
        self.impact = impact
    }
}

/// What factor influenced the decision.
public enum ReasoningFactor: String, Sendable {
    case muscleRecovery
    case readiness
    case overtraining
    case programTemplate
    case exerciseHistory
    case timeConstraint
    case userProfile
}

/// What aspect of the workout was affected.
public enum ReasoningImpact: String, Sendable {
    case exerciseSelection
    case volumeIntensity
    case restDay
}

/// Internal slot used during generation before final output.
struct PlannedExerciseSlot {
    let exercise: Exercise?
    let order: Int
    let targetSets: Int
    let targetReps: String
    let targetRPE: Double?
    let notes: String
    var adjustedWeightMultiplier: Double?
    var adjustedSetsMultiplier: Double?
}
