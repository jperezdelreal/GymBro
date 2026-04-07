import Foundation
import SwiftData
import os

public struct ExerciseDataSeeder {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ExerciseDataSeeder")
    private struct ExerciseSeedData: Codable {
        let name: String
        let category: String
        let equipment: String
        let instructions: String
        let muscleGroups: [MuscleGroupSeed]
    }
    
    private struct MuscleGroupSeed: Codable {
        let name: String
        let isPrimary: Bool
    }
    
    public static func seedExercises(modelContext: ModelContext) async throws {
        // Only seed if there are NO exercises in the database yet
        // This ensures we never overwrite user's custom exercises
        let alreadySeeded = try modelContext.fetch(FetchDescriptor<Exercise>()).count > 0
        
        guard !alreadySeeded else {
            logger.info("Exercises already seeded, skipping...")
            return
        }
        
        guard let url = Bundle.main.url(forResource: "exercises-seed", withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            logger.error("Failed to load exercises-seed.json")
            return
        }
        
        let decoder = JSONDecoder()
        let seedData = try decoder.decode([ExerciseSeedData].self, from: data)
        
        logger.info("Seeding \(seedData.count) exercises...")
        
        for exerciseData in seedData {
            let exercise = Exercise(
                name: exerciseData.name,
                category: ExerciseCategory(rawValue: exerciseData.category) ?? .compound,
                equipment: Equipment(rawValue: exerciseData.equipment) ?? .other,
                instructions: exerciseData.instructions,
                muscleGroups: exerciseData.muscleGroups.map { 
                    MuscleGroup(name: $0.name, isPrimary: $0.isPrimary)
                },
                isCustom: false
            )
            
            modelContext.insert(exercise)
        }
        
        try modelContext.save()
        logger.info("Successfully seeded \(seedData.count) exercises")
    }
}
