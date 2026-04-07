import Foundation
import SwiftData
import Observation
import GymBroCore
import os

@MainActor
@Observable
public final class ActiveWorkoutViewModel {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ActiveWorkout")

    private let modelContext: ModelContext
    private let smartDefaultsService: SmartDefaultsService
    private let supersetService = SupersetService.shared
    
    public private(set) var workout: Workout
    public private(set) var activeExercise: Exercise?
    public private(set) var activeSetNumber: Int = 1
    public private(set) var currentWeight: Double = 0
    public private(set) var currentReps: Int = 0
    public private(set) var currentRPE: Double?
    public private(set) var isWarmup: Bool = false
    public private(set) var restTimerEndTime: Date?
    public private(set) var isRestTimerActive: Bool = false
    public var saveError: String?
    
    private var restTimerSeconds: Int = 120
    
    public var completedSetsForActiveExercise: [ExerciseSet] {
        workout.sets
            .filter { $0.exercise?.id == activeExercise?.id && $0.completedAt != nil }
            .sorted { $0.setNumber < $1.setNumber }
    }
    
    public var isWorkoutStarted: Bool {
        workout.startTime != nil
    }
    
    public var workoutDuration: TimeInterval {
        guard let startTime = workout.startTime else { return 0 }
        return Date().timeIntervalSince(startTime)
    }
    
    public var totalVolume: Double {
        workout.totalVolume
    }
    
    public var totalCompletedSets: Int {
        workout.sets.filter { $0.completedAt != nil && !$0.isWarmup }.count
    }
    
    public init(
        modelContext: ModelContext,
        workout: Workout,
        exercises: [Exercise] = []
    ) {
        self.modelContext = modelContext
        self.workout = workout
        self.smartDefaultsService = SmartDefaultsService(modelContext: modelContext)
        
        if !exercises.isEmpty {
            self.activeExercise = exercises.first
            if let exercise = exercises.first {
                loadSmartDefaults(for: exercise)
            }
        }
    }
    
    public func startWorkout() {
        guard workout.startTime == nil else { return }
        workout.startTime = Date()
        workout.isActive = true
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to save workout start: \(error.localizedDescription)")
            saveError = "Failed to save workout. Please try again."
        }

        // Start Live Activity for Dynamic Island + Lock Screen
        LiveActivityService.shared.startWorkoutActivity(workoutId: workout.id.uuidString)
    }
    
    public func setActiveExercise(_ exercise: Exercise) {
        activeExercise = exercise
        activeSetNumber = (completedSetsForActiveExercise.last?.setNumber ?? 0) + 1
        loadSmartDefaults(for: exercise)
    }
    
    public func updateWeight(_ weight: Double) {
        currentWeight = max(0, min(weight, 999))
    }
    
    public func updateReps(_ reps: Int) {
        currentReps = max(0, min(reps, 999))
    }
    
    public func updateRPE(_ rpe: Double?) {
        guard let rpe else { currentRPE = nil; return }
        currentRPE = max(1, min(rpe, 10))
    }
    
    public func toggleWarmup() {
        isWarmup.toggle()
    }
    
    public func completeSet() {
        guard let exercise = activeExercise else { return }
        
        if !isWorkoutStarted {
            startWorkout()
        }
        
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: currentWeight,
            reps: currentReps,
            rpe: currentRPE,
            restSeconds: restTimerSeconds,
            setType: isWarmup ? .warmup : .working,
            setNumber: activeSetNumber
        )
        set.completedAt = Date()
        
        modelContext.insert(set)
        workout.sets.append(set)
        workout.updatedAt = Date()
        
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to save completed set: \(error.localizedDescription)")
            saveError = "Failed to save set. Your data may not be persisted."
        }
        
        HapticFeedbackService.shared.setCompleted()
        
        if isPR(set: set) {
            HapticFeedbackService.shared.personalRecordAchieved()
        }
        
        activeSetNumber += 1
        
        if !isWarmup {
            // Check if we should start rest timer (normal behavior or after completing superset round)
            let lastSet = workout.sets.last
            if lastSet != nil && supersetService.shouldStartRestTimer(after: lastSet!, in: workout) {
                startRestTimer()
            }
        }
        
        if isWarmup {
            isWarmup = false
        }

        // Update Live Activity with new set info
        updateLiveActivityState()
    }
    
    public func addExercise(_ exercise: Exercise) {
        if activeExercise == nil {
            setActiveExercise(exercise)
        }
    }

    /// Undo a completed set — clears completedAt, re-adjusts set counter.
    public func undoSetCompletion(_ set: ExerciseSet) {
        set.completedAt = nil
        set.updatedAt = Date()
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to undo set: \(error.localizedDescription)")
            saveError = "Failed to undo set."
        }
        activeSetNumber = (completedSetsForActiveExercise.last?.setNumber ?? 0) + 1
        HapticFeedbackService.shared.lightImpact()
    }

    /// Change the type of a completed set (drop, warmup, amrap, etc).
    public func changeSetType(_ set: ExerciseSet, to newType: SetType) {
        set.setType = newType
        set.updatedAt = Date()
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to change set type: \(error.localizedDescription)")
            saveError = "Failed to update set type."
        }
        HapticFeedbackService.shared.lightImpact()
    }

    /// Delete a set from the workout.
    public func deleteSet(_ set: ExerciseSet) {
        workout.sets.removeAll { $0.id == set.id }
        modelContext.delete(set)
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to delete set: \(error.localizedDescription)")
            saveError = "Failed to delete set."
        }
        activeSetNumber = (completedSetsForActiveExercise.last?.setNumber ?? 0) + 1
        HapticFeedbackService.shared.mediumImpact()
    }
    
    public func finishWorkout() -> WorkoutSummary {
        workout.endTime = Date()
        workout.isActive = false
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to save finished workout: \(error.localizedDescription)")
            saveError = "Failed to save workout. Your data may not be persisted."
        }
        
        let summary = WorkoutSummary(
            duration: workout.duration ?? 0,
            totalVolume: workout.totalVolume,
            totalSets: workout.totalSets,
            personalRecords: countPersonalRecords()
        )

        // End Live Activity with summary
        LiveActivityService.shared.endWorkoutActivity(
            totalSets: summary.totalSets,
            totalVolume: summary.totalVolume,
            durationSeconds: Int(summary.duration)
        )
        
        return summary
    }
    
    public func cancelWorkout() {
        workout.endTime = Date()
        workout.isActive = false
        workout.isCancelled = true
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to save cancelled workout: \(error.localizedDescription)")
            saveError = "Failed to cancel workout. Your data may not be persisted."
        }

        // Dismiss Live Activity immediately
        LiveActivityService.shared.dismissWorkoutActivity()
    }
    
    private func loadSmartDefaults(for exercise: Exercise) {
        let defaults = smartDefaultsService.getSmartDefaults(for: exercise)
        currentWeight = defaults.weight
        currentReps = defaults.reps
        currentRPE = nil
        isWarmup = false
    }
    
    private func startRestTimer() {
        restTimerEndTime = Date().addingTimeInterval(TimeInterval(restTimerSeconds))
        isRestTimerActive = true

        // Push rest timer to Live Activity / Dynamic Island
        if let exercise = activeExercise {
            LiveActivityService.shared.updateRestTimer(
                exerciseName: exercise.name,
                currentSetNumber: activeSetNumber,
                restDuration: restTimerSeconds,
                completedSets: totalCompletedSets,
                totalVolume: totalVolume,
                elapsedSeconds: Int(workoutDuration),
                lastWeight: currentWeight,
                lastReps: currentReps
            )
        }
    }
    
    public func skipRestTimer() {
        restTimerEndTime = nil
        isRestTimerActive = false

        // Clear rest timer from Live Activity
        if let exercise = activeExercise {
            LiveActivityService.shared.clearRestTimer(
                exerciseName: exercise.name,
                currentSetNumber: activeSetNumber,
                completedSets: totalCompletedSets,
                totalVolume: totalVolume,
                elapsedSeconds: Int(workoutDuration),
                lastWeight: currentWeight,
                lastReps: currentReps
            )
        }
    }
    
    private func updateLiveActivityState() {
        guard let exercise = activeExercise else { return }
        LiveActivityService.shared.updateWorkoutActivity(
            exerciseName: exercise.name,
            currentSetNumber: activeSetNumber,
            completedSets: totalCompletedSets,
            totalVolume: totalVolume,
            elapsedSeconds: Int(workoutDuration),
            lastWeight: currentWeight,
            lastReps: currentReps
        )
    }

    private func isPR(set: ExerciseSet) -> Bool {
        guard let exercise = activeExercise else { return false }
        
        let descriptor = FetchDescriptor<ExerciseSet>(
            predicate: #Predicate<ExerciseSet> { historicSet in
                historicSet.exercise?.id == exercise.id && 
                historicSet.completedAt != nil &&
                historicSet.setType == .working
            }
        )
        
        let historicalSets: [ExerciseSet]
        do {
            historicalSets = try modelContext.fetch(descriptor)
        } catch {
            Self.logger.error("Failed to fetch historical sets for PR check: \(error.localizedDescription)")
            return true
        }
        
        let maxE1RM = historicalSets.map(\.estimatedOneRepMax).max() ?? 0
        return set.estimatedOneRepMax > maxE1RM
    }
    
    private func countPersonalRecords() -> Int {
        workout.sets.filter { set in
            isPR(set: set)
        }.count
    }
    
    // MARK: - Superset Support
    
    /// Create a superset group linking the active exercise with another exercise
    public func createSuperset(with exercise: Exercise) {
        guard let activeEx = activeExercise else { return }
        
        let group = supersetService.createSupersetGroup(
            exercises: [activeEx, exercise],
            workout: workout,
            modelContext: modelContext
        )
        
        // Add existing sets to the superset group
        let existingSets = workout.sets.filter { 
            $0.exercise?.id == activeEx.id || $0.exercise?.id == exercise.id 
        }
        for set in existingSets {
            supersetService.addSetToSuperset(set: set, group: group)
        }
        
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to create superset: \(error.localizedDescription)")
            saveError = "Failed to create superset."
        }
        HapticFeedbackService.shared.mediumImpact()
    }
    
    /// Remove an exercise from its superset group
    public func removeFromSuperset(exercise: Exercise) {
        let sets = workout.sets.filter { $0.exercise?.id == exercise.id }
        for set in sets {
            supersetService.removeSetFromSuperset(set: set, modelContext: modelContext)
        }
        
        workout.updatedAt = Date()
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to remove from superset: \(error.localizedDescription)")
            saveError = "Failed to remove from superset."
        }
        HapticFeedbackService.shared.lightImpact()
    }
    
    /// Get exercises supersetted with the given exercise
    public func getSupersetPartners(for exercise: Exercise) -> [Exercise] {
        supersetService.getSupersetPartners(for: exercise, in: workout)
    }
    
    // MARK: - Rest-Pause Support
    
    /// Complete a rest-pause set with sub-set reps
    public func completeRestPauseSet(subSetReps: [Int]) {
        guard let exercise = activeExercise else { return }
        
        if !isWorkoutStarted {
            startWorkout()
        }
        
        let totalReps = subSetReps.reduce(0, +)
        
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: currentWeight,
            reps: totalReps,
            rpe: currentRPE,
            restSeconds: restTimerSeconds,
            setType: .restPause,
            setNumber: activeSetNumber,
            subSetReps: subSetReps
        )
        set.completedAt = Date()
        
        modelContext.insert(set)
        workout.sets.append(set)
        workout.updatedAt = Date()
        
        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to save rest-pause set: \(error.localizedDescription)")
            saveError = "Failed to save set. Your data may not be persisted."
        }
        
        HapticFeedbackService.shared.setCompleted()
        
        if isPR(set: set) {
            HapticFeedbackService.shared.personalRecordAchieved()
        }
        
        activeSetNumber += 1
        startRestTimer()
        
        updateLiveActivityState()
    }
}

public struct WorkoutSummary {
    public let duration: TimeInterval
    public let totalVolume: Double
    public let totalSets: Int
    public let personalRecords: Int
}
