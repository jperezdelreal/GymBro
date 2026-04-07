import Foundation
import SwiftData
import Observation
import GymBroCore
import os

/// ViewModel for the workout generation UI.
///
/// Provides a reactive interface for generating, adjusting, and starting workouts.
/// Wraps ``WorkoutGeneratorService`` and manages UI state for exercise swaps,
/// duration changes, and reasoning display.
@MainActor
@Observable
public final class WorkoutGeneratorViewModel {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WorkoutGeneratorVM")

    // MARK: - Dependencies

    private let modelContext: ModelContext
    private let generatorService: WorkoutGeneratorService

    // MARK: - Published State

    /// The most recently generated workout plan.
    public private(set) var generatedWorkout: GeneratedWorkout?

    /// Whether a generation is in progress.
    public private(set) var isGenerating: Bool = false

    /// Human-readable reasoning for the current workout.
    public var reasoningText: String {
        generatedWorkout?.reasoningText ?? "Tap Generate to create today's workout."
    }

    /// Whether the generated workout is a rest day recommendation.
    public var isRestDay: Bool {
        generatedWorkout?.isRestDay ?? false
    }

    /// Error message from the last generation attempt.
    public var errorMessage: String?

    /// Current time constraint in minutes.
    public private(set) var durationMinutes: Int = 60

    // MARK: - Init

    public init(modelContext: ModelContext, generatorService: WorkoutGeneratorService? = nil) {
        self.modelContext = modelContext
        self.generatorService = generatorService ?? WorkoutGeneratorService()
    }

    // MARK: - Public API

    /// Generate today's workout based on all available context.
    public func generateWorkout() {
        isGenerating = true
        errorMessage = nil

        let activeProgram = fetchActiveProgram()
        let workoutHistory = fetchRecentWorkouts(days: 14)
        let readinessScore = fetchLatestReadiness()
        let userProfile = fetchUserProfile()
        let availableExercises = fetchAllExercises()

        let result = generatorService.generateWorkout(
            activeProgram: activeProgram,
            workoutHistory: workoutHistory,
            readinessScore: readinessScore,
            userProfile: userProfile,
            availableExercises: availableExercises,
            timeConstraintMinutes: durationMinutes
        )

        generatedWorkout = result
        isGenerating = false

        Self.logger.info(
            "Generated workout: \(result.exercises.count) exercises, rest day: \(result.isRestDay)"
        )
    }

    /// Swap a single exercise at a given index with a suitable alternative.
    public func swapExercise(at index: Int, with replacement: Exercise? = nil) {
        guard var workout = generatedWorkout,
              index >= 0, index < workout.exercises.count else { return }

        let current = workout.exercises[index]
        let availableExercises = fetchAllExercises()
        let workoutHistory = fetchRecentWorkouts(days: 14)

        let swap: Exercise?
        if let replacement = replacement {
            swap = replacement
        } else if let currentExercise = current.exercise {
            swap = generatorService.findSwapCandidate(
                for: currentExercise,
                in: availableExercises,
                workoutHistory: workoutHistory
            )
        } else {
            swap = nil
        }

        guard let newExercise = swap else {
            errorMessage = "No suitable swap found for \(current.exerciseName)"
            return
        }

        var updatedExercises = workout.exercises
        updatedExercises[index] = GeneratedExercise(
            exercise: newExercise,
            exerciseName: newExercise.name,
            order: current.order,
            targetSets: current.targetSets,
            targetReps: current.targetReps,
            targetRPE: current.targetRPE,
            weightMultiplier: current.weightMultiplier,
            notes: "Swapped from \(current.exerciseName)"
        )

        var updatedReasoning = workout.reasoning
        updatedReasoning.append(ReasoningStep(
            factor: .exerciseHistory,
            summary: "Swapped \(current.exerciseName) → \(newExercise.name) (user preference)",
            impact: .exerciseSelection
        ))

        generatedWorkout = GeneratedWorkout(
            exercises: updatedExercises,
            reasoning: updatedReasoning,
            recommendation: workout.recommendation,
            estimatedDurationMinutes: workout.estimatedDurationMinutes,
            sessionName: workout.sessionName,
            isRestDay: workout.isRestDay
        )

        Self.logger.info("Swapped exercise at index \(index): \(current.exerciseName) → \(newExercise.name)")
    }

    /// Adjust the time constraint and regenerate the workout.
    public func adjustDuration(minutes: Int) {
        let clamped = max(15, min(minutes, 120))
        durationMinutes = clamped
        generateWorkout()

        Self.logger.info("Duration adjusted to \(clamped) minutes — regenerating")
    }

    /// Convert the generated workout into an actual Workout model ready for logging.
    public func startWorkout() -> Workout? {
        guard let generated = generatedWorkout, !generated.isRestDay else { return nil }

        let workout = Workout(date: Date())
        workout.isActive = true
        workout.startTime = Date()
        workout.notes = "Generated: \(generated.sessionName)"

        for genExercise in generated.exercises {
            guard let exercise = genExercise.exercise else { continue }

            for setNum in 1...genExercise.targetSets {
                let set = ExerciseSet(
                    exercise: exercise,
                    workout: workout,
                    weightKg: 0, // User fills in or SmartDefaults provides
                    reps: parseMinReps(genExercise.targetReps),
                    setType: .working,
                    setNumber: setNum
                )
                workout.sets.append(set)
            }
        }

        modelContext.insert(workout)

        Self.logger.info("Started workout from generated plan: \(generated.exercises.count) exercises")
        return workout
    }

    // MARK: - Data Fetching

    private func fetchActiveProgram() -> Program? {
        let descriptor = FetchDescriptor<Program>(
            predicate: #Predicate<Program> { $0.isActive }
        )
        return try? modelContext.fetch(descriptor).first
    }

    private func fetchRecentWorkouts(days: Int) -> [Workout] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -days, to: Date()) ?? Date()
        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { $0.date >= cutoff },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    private func fetchLatestReadiness() -> ReadinessScore? {
        let descriptor = FetchDescriptor<ReadinessScore>(
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return try? modelContext.fetch(descriptor).first
    }

    private func fetchUserProfile() -> UserProfile? {
        let descriptor = FetchDescriptor<UserProfile>()
        return try? modelContext.fetch(descriptor).first
    }

    private func fetchAllExercises() -> [Exercise] {
        let descriptor = FetchDescriptor<Exercise>()
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    // MARK: - Helpers

    /// Parse minimum reps from a range string like "8-10" → 8.
    private func parseMinReps(_ repsString: String) -> Int {
        let components = repsString.components(separatedBy: "-")
        return Int(components.first ?? "8") ?? 8
    }
}
