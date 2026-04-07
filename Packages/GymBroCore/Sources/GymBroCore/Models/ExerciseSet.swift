import Foundation
import SwiftData

@Model
public public final class ExerciseSet {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    public var completedAt: Date?
    
    @Relationship(deleteRule: .nullify)
    public var exercise: Exercise?
    
    @Relationship(deleteRule: .nullify)
    public var workout: Workout?
    
    public var weightKg: Double
    public var reps: Int
    public var rpe: Double?
    public var restSeconds: Int
    public var setType: SetType
    public var setNumber: Int
    
    /// For rest-pause sets: stores sub-set reps (e.g., [8, 4, 2])
    /// Total reps = sum of subSetReps
    public var subSetReps: [Int]?
    
    @Relationship(deleteRule: .nullify)
    public var supersetGroup: SupersetGroup?
    
    public init(
        id: UUID = UUID(),
        exercise: Exercise? = nil,
        workout: Workout? = nil,
        weightKg: Double,
        reps: Int,
        rpe: Double? = nil,
        restSeconds: Int = 120,
        setType: SetType = .working,
        setNumber: Int = 1,
        subSetReps: [Int]? = nil,
        supersetGroup: SupersetGroup? = nil
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
        self.subSetReps = subSetReps
        self.supersetGroup = supersetGroup
    }
    
    public var isWarmup: Bool {
        setType == .warmup
    }
    
    public var isRestPause: Bool {
        setType == .restPause
    }
    
    /// Total reps for display (sum of sub-sets for rest-pause, otherwise reps)
    public var totalReps: Int {
        if isRestPause, let subReps = subSetReps, !subReps.isEmpty {
            return subReps.reduce(0, +)
        }
        return reps
    }
    
    public var volume: Double {
        Double(reps) * weightKg
    }
    
    public var estimatedOneRepMax: Double {
        guard reps > 0 else { return weightKg }
        return weightKg * (1 + Double(reps) / 30.0)
    }
    
    public func weightInUnit(_ unit: UnitSystem) -> Double {
        switch unit {
        case .metric:
            return weightKg
        case .imperial:
            return weightKg * 2.20462
        }
    }
}

public public enum SetType: String, Codable {
    case warmup
    case working
    case drop
    case backoff
    case amrap
    case restPause
}
