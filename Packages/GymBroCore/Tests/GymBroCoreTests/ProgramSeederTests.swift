import XCTest
import SwiftData
@testable import GymBroCore

final class ProgramSeederTests: XCTestCase {
    var modelContainer: ModelContainer!
    var modelContext: ModelContext!
    
    override func setUp() async throws {
        let schema = Schema([
            Program.self,
            ProgramDay.self,
            ProgramWeek.self,
            PlannedExercise.self,
            Exercise.self
        ])
        
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        modelContainer = try ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(modelContainer)
        
        // Seed exercises first (required for program seeding)
        try await ExerciseDataSeeder.seedExercises(modelContext: modelContext)
    }
    
    override func tearDown() {
        modelContainer = nil
        modelContext = nil
    }
    
    func testProgramSeederCreatesPrograms() async throws {
        // Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        XCTAssertGreaterThan(programs.count, 0, "Should create at least one program")
        XCTAssertTrue(programs.allSatisfy { !$0.isCustom }, "Seeded programs should not be marked as custom")
    }
    
    func testProgramSeederDoesNotDuplicateOnSecondRun() async throws {
        // Arrange
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        let firstCount = try modelContext.fetch(FetchDescriptor<Program>()).count
        
        // Act - run seeder again
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let secondCount = try modelContext.fetch(FetchDescriptor<Program>()).count
        XCTAssertEqual(firstCount, secondCount, "Should not duplicate programs on second run")
    }
    
    func testFivThreeOneProgramStructure() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let fiveThreeOne = programs.first { $0.name.contains("5/3/1") }
        
        XCTAssertNotNil(fiveThreeOne, "Should have 5/3/1 program")
        XCTAssertEqual(fiveThreeOne?.durationWeeks, 4, "5/3/1 should be 4 weeks")
        XCTAssertEqual(fiveThreeOne?.frequencyPerWeek, 4, "5/3/1 should be 4 days/week")
        XCTAssertEqual(fiveThreeOne?.periodizationType, .block, "5/3/1 should use block periodization")
        XCTAssertEqual(fiveThreeOne?.days.count, 4, "5/3/1 should have 4 program days")
    }
    
    func testFiveThreeOneWeekVariations() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let fiveThreeOne = programs.first { $0.name.contains("5/3/1") }
        
        let squatDay = fiveThreeOne?.days.first { $0.name.contains("Squat") }
        XCTAssertNotNil(squatDay, "Should have squat day")
        XCTAssertEqual(squatDay?.weeks.count, 4, "Squat day should have 4 week variations")
        
        // Verify week-level progression
        let week1 = squatDay?.weeks.first { $0.weekNumber == 1 }
        let week2 = squatDay?.weeks.first { $0.weekNumber == 2 }
        let week3 = squatDay?.weeks.first { $0.weekNumber == 3 }
        let week4 = squatDay?.weeks.first { $0.weekNumber == 4 }
        
        XCTAssertNotNil(week1, "Should have week 1")
        XCTAssertNotNil(week2, "Should have week 2")
        XCTAssertNotNil(week3, "Should have week 3")
        XCTAssertNotNil(week4, "Should have deload week 4")
        
        // Verify deload week has lower RPE
        let week1RPE = week1?.plannedExercises.first?.targetRPE ?? 0
        let week4RPE = week4?.plannedExercises.first?.targetRPE ?? 0
        XCTAssertLessThan(week4RPE, week1RPE, "Deload week should have lower RPE than week 1")
    }
    
    func testPPLProgramStructure() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let ppl = programs.first { $0.name.contains("PPL") }
        
        XCTAssertNotNil(ppl, "Should have PPL program")
        XCTAssertEqual(ppl?.frequencyPerWeek, 6, "PPL should be 6 days/week")
        XCTAssertEqual(ppl?.periodizationType, .linear, "PPL should use linear periodization")
        XCTAssertEqual(ppl?.days.count, 6, "PPL should have 6 program days")
    }
    
    func testGZCLProgramStructure() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let gzcl = programs.first { $0.name.contains("GZCL") }
        
        XCTAssertNotNil(gzcl, "Should have GZCL program")
        XCTAssertEqual(gzcl?.frequencyPerWeek, 4, "GZCL should be 4 days/week")
        XCTAssertEqual(gzcl?.days.count, 4, "GZCL should have 4 program days")
    }
    
    func testStartingStrengthProgramStructure() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let ss = programs.first { $0.name.contains("Starting Strength") }
        
        XCTAssertNotNil(ss, "Should have Starting Strength program")
        XCTAssertEqual(ss?.frequencyPerWeek, 3, "SS should be 3 days/week")
        XCTAssertEqual(ss?.periodizationType, .linear, "SS should use linear periodization")
        XCTAssertEqual(ss?.days.count, 2, "SS should have 2 program days (A/B split)")
    }
    
    func testUpperLowerProgramStructure() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let upperLower = programs.first { $0.name.contains("Upper/Lower") }
        
        XCTAssertNotNil(upperLower, "Should have Upper/Lower program")
        XCTAssertEqual(upperLower?.frequencyPerWeek, 4, "Upper/Lower should be 4 days/week")
        XCTAssertEqual(upperLower?.periodizationType, .undulating, "Upper/Lower should use undulating periodization")
        XCTAssertEqual(upperLower?.days.count, 4, "Upper/Lower should have 4 program days")
    }
    
    func testFullBodyProgramStructure() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        let fullBody = programs.first { $0.name.contains("Full Body") }
        
        XCTAssertNotNil(fullBody, "Should have Full Body program")
        XCTAssertEqual(fullBody?.frequencyPerWeek, 3, "Full Body should be 3 days/week")
        XCTAssertEqual(fullBody?.periodizationType, .undulating, "Full Body should use undulating periodization")
        XCTAssertEqual(fullBody?.days.count, 3, "Full Body should have 3 program days")
    }
    
    func testAllProgramsHaveValidExercises() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        
        for program in programs {
            for day in program.days {
                for week in day.weeks {
                    for plannedExercise in week.plannedExercises {
                        XCTAssertNotNil(plannedExercise.exercise, "All planned exercises should have a linked exercise in program \(program.name)")
                        XCTAssertGreaterThan(plannedExercise.targetSets, 0, "All exercises should have at least 1 set in program \(program.name)")
                        XCTAssertFalse(plannedExercise.targetReps.isEmpty, "All exercises should have target reps in program \(program.name)")
                    }
                }
            }
        }
    }
    
    func testAllProgramsHaveMetadata() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        
        for program in programs {
            XCTAssertFalse(program.name.isEmpty, "Program should have a name")
            XCTAssertFalse(program.programDescription.isEmpty, "Program \(program.name) should have a description")
            XCTAssertGreaterThan(program.durationWeeks, 0, "Program \(program.name) should have duration")
            XCTAssertGreaterThan(program.frequencyPerWeek, 0, "Program \(program.name) should have frequency")
        }
    }
    
    func testRPEValuesAreRealistic() async throws {
        // Arrange & Act
        try await ProgramSeeder.seedPrograms(modelContext: modelContext)
        
        // Assert
        let programs = try modelContext.fetch(FetchDescriptor<Program>())
        
        for program in programs {
            for day in program.days {
                for week in day.weeks {
                    for plannedExercise in week.plannedExercises {
                        if let rpe = plannedExercise.targetRPE {
                            XCTAssertTrue(rpe >= 5.0 && rpe <= 10.0, 
                                        "RPE should be between 5-10 in program \(program.name), found \(rpe)")
                        }
                    }
                }
            }
        }
    }
}
