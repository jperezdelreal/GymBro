import Foundation
import SwiftData
import os

@MainActor
public struct WorkoutRecoveryService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WorkoutRecovery")
    
    private let modelContext: ModelContext
    
    public init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }
    
    /// Returns the most recent unfinished workout, if any.
    public func findUnfinishedWorkout() -> Workout? {
        var descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { workout in
                workout.isActive && workout.endTime == nil
            },
            sortBy: [SortDescriptor(\.updatedAt, order: .reverse)]
        )
        descriptor.fetchLimit = 1
        
        do {
            let results = try modelContext.fetch(descriptor)
            if let workout = results.first {
                Self.logger.info("Found unfinished workout from \(workout.date)")
                return workout
            }
        } catch {
            Self.logger.error("Failed to query unfinished workouts: \(error.localizedDescription)")
        }
        return nil
    }
    
    /// Discards an unfinished workout by marking it cancelled.
    public func discardWorkout(_ workout: Workout) {
        workout.endTime = Date()
        workout.isActive = false
        workout.isCancelled = true
        workout.updatedAt = Date()
        do {
            try modelContext.save()
            Self.logger.info("Discarded workout \(workout.id)")
        } catch {
            Self.logger.error("Failed to discard workout: \(error.localizedDescription)")
        }
    }
    
    /// Marks all stale active workouts as cancelled (cleanup on launch).
    public func cleanupStaleWorkouts(excluding workoutId: UUID? = nil) {
        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { workout in
                workout.isActive && workout.endTime == nil
            }
        )
        
        do {
            let staleWorkouts = try modelContext.fetch(descriptor)
            for workout in staleWorkouts where workout.id != workoutId {
                workout.endTime = Date()
                workout.isActive = false
                workout.isCancelled = true
                workout.updatedAt = Date()
            }
            try modelContext.save()
            if staleWorkouts.count > 1 {
                Self.logger.info("Cleaned up \(staleWorkouts.count - 1) stale workouts")
            }
        } catch {
            Self.logger.error("Failed to cleanup stale workouts: \(error.localizedDescription)")
        }
    }
}
