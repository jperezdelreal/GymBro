import Foundation
import SwiftData

@Model
public final class Exercise {
    public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    
    public var name: String
    public var category: ExerciseCategory
    public var equipment: Equipment
    public var instructions: String
    public var defaultRestSeconds: Int?
    
    @Relationship(deleteRule: .nullify)
    public var muscleGroups: [MuscleGroup]
    
    @Relationship(deleteRule: .nullify, inverse: \ExerciseSet.exercise)
    public var sets: [ExerciseSet]
    
    public var isCustom: Bool
    public var source: ExerciseSource
    public var wgerId: Int?
    public var lastSyncedAt: Date?
    
    public init(
        id: UUID = UUID(),
        name: String,
        category: ExerciseCategory,
        equipment: Equipment,
        instructions: String = "",
        muscleGroups: [MuscleGroup] = [],
        isCustom: Bool = false,
        defaultRestSeconds: Int? = nil,
        source: ExerciseSource = .seed,
        wgerId: Int? = nil
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
        self.defaultRestSeconds = defaultRestSeconds
        self.source = source
        self.wgerId = wgerId
        self.lastSyncedAt = nil
    }
    
    public var restTime: Int {
        if let defaultRestSeconds = defaultRestSeconds {
            return defaultRestSeconds
        }
        
        switch category {
        case .compound:
            return 180 // 3 minutes
        case .isolation:
            return 90  // 90 seconds
        case .accessory:
            return 60  // 60 seconds
        }
    }
}

public enum ExerciseSource: String, Codable {
    case seed
    case wger
    case custom
}

public enum ExerciseCategory: String, Codable {
    case compound
    case isolation
    case accessory
}

public enum Equipment: String, Codable {
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
public final class MuscleGroup {
    public var id: UUID
    public var name: String
    public var isPrimary: Bool
    
    public init(id: UUID = UUID(), name: String, isPrimary: Bool = true) {
        self.id = id
        self.name = name
        self.isPrimary = isPrimary
    }
}
