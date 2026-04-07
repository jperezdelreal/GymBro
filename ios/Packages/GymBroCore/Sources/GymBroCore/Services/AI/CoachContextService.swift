import Foundation
import SwiftData

/// Provides workout context stats for the AI Coach context indicator bar.
@MainActor
public final class CoachContextService {

    private let modelContext: ModelContext

    public init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    /// Returns a summary of the user's tracked data for the context indicator.
    public func fetchContextSummary() -> CoachContextSummary {
        let workoutCount = countWorkouts()
        let weeksOfData = calculateWeeksOfData()
        let lastWorkoutDate = findLastWorkoutDate()
        return CoachContextSummary(
            workoutCount: workoutCount,
            weeksOfData: weeksOfData,
            lastWorkoutDate: lastWorkoutDate
        )
    }

    private func countWorkouts() -> Int {
        var descriptor = FetchDescriptor<Workout>()
        descriptor.predicate = #Predicate<Workout> { workout in
            workout.endTime != nil && !workout.isCancelled
        }
        return (try? modelContext.fetchCount(descriptor)) ?? 0
    }

    private func calculateWeeksOfData() -> Int {
        var descriptor = FetchDescriptor<Workout>(
            sortBy: [SortDescriptor(\.createdAt, order: .forward)]
        )
        descriptor.predicate = #Predicate<Workout> { workout in
            workout.endTime != nil && !workout.isCancelled
        }
        descriptor.fetchLimit = 1

        guard let oldest = try? modelContext.fetch(descriptor).first else { return 0 }
        let days = Calendar.current.dateComponents([.day], from: oldest.createdAt, to: Date()).day ?? 0
        return max(1, days / 7)
    }

    private func findLastWorkoutDate() -> Date? {
        var descriptor = FetchDescriptor<Workout>(
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        descriptor.predicate = #Predicate<Workout> { workout in
            workout.endTime != nil && !workout.isCancelled
        }
        descriptor.fetchLimit = 1
        return try? modelContext.fetch(descriptor).first?.createdAt
    }
}

/// Lightweight summary for displaying in the context indicator bar.
public struct CoachContextSummary: Sendable {
    public let workoutCount: Int
    public let weeksOfData: Int
    public let lastWorkoutDate: Date?

    public init(workoutCount: Int = 0, weeksOfData: Int = 0, lastWorkoutDate: Date? = nil) {
        self.workoutCount = workoutCount
        self.weeksOfData = weeksOfData
        self.lastWorkoutDate = lastWorkoutDate
    }
}
