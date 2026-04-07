import Foundation
import SwiftData

@Model
public final class Program {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    public var name: String
    public var programDescription: String
    public var durationWeeks: Int
    public var frequencyPerWeek: Int
    public var periodizationType: PeriodizationType
    public var isActive: Bool
    public var isCustom: Bool
    
    @Relationship(deleteRule: .cascade, inverse: \ProgramDay.program)
    public var days: [ProgramDay]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.program)
    public var workouts: [Workout]
    
    public init(
        id: UUID = UUID(),
        name: String,
        programDescription: String = "",
        durationWeeks: Int,
        frequencyPerWeek: Int,
        periodizationType: PeriodizationType = .linear,
        isActive: Bool = false,
        isCustom: Bool = true
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.name = name
        self.programDescription = programDescription
        self.durationWeeks = durationWeeks
        self.frequencyPerWeek = frequencyPerWeek
        self.periodizationType = periodizationType
        self.isActive = isActive
        self.isCustom = isCustom
        self.days = []
        self.workouts = []
    }
}

public enum PeriodizationType: String, Codable {
    case linear
    case undulating
    case block
    case autoregulated
}

@Model
public final class ProgramDay {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    public var dayNumber: Int
    public var name: String
    public var dayDescription: String
    
    @Relationship(deleteRule: .nullify)
    public var program: Program?
    
    @Relationship(deleteRule: .cascade, inverse: \PlannedExercise.programDay)
    public var plannedExercises: [PlannedExercise]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.programDay)
    public var workouts: [Workout]
    
    public init(
        id: UUID = UUID(),
        dayNumber: Int,
        name: String,
        dayDescription: String = "",
        program: Program? = nil
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.dayNumber = dayNumber
        self.name = name
        self.dayDescription = dayDescription
        self.program = program
        self.plannedExercises = []
        self.workouts = []
    }
}

@Model
public final class PlannedExercise {
    public var id: UUID
    public var order: Int
    
    @Relationship(deleteRule: .nullify)
    public var exercise: Exercise?
    
    @Relationship(deleteRule: .nullify)
    public var programDay: ProgramDay?
    
    public var targetSets: Int
    public var targetReps: String
    public var targetRPE: Double?
    public var notes: String
    
    public init(
        id: UUID = UUID(),
        order: Int,
        exercise: Exercise? = nil,
        programDay: ProgramDay? = nil,
        targetSets: Int,
        targetReps: String,
        targetRPE: Double? = nil,
        notes: String = ""
    ) {
        self.id = id
        self.order = order
        self.exercise = exercise
        self.programDay = programDay
        self.targetSets = targetSets
        self.targetReps = targetReps
        self.targetRPE = targetRPE
        self.notes = notes
    }
}
