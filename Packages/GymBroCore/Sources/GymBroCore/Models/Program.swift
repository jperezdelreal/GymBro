import Foundation
import SwiftData

@Model
final class Program {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    
    var name: String
    var programDescription: String
    var durationWeeks: Int
    var frequencyPerWeek: Int
    var periodizationType: PeriodizationType
    var isActive: Bool
    var isCustom: Bool
    
    @Relationship(deleteRule: .cascade, inverse: \ProgramDay.program)
    var days: [ProgramDay]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.program)
    var workouts: [Workout]
    
    init(
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

enum PeriodizationType: String, Codable {
    case linear
    case undulating
    case block
    case autoregulated
}

@Model
final class ProgramDay {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    
    var dayNumber: Int
    var name: String
    var dayDescription: String
    
    @Relationship(deleteRule: .nullify)
    var program: Program?
    
    @Relationship(deleteRule: .cascade)
    var plannedExercises: [PlannedExercise]
    
    @Relationship(deleteRule: .nullify, inverse: \Workout.programDay)
    var workouts: [Workout]
    
    init(
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
final class PlannedExercise {
    var id: UUID
    var order: Int
    
    @Relationship(deleteRule: .nullify)
    var exercise: Exercise?
    
    var targetSets: Int
    var targetReps: String
    var targetRPE: Double?
    var notes: String
    
    init(
        id: UUID = UUID(),
        order: Int,
        exercise: Exercise? = nil,
        targetSets: Int,
        targetReps: String,
        targetRPE: Double? = nil,
        notes: String = ""
    ) {
        self.id = id
        self.order = order
        self.exercise = exercise
        self.targetSets = targetSets
        self.targetReps = targetReps
        self.targetRPE = targetRPE
        self.notes = notes
    }
}
