import Foundation
import SwiftData
import os

public final class SmartDefaultsService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "SmartDefaults")

    private let modelContext: ModelContext
    
    public init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }
    
    public func getSmartDefaults(for exercise: Exercise) -> (weight: Double, reps: Int) {
        let descriptor = FetchDescriptor<ExerciseSet>(
            predicate: #Predicate<ExerciseSet> { set in
                set.exercise?.id == exercise.id && 
                set.setType == .working &&
                set.completedAt != nil
            },
            sortBy: [SortDescriptor(\.completedAt, order: .reverse)]
        )
        
        let recentSets: [ExerciseSet]
        do {
            recentSets = try modelContext.fetch(descriptor)
        } catch {
            Self.logger.error("Failed to fetch smart defaults: \(error.localizedDescription)")
            return defaultValues(for: exercise)
        }
        
        guard !recentSets.isEmpty else {
            return defaultValues(for: exercise)
        }
        
        let lastWorkout = recentSets.prefix(while: { set in
            guard let firstSetWorkout = recentSets.first?.workout,
                  let currentSetWorkout = set.workout else { return false }
            return firstSetWorkout.id == currentSetWorkout.id
        })
        
        guard !lastWorkout.isEmpty else {
            return defaultValues(for: exercise)
        }
        
        let avgWeight = lastWorkout.map(\.weightKg).reduce(0, +) / Double(lastWorkout.count)
        let totalReps = lastWorkout.map(\.reps).reduce(0, +)
        let avgReps = Int((Double(totalReps) / Double(lastWorkout.count)).rounded())
        
        let progressionWeight = applyProgression(weight: avgWeight, for: exercise)
        
        return (weight: progressionWeight, reps: avgReps)
    }
    
    private func applyProgression(weight: Double, for exercise: Exercise) -> Double {
        switch exercise.category {
        case .compound:
            return weight + 2.5
        case .isolation:
            return weight + 1.25
        case .accessory:
            return weight
        }
    }
    
    private func defaultValues(for exercise: Exercise) -> (weight: Double, reps: Int) {
        switch exercise.category {
        case .compound:
            switch exercise.equipment {
            case .barbell:
                return (60.0, 5)
            case .dumbbell:
                return (20.0, 8)
            default:
                return (40.0, 8)
            }
        case .isolation:
            return (15.0, 10)
        case .accessory:
            return (10.0, 12)
        }
    }
}
