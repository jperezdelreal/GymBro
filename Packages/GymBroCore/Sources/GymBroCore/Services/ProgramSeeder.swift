import Foundation
import SwiftData
import os

public struct ProgramSeeder {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ProgramSeeder")
    
    private struct ProgramSeedData: Codable {
        let name: String
        let programDescription: String
        let durationWeeks: Int
        let frequencyPerWeek: Int
        let periodizationType: String
        let targetAudience: String
        let expectedOutcome: String
        let progressionScheme: String
        let days: [ProgramDaySeed]
    }
    
    private struct ProgramDaySeed: Codable {
        let dayNumber: Int
        let name: String
        let dayDescription: String
        let weekVariations: [WeekVariationSeed]
    }
    
    private struct WeekVariationSeed: Codable {
        let weekNumber: Int
        let exercises: [PlannedExerciseSeed]
    }
    
    private struct PlannedExerciseSeed: Codable {
        let order: Int
        let exerciseName: String
        let targetSets: Int
        let targetReps: String
        let targetRPE: Double?
        let notes: String
    }
    
    public static func seedPrograms(modelContext: ModelContext) async throws {
        // Only seed if there are NO programs in the database yet
        // This ensures we never overwrite user's custom programs
        let descriptor = FetchDescriptor<Program>(predicate: #Predicate { !$0.isCustom })
        let alreadySeeded = try modelContext.fetch(descriptor).count > 0
        
        guard !alreadySeeded else {
            logger.info("Programs already seeded, skipping...")
            return
        }
        
        guard let url = Bundle.main.url(forResource: "programs-seed", withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            logger.error("Failed to load programs-seed.json")
            return
        }
        
        let decoder = JSONDecoder()
        let seedData = try decoder.decode([ProgramSeedData].self, from: data)
        
        logger.info("Seeding \(seedData.count) programs...")
        
        // Fetch all exercises to map names to Exercise objects
        let exerciseDescriptor = FetchDescriptor<Exercise>()
        let exercises = try modelContext.fetch(exerciseDescriptor)
        let exercisesByName = Dictionary(uniqueKeysWithValues: exercises.map { ($0.name, $0) })
        
        for programData in seedData {
            let program = Program(
                name: programData.name,
                programDescription: programData.programDescription,
                durationWeeks: programData.durationWeeks,
                frequencyPerWeek: programData.frequencyPerWeek,
                periodizationType: PeriodizationType(rawValue: programData.periodizationType) ?? .linear,
                isActive: false,
                isCustom: false,
                targetAudience: programData.targetAudience,
                expectedOutcome: programData.expectedOutcome,
                progressionScheme: programData.progressionScheme
            )
            
            modelContext.insert(program)
            
            // Create ProgramDays and their week variations
            for dayData in programData.days {
                let programDay = ProgramDay(
                    dayNumber: dayData.dayNumber,
                    name: dayData.name,
                    dayDescription: dayData.dayDescription,
                    program: program
                )
                
                modelContext.insert(programDay)
                program.days.append(programDay)
                
                // Create week variations
                for weekData in dayData.weekVariations {
                    let programWeek = ProgramWeek(
                        weekNumber: weekData.weekNumber,
                        programDay: programDay
                    )
                    
                    modelContext.insert(programWeek)
                    programDay.weeks.append(programWeek)
                    
                    // Create planned exercises for this week
                    for exerciseData in weekData.exercises {
                        guard let exercise = exercisesByName[exerciseData.exerciseName] else {
                            logger.warning("Exercise not found: \(exerciseData.exerciseName)")
                            continue
                        }
                        
                        let plannedExercise = PlannedExercise(
                            order: exerciseData.order,
                            exercise: exercise,
                            programDay: programDay,
                            programWeek: programWeek,
                            targetSets: exerciseData.targetSets,
                            targetReps: exerciseData.targetReps,
                            targetRPE: exerciseData.targetRPE,
                            notes: exerciseData.notes
                        )
                        
                        modelContext.insert(plannedExercise)
                        programWeek.plannedExercises.append(plannedExercise)
                    }
                }
            }
        }
        
        try modelContext.save()
        logger.info("Successfully seeded \(seedData.count) programs")
    }
}
