import Foundation
import SwiftData

@Model
final class Exercise {
    var id: UUID
    var createdAt: Date
    var updatedAt: Date
    
    var name: String
    var category: ExerciseCategory
    var equipment: Equipment
    var instructions: String
    
    @Relationship(deleteRule: .nullify)
    var muscleGroups: [MuscleGroup]
    
    @Relationship(deleteRule: .nullify, inverse: \ExerciseSet.exercise)
    var sets: [ExerciseSet]
    
    var isCustom: Bool
    
    init(
        id: UUID = UUID(),
        name: String,
        category: ExerciseCategory,
        equipment: Equipment,
        instructions: String = "",
        muscleGroups: [MuscleGroup] = [],
        isCustom: Bool = false
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.name = name
        self.category = category
        self.equipment = equipment
        self.instructions = instructions
        self.muscleGroups = muscleGroups
        self.sets = []
        self.isCustom = isCustom
    }
}

enum ExerciseCategory: String, Codable {
    case compound
    case isolation
    case accessory
}

enum Equipment: String, Codable {
    case barbell
    case dumbbell
    case kettlebell
    case machine
    case cable
    case bodyweight
    case band
    case other
}

@Model
final class MuscleGroup {
    var id: UUID
    var name: String
    var isPrimary: Bool
    
    init(id: UUID = UUID(), name: String, isPrimary: Bool = true) {
        self.id = id
        self.name = name
        self.isPrimary = isPrimary
    }
}
