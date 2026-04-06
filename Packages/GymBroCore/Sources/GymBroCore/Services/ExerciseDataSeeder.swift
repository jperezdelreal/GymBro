import Foundation
import SwiftData

public struct ExerciseDataSeeder {
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
        let alreadySeeded = try modelContext.fetch(FetchDescriptor<Exercise>()).count > 0
        
        guard !alreadySeeded else {
            print("Exercises already seeded, skipping...")
            return
        }
        
        guard let url = Bundle.main.url(forResource: "exercises-seed", withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            print("Failed to load exercises-seed.json")
            return
        }
        
        let decoder = JSONDecoder()
        let seedData = try decoder.decode([ExerciseSeedData].self, from: data)
        
        print("Seeding \(seedData.count) exercises...")
        
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
        print("Successfully seeded \(seedData.count) exercises")
    }
}
