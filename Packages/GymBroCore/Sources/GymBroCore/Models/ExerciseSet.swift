import Foundation
import SwiftData

@Model
final class ExerciseSet {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    var completedAt: Date?
    
    @Relationship(deleteRule: .nullify)
    var exercise: Exercise?
    
    @Relationship(deleteRule: .nullify)
    var workout: Workout?
    
    var weightKg: Double
    var reps: Int
    var rpe: Double?
    var restSeconds: Int
    var setType: SetType
    var setNumber: Int
    
    init(
        id: UUID = UUID(),
        exercise: Exercise? = nil,
        workout: Workout? = nil,
        weightKg: Double,
        reps: Int,
        rpe: Double? = nil,
        restSeconds: Int = 120,
        setType: SetType = .working,
        setNumber: Int = 1
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.exercise = exercise
        self.workout = workout
        self.weightKg = weightKg
        self.reps = reps
        self.rpe = rpe
        self.restSeconds = restSeconds
        self.setType = setType
        self.setNumber = setNumber
    }
    
    var isWarmup: Bool {
        setType == .warmup
    }
    
    var volume: Double {
        Double(reps) * weightKg
    }
    
    var estimatedOneRepMax: Double {
        guard reps > 0 else { return weightKg }
        return weightKg * (1 + Double(reps) / 30.0)
    }
    
    func weightInUnit(_ unit: UnitSystem) -> Double {
        switch unit {
        case .metric:
            return weightKg
        case .imperial:
            return weightKg * 2.20462
        }
    }
}

enum SetType: String, Codable {
    case warmup
    case working
    case drop
    case backoff
    case amrap
}
