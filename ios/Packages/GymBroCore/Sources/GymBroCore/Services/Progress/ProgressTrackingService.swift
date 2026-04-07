import Foundation
import SwiftData

// MARK: - Data Transfer Objects

/// A single data point for charting e1RM over time.
public struct E1RMDataPoint: Identifiable, Sendable {
    public let id: UUID
    public let date: Date
    public let e1rm: Double
    public let weight: Double
    public let reps: Int
    public let exerciseName: String

    public init(id: UUID = UUID(), date: Date, e1rm: Double, weight: Double, reps: Int, exerciseName: String) {
        self.id = id
        self.date = date
        self.e1rm = e1rm
        self.weight = weight
        self.reps = reps
        self.exerciseName = exerciseName
    }
}

/// Aggregated volume data for a time period.
public struct VolumeDataPoint: Identifiable, Sendable {
    public let id: UUID
    public let periodStart: Date
    public let totalVolume: Double   // sets × reps × weight
    public let totalTonnage: Double  // total weight moved
    public let totalSets: Int
    public let totalReps: Int

    public init(id: UUID = UUID(), periodStart: Date, totalVolume: Double, totalTonnage: Double, totalSets: Int, totalReps: Int) {
        self.id = id
        self.periodStart = periodStart
        self.totalVolume = totalVolume
        self.totalTonnage = totalTonnage
        self.totalSets = totalSets
        self.totalReps = totalReps
    }
}

/// Frequency data point — workouts per period.
public struct FrequencyDataPoint: Identifiable, Sendable {
    public let id: UUID
    public let periodStart: Date
    public let workoutCount: Int
    public let exercisesByMuscleGroup: [String: Int]

    public init(id: UUID = UUID(), periodStart: Date, workoutCount: Int, exercisesByMuscleGroup: [String: Int] = [:]) {
        self.id = id
        self.periodStart = periodStart
        self.workoutCount = workoutCount
        self.exercisesByMuscleGroup = exercisesByMuscleGroup
    }
}

/// Muscle group balance — volume distribution across muscle groups.
public struct MuscleGroupBalance: Identifiable, Sendable {
    public let id: UUID
    public let muscleGroup: String
    public let totalVolume: Double
    public let percentage: Double

    public init(id: UUID = UUID(), muscleGroup: String, totalVolume: Double, percentage: Double) {
        self.id = id
        self.muscleGroup = muscleGroup
        self.totalVolume = totalVolume
        self.percentage = percentage
    }
}

/// A detected personal record with context.
public struct PREvent: Identifiable, Sendable {
    public let id: UUID
    public let date: Date
    public let exerciseName: String
    public let recordType: String   // "e1RM", "Weight", "Reps", "Volume"
    public let value: Double
    public let previousBest: Double

    public init(id: UUID = UUID(), date: Date, exerciseName: String, recordType: String, value: Double, previousBest: Double) {
        self.id = id
        self.date = date
        self.exerciseName = exerciseName
        self.recordType = recordType
        self.value = value
        self.previousBest = previousBest
    }
}

/// Supported time windows for filtering progress data.
public enum TimeWindow: String, CaseIterable, Sendable {
    case oneWeek = "1W"
    case oneMonth = "1M"
    case threeMonths = "3M"
    case sixMonths = "6M"
    case oneYear = "1Y"
    case allTime = "All"

    public var days: Int? {
        switch self {
        case .oneWeek: return 7
        case .oneMonth: return 30
        case .threeMonths: return 90
        case .sixMonths: return 180
        case .oneYear: return 365
        case .allTime: return nil
        }
    }

    public func startDate(from reference: Date = Date()) -> Date? {
        guard let days else { return nil }
        return Calendar.current.date(byAdding: .day, value: -days, to: reference)
    }
}

// MARK: - Service

/// Computes progress metrics from workout history.
/// Designed to be called on-demand or via nightly batch processing.
public class ProgressTrackingService {
    private let calculator: E1RMCalculator

    public init(calculator: E1RMCalculator = E1RMCalculator()) {
        self.calculator = calculator
    }

    // MARK: - e1RM Trends

    /// Computes best e1RM per session for an exercise within a time window.
    /// Returns data points sorted chronologically for charting.
    public func e1rmTrend(
        sets: [ExerciseSet],
        exerciseName: String,
        timeWindow: TimeWindow,
        formula: E1RMFormula = .epley
    ) -> [E1RMDataPoint] {
        let filtered = filterByTimeWindow(sets: sets, timeWindow: timeWindow)

        // Group by workout date, pick best e1RM per session
        let grouped = Dictionary(grouping: filtered) { set in
            Calendar.current.startOfDay(for: set.workout?.date ?? set.createdAt)
        }

        return grouped.compactMap { (date, daySets) -> E1RMDataPoint? in
            let workingSets = daySets.filter { $0.setType == .working }
            guard !workingSets.isEmpty else { return nil }

            let best = workingSets.max(by: {
                calculator.calculate(weight: $0.weightKg, reps: $0.reps, formula: formula) <
                calculator.calculate(weight: $1.weightKg, reps: $1.reps, formula: formula)
            })!

            let e1rm = calculator.calculate(weight: best.weightKg, reps: best.reps, formula: formula)

            return E1RMDataPoint(
                date: date,
                e1rm: e1rm,
                weight: best.weightKg,
                reps: best.reps,
                exerciseName: exerciseName
            )
        }
        .sorted { $0.date < $1.date }
    }

    // MARK: - Volume Load

    /// Aggregates weekly volume (sets × reps × weight) for given sets.
    public func weeklyVolume(sets: [ExerciseSet], timeWindow: TimeWindow) -> [VolumeDataPoint] {
        let filtered = filterByTimeWindow(sets: sets, timeWindow: timeWindow)
            .filter { $0.setType == .working }

        let grouped = Dictionary(grouping: filtered) { set in
            self.startOfWeek(for: set.workout?.date ?? set.createdAt)
        }

        return grouped.map { (weekStart, weekSets) in
            let totalVolume = weekSets.reduce(0.0) { $0 + $1.volume }
            let totalTonnage = weekSets.reduce(0.0) { $0 + $1.weightKg * Double($1.reps) }
            let totalSets = weekSets.count
            let totalReps = weekSets.reduce(0) { $0 + $1.reps }

            return VolumeDataPoint(
                periodStart: weekStart,
                totalVolume: totalVolume,
                totalTonnage: totalTonnage,
                totalSets: totalSets,
                totalReps: totalReps
            )
        }
        .sorted { $0.periodStart < $1.periodStart }
    }

    // MARK: - Tonnage

    /// Aggregates total tonnage per workout for charting.
    public func tonnagePerWorkout(workouts: [Workout], timeWindow: TimeWindow) -> [VolumeDataPoint] {
        let startDate = timeWindow.startDate()

        let filtered: [Workout]
        if let start = startDate {
            filtered = workouts.filter { $0.date >= start }
        } else {
            filtered = workouts
        }

        return filtered.map { workout in
            let workingSets = workout.sets.filter { $0.setType == .working }
            let tonnage = workingSets.reduce(0.0) { $0 + $1.weightKg * Double($1.reps) }
            let volume = workingSets.reduce(0.0) { $0 + $1.volume }

            return VolumeDataPoint(
                periodStart: workout.date,
                totalVolume: volume,
                totalTonnage: tonnage,
                totalSets: workingSets.count,
                totalReps: workingSets.reduce(0) { $0 + $1.reps }
            )
        }
        .sorted { $0.periodStart < $1.periodStart }
    }

    // MARK: - Frequency

    /// Calculates workout frequency per week.
    public func weeklyFrequency(workouts: [Workout], timeWindow: TimeWindow) -> [FrequencyDataPoint] {
        let startDate = timeWindow.startDate()

        let filtered: [Workout]
        if let start = startDate {
            filtered = workouts.filter { $0.date >= start }
        } else {
            filtered = workouts
        }

        let grouped = Dictionary(grouping: filtered) { workout in
            self.startOfWeek(for: workout.date)
        }

        return grouped.map { (weekStart, weekWorkouts) in
            var muscleGroupCounts: [String: Int] = [:]
            for workout in weekWorkouts {
                for set in workout.sets where set.setType == .working {
                    if let exercise = set.exercise {
                        for mg in exercise.muscleGroups {
                            muscleGroupCounts[mg.name, default: 0] += 1
                        }
                    }
                }
            }

            return FrequencyDataPoint(
                periodStart: weekStart,
                workoutCount: weekWorkouts.count,
                exercisesByMuscleGroup: muscleGroupCounts
            )
        }
        .sorted { $0.periodStart < $1.periodStart }
    }

    // MARK: - Muscle Group Balance

    /// Computes volume distribution across muscle groups.
    public func muscleGroupBalance(sets: [ExerciseSet], timeWindow: TimeWindow) -> [MuscleGroupBalance] {
        let filtered = filterByTimeWindow(sets: sets, timeWindow: timeWindow)
            .filter { $0.setType == .working }

        var volumeByGroup: [String: Double] = [:]

        for set in filtered {
            guard let exercise = set.exercise else { continue }
            for mg in exercise.muscleGroups {
                let factor: Double = mg.isPrimary ? 1.0 : 0.5
                volumeByGroup[mg.name, default: 0] += set.volume * factor
            }
        }

        let totalVolume = volumeByGroup.values.reduce(0, +)
        guard totalVolume > 0 else { return [] }

        return volumeByGroup.map { (group, volume) in
            MuscleGroupBalance(
                muscleGroup: group,
                totalVolume: volume,
                percentage: (volume / totalVolume) * 100.0
            )
        }
        .sorted { $0.totalVolume > $1.totalVolume }
    }

    // MARK: - PR Detection

    /// Scans sets chronologically and detects when new e1RM/weight records are set.
    public func detectPRs(sets: [ExerciseSet], exerciseName: String) -> [PREvent] {
        let workingSets = sets
            .filter { $0.setType == .working }
            .sorted { ($0.workout?.date ?? $0.createdAt) < ($1.workout?.date ?? $1.createdAt) }

        var events: [PREvent] = []
        var bestE1RM: Double = 0
        var bestWeight: Double = 0

        for set in workingSets {
            let e1rm = calculator.calculate(weight: set.weightKg, reps: set.reps)
            let date = set.workout?.date ?? set.createdAt

            if e1rm > bestE1RM {
                let previousBest = bestE1RM
                bestE1RM = e1rm
                if previousBest > 0 {
                    events.append(PREvent(
                        date: date,
                        exerciseName: exerciseName,
                        recordType: "e1RM",
                        value: e1rm,
                        previousBest: previousBest
                    ))
                }
            }

            if set.weightKg > bestWeight {
                let previousBest = bestWeight
                bestWeight = set.weightKg
                if previousBest > 0 {
                    events.append(PREvent(
                        date: date,
                        exerciseName: exerciseName,
                        recordType: "Weight",
                        value: set.weightKg,
                        previousBest: previousBest
                    ))
                }
            }
        }

        return events.sorted { $0.date < $1.date }
    }

    // MARK: - Helpers

    private func filterByTimeWindow(sets: [ExerciseSet], timeWindow: TimeWindow) -> [ExerciseSet] {
        guard let startDate = timeWindow.startDate() else { return sets }
        return sets.filter { ($0.workout?.date ?? $0.createdAt) >= startDate }
    }

    private func startOfWeek(for date: Date) -> Date {
        var calendar = Calendar.current
        calendar.firstWeekday = 2 // Monday
        let components = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: date)
        return calendar.date(from: components) ?? date
    }
}
