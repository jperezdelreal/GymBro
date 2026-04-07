import Foundation
import SwiftData

/// Provides snapshot data for widgets by querying the shared SwiftData container.
/// Widgets use this to fetch workout stats without depending on the main app process.
@MainActor
public struct WidgetDataProvider {
    private let modelContext: ModelContext

    public init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Streak

    /// Returns the current consecutive-day workout streak.
    public func currentStreak() -> Int {
        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { $0.endTime != nil && !$0.isCancelled },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )

        guard let workouts = try? modelContext.fetch(descriptor), !workouts.isEmpty else {
            return 0
        }

        let calendar = Calendar.current
        var streak = 0
        var checkDate = calendar.startOfDay(for: Date())

        let workoutDates = Set(workouts.map { calendar.startOfDay(for: $0.date) })

        while workoutDates.contains(checkDate) {
            streak += 1
            guard let previous = calendar.date(byAdding: .day, value: -1, to: checkDate) else { break }
            checkDate = previous
        }

        return streak
    }

    /// Days since the most recent completed workout.
    public func daysSinceLastWorkout() -> Int {
        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { $0.endTime != nil && !$0.isCancelled },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )

        guard let workouts = try? modelContext.fetch(descriptor),
              let latest = workouts.first else {
            return 0
        }

        return Calendar.current.dateComponents([.day], from: latest.date, to: Date()).day ?? 0
    }

    // MARK: - Weekly Volume

    /// Total volume (kg) for the current calendar week.
    public func weeklyVolume() -> Double {
        let calendar = Calendar.current
        let startOfWeek = calendar.dateInterval(of: .weekOfYear, for: Date())?.start ?? Date()

        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { $0.endTime != nil && !$0.isCancelled && $0.date >= startOfWeek }
        )

        guard let workouts = try? modelContext.fetch(descriptor) else { return 0 }
        return workouts.reduce(0) { $0 + $1.totalVolume }
    }

    /// Number of completed workouts this calendar week.
    public func weeklyWorkoutCount() -> Int {
        let calendar = Calendar.current
        let startOfWeek = calendar.dateInterval(of: .weekOfYear, for: Date())?.start ?? Date()

        let descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate<Workout> { $0.endTime != nil && !$0.isCancelled && $0.date >= startOfWeek }
        )

        return (try? modelContext.fetch(descriptor))?.count ?? 0
    }

    // MARK: - Next Workout

    /// Returns info about the next scheduled workout from an active program, if any.
    public func nextScheduledWorkout() -> NextWorkoutInfo? {
        // Query active programs to find the next scheduled day
        let descriptor = FetchDescriptor<Program>(
            predicate: #Predicate<Program> { $0.isActive }
        )

        guard let programs = try? modelContext.fetch(descriptor),
              let program = programs.first,
              let nextDay = program.days.first else {
            return nil
        }

        return NextWorkoutInfo(
            name: nextDay.name,
            scheduledDate: nil,
            exerciseCount: nextDay.plannedExercises.count
        )
    }

    // MARK: - Recent PRs

    /// Number of personal records set in the last 7 days.
    public func recentPRCount() -> Int {
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()

        let descriptor = FetchDescriptor<ExerciseSet>(
            predicate: #Predicate<ExerciseSet> {
                $0.completedAt != nil && $0.createdAt >= weekAgo && $0.setType == .working
            }
        )

        // Simplified PR approximation: count sets that are the highest e1RM for their exercise
        guard let sets = try? modelContext.fetch(descriptor) else { return 0 }

        var prCount = 0
        let byExercise = Dictionary(grouping: sets) { $0.exercise?.id }

        for (_, exerciseSets) in byExercise {
            if let best = exerciseSets.max(by: { $0.estimatedOneRepMax < $1.estimatedOneRepMax }) {
                // Check if this is truly a PR against all-time history
                if let exerciseId = best.exercise?.id {
                    let allTimeDescriptor = FetchDescriptor<ExerciseSet>(
                        predicate: #Predicate<ExerciseSet> {
                            $0.exercise?.id == exerciseId && $0.completedAt != nil && $0.setType == .working
                        }
                    )
                    if let allSets = try? modelContext.fetch(allTimeDescriptor) {
                        let allTimeMax = allSets.map(\.estimatedOneRepMax).max() ?? 0
                        if best.estimatedOneRepMax >= allTimeMax {
                            prCount += 1
                        }
                    }
                }
            }
        }

        return prCount
    }
}

// MARK: - Data Transfer Types

public struct NextWorkoutInfo {
    public let name: String
    public let scheduledDate: Date?
    public let exerciseCount: Int
}

public struct WidgetSnapshot {
    public let streak: Int
    public let daysSinceLastWorkout: Int
    public let weeklyVolume: Double
    public let weeklyWorkoutCount: Int
    public let recentPRs: Int
    public let nextWorkout: NextWorkoutInfo?
    public let readinessScore: Int // 0-100

    public static let placeholder = WidgetSnapshot(
        streak: 5,
        daysSinceLastWorkout: 0,
        weeklyVolume: 12500,
        weeklyWorkoutCount: 4,
        recentPRs: 2,
        nextWorkout: NextWorkoutInfo(name: "Push Day", scheduledDate: nil, exerciseCount: 6),
        readinessScore: 85
    )
}
