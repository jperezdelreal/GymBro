import XCTest
@testable import GymBroCore

final class ReadinessProgramIntegrationTests: XCTestCase {
    var integration: ReadinessProgramIntegration!
    
    override func setUp() {
        super.setUp()
        integration = ReadinessProgramIntegration()
    }
    
    override func tearDown() {
        integration = nil
        super.tearDown()
    }
    
    // MARK: - Proceed As Planned Tests
    
    func testProceedAsPlannedWhenReadinessGood() {
        let programDay = makeProgramDay(name: "Upper Body", isHeavy: false)
        let readiness = makeReadiness(score: 75)
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .proceedAsPlanned)
        XCTAssertNil(result.intensityAdjustment)
        XCTAssertNil(result.volumeAdjustment)
        XCTAssertTrue(result.exerciseReplacements.isEmpty)
    }
    
    func testProceedAsPlannedWhenReadinessExcellent() {
        let programDay = makeProgramDay(name: "Heavy Squat Day", isHeavy: true)
        let readiness = makeReadiness(score: 92)
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .proceedAsPlanned)
    }
    
    func testProceedAsPlannedForLightDayEvenWithModerateReadiness() {
        // Light day + moderate readiness = OK to proceed
        let programDay = makeProgramDay(name: "Light Accessories", isHeavy: false)
        let readiness = makeReadiness(score: 55)
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .proceedAsPlanned)
    }
    
    // MARK: - Lighter Variant Tests
    
    func testSuggestsLighterVariantWhenReadinessModerateAndHeavyDay() {
        let programDay = makeProgramDay(name: "Heavy Deadlift", isHeavy: true)
        let readiness = makeReadiness(score: 55) // Below 60 threshold
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .lighterVariant)
        XCTAssertNotNil(result.intensityAdjustment)
        XCTAssertNotNil(result.volumeAdjustment)
        XCTAssertLessThan(result.intensityAdjustment ?? 0, 0, "Intensity should be reduced")
        XCTAssertLessThan(result.volumeAdjustment ?? 0, 0, "Volume should be reduced")
    }
    
    func testIntensityReductionScalesWithReadiness() {
        let programDay = makeProgramDay(name: "Heavy Bench", isHeavy: true)
        
        // Score 58 → 20% reduction
        let readiness58 = makeReadiness(score: 58)
        let result58 = integration.adjustWorkout(programDay: programDay, readiness: readiness58)
        
        // Score 52 → 30% reduction
        let readiness52 = makeReadiness(score: 52)
        let result52 = integration.adjustWorkout(programDay: programDay, readiness: readiness52)
        
        // Score 45 → 40% reduction
        let readiness45 = makeReadiness(score: 45)
        let result45 = integration.adjustWorkout(programDay: programDay, readiness: readiness45)
        
        XCTAssertNotNil(result58.intensityAdjustment)
        XCTAssertNotNil(result52.intensityAdjustment)
        XCTAssertNotNil(result45.intensityAdjustment)
        
        // Lower readiness should have larger reduction
        XCTAssertLessThan(
            result52.intensityAdjustment ?? 0,
            result58.intensityAdjustment ?? 0
        )
        XCTAssertLessThan(
            result45.intensityAdjustment ?? 0,
            result52.intensityAdjustment ?? 0
        )
    }
    
    func testVolumeReductionScalesWithReadiness() {
        let programDay = makeProgramDay(name: "Heavy Squat", isHeavy: true)
        
        let highReadiness = makeReadiness(score: 58)
        let lowReadiness = makeReadiness(score: 45)
        
        let resultHigh = integration.adjustWorkout(programDay: programDay, readiness: highReadiness)
        let resultLow = integration.adjustWorkout(programDay: programDay, readiness: lowReadiness)
        
        // Lower readiness should have larger volume reduction
        XCTAssertLessThan(
            resultLow.volumeAdjustment ?? 0,
            resultHigh.volumeAdjustment ?? 0
        )
    }
    
    func testLighterVariantIncludesRationale() {
        let programDay = makeProgramDay(name: "Heavy Day", isHeavy: true)
        let readiness = makeReadiness(score: 55)
        
        let result = integration.adjustWorkout(programDay: programDay, readiness: readiness)
        
        XCTAssertFalse(result.rationale.isEmpty)
        XCTAssertTrue(result.rationale.contains("Moderate") || result.rationale.contains("55"))
    }
    
    // MARK: - Rest Day Tests
    
    func testSuggestsRestDayWhenReadinessPoor() {
        let programDay = makeProgramDay(name: "Any Day", isHeavy: false)
        let readiness = makeReadiness(score: 35) // Below 40 threshold
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .restDay)
        XCTAssertTrue(result.rationale.contains("rest") || result.rationale.contains("Rest"))
    }
    
    func testRestDayOverridesHeavyDaySchedule() {
        let programDay = makeProgramDay(name: "Heavy Squat Day", isHeavy: true)
        let readiness = makeReadiness(score: 30)
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .restDay, "Rest day should override heavy day when readiness is poor")
    }
    
    func testRestDayAtThreshold() {
        let programDay = makeProgramDay(name: "Training Day", isHeavy: false)
        let readiness = makeReadiness(score: 39) // Just below 40 threshold
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness
        )
        
        XCTAssertEqual(result.action, .restDay)
    }
    
    // MARK: - Muscle-Specific Adjustments Tests
    
    func testDetectsFatiguedMuscles() {
        let programDay = makeProgramDay(
            name: "Upper Body",
            isHeavy: false,
            exercises: [
                ("Bench Press", ["Chest"]),
                ("Row", ["Back"]),
                ("Curl", ["Biceps"])
            ]
        )
        
        let readiness = makeReadiness(score: 70)
        let muscleRecovery: [String: MuscleRecoveryStatus] = [
            "Chest": MuscleRecoveryStatus(
                muscleName: "Chest",
                status: .fatigued,
                hoursSinceLastTrained: 12,
                lastTrainedDate: Date().addingTimeInterval(-12 * 3600),
                recentVolume: 5000,
                recoveryPercentage: 25
            ),
            "Back": MuscleRecoveryStatus(
                muscleName: "Back",
                status: .fresh,
                hoursSinceLastTrained: 72,
                lastTrainedDate: Date().addingTimeInterval(-72 * 3600),
                recentVolume: 0,
                recoveryPercentage: 100
            ),
        ]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        XCTAssertEqual(result.action, .modifyExercises)
        XCTAssertFalse(result.exerciseReplacements.isEmpty)
        XCTAssertTrue(result.exerciseReplacements.contains { $0.original == "Bench Press" })
    }
    
    func testNoMuscleAdjustmentWhenAllMusclesFresh() {
        let programDay = makeProgramDay(
            name: "Full Body",
            isHeavy: false,
            exercises: [
                ("Squat", ["Quadriceps"]),
                ("Bench Press", ["Chest"])
            ]
        )
        
        let readiness = makeReadiness(score: 75)
        let muscleRecovery: [String: MuscleRecoveryStatus] = [
            "Quadriceps": MuscleRecoveryStatus(
                muscleName: "Quadriceps",
                status: .fresh,
                hoursSinceLastTrained: 96,
                lastTrainedDate: nil,
                recentVolume: 0,
                recoveryPercentage: 100
            ),
            "Chest": MuscleRecoveryStatus(
                muscleName: "Chest",
                status: .fresh,
                hoursSinceLastTrained: 72,
                lastTrainedDate: nil,
                recentVolume: 0,
                recoveryPercentage: 100
            ),
        ]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        XCTAssertEqual(result.action, .proceedAsPlanned)
        XCTAssertTrue(result.exerciseReplacements.isEmpty)
    }
    
    func testMuscleAdjustmentIncludesIntensityReduction() {
        let programDay = makeProgramDay(
            name: "Chest Day",
            isHeavy: false,
            exercises: [("Bench Press", ["Chest"])]
        )
        
        let readiness = makeReadiness(score: 70)
        let muscleRecovery: [String: MuscleRecoveryStatus] = [
            "Chest": MuscleRecoveryStatus(
                muscleName: "Chest",
                status: .fatigued,
                hoursSinceLastTrained: 18,
                lastTrainedDate: Date(),
                recentVolume: 4000,
                recoveryPercentage: 30
            ),
        ]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        XCTAssertEqual(result.action, .modifyExercises)
        XCTAssertNotNil(result.intensityAdjustment)
        XCTAssertEqual(result.intensityAdjustment, -40, "Fatigued muscle should reduce intensity by 40%")
    }
    
    func testMultipleFatiguedMusclesListedInRationale() {
        let programDay = makeProgramDay(
            name: "Upper Body",
            isHeavy: false,
            exercises: [
                ("Bench Press", ["Chest"]),
                ("Row", ["Back"])
            ]
        )
        
        let readiness = makeReadiness(score: 70)
        let muscleRecovery: [String: MuscleRecoveryStatus] = [
            "Chest": MuscleRecoveryStatus(
                muscleName: "Chest",
                status: .fatigued,
                hoursSinceLastTrained: 15,
                lastTrainedDate: Date(),
                recentVolume: 4000,
                recoveryPercentage: 30
            ),
            "Back": MuscleRecoveryStatus(
                muscleName: "Back",
                status: .fatigued,
                hoursSinceLastTrained: 18,
                lastTrainedDate: Date(),
                recentVolume: 5000,
                recoveryPercentage: 35
            ),
        ]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        XCTAssertTrue(result.rationale.contains("Chest") || result.rationale.contains("chest"))
        XCTAssertTrue(result.rationale.contains("Back") || result.rationale.contains("back"))
    }
    
    // MARK: - Priority Tests (Rest Day > Muscle Fatigue > Lighter Variant)
    
    func testRestDayTakesPriorityOverMuscleAdjustments() {
        let programDay = makeProgramDay(
            name: "Squat Day",
            isHeavy: true,
            exercises: [("Squat", ["Quadriceps"])]
        )
        
        let readiness = makeReadiness(score: 35) // Poor readiness
        let muscleRecovery: [String: MuscleRecoveryStatus] = [
            "Quadriceps": MuscleRecoveryStatus(
                muscleName: "Quadriceps",
                status: .fatigued,
                hoursSinceLastTrained: 12,
                lastTrainedDate: Date(),
                recentVolume: 6000,
                recoveryPercentage: 20
            ),
        ]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        // Rest day should take priority over muscle-specific adjustments
        XCTAssertEqual(result.action, .restDay)
    }
    
    func testLighterVariantCheckedBeforeMuscleSpecific() {
        // When readiness is moderate AND heavy day, should suggest lighter variant
        // Even if there's some muscle fatigue
        let programDay = makeProgramDay(
            name: "Heavy Squat",
            isHeavy: true,
            exercises: [("Squat", ["Quadriceps"])]
        )
        
        let readiness = makeReadiness(score: 55) // Moderate readiness
        let muscleRecovery: [String: MuscleRecoveryStatus] = [
            "Quadriceps": MuscleRecoveryStatus(
                muscleName: "Quadriceps",
                status: .recovering, // Not fatigued
                hoursSinceLastTrained: 30,
                lastTrainedDate: Date(),
                recentVolume: 3000,
                recoveryPercentage: 60
            ),
        ]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        // Should suggest lighter variant, not muscle modification
        XCTAssertEqual(result.action, .lighterVariant)
    }
    
    // MARK: - Edge Cases
    
    func testHandlesEmptyExerciseList() {
        let programDay = makeProgramDay(name: "Empty Day", isHeavy: false, exercises: [])
        let readiness = makeReadiness(score: 70)
        
        XCTAssertNoThrow {
            _ = integration.adjustWorkout(programDay: programDay, readiness: readiness)
        }
    }
    
    func testHandlesNilMuscleRecovery() {
        let programDay = makeProgramDay(name: "Test Day", isHeavy: false)
        let readiness = makeReadiness(score: 70)
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: nil
        )
        
        XCTAssertEqual(result.action, .proceedAsPlanned)
    }
    
    func testHandlesEmptyMuscleRecoveryMap() {
        let programDay = makeProgramDay(name: "Test Day", isHeavy: false)
        let readiness = makeReadiness(score: 70)
        let muscleRecovery: [String: MuscleRecoveryStatus] = [:]
        
        let result = integration.adjustWorkout(
            programDay: programDay,
            readiness: readiness,
            muscleRecovery: muscleRecovery
        )
        
        XCTAssertEqual(result.action, .proceedAsPlanned)
    }
    
    // MARK: - Helper Methods
    
    private func makeProgramDay(
        name: String,
        isHeavy: Bool,
        exercises: [(name: String, muscles: [String])] = []
    ) -> ProgramDayInfo {
        let exerciseInfos = exercises.map { exercise in
            ProgramExerciseInfo(name: exercise.name, primaryMuscles: exercise.muscles)
        }
        
        return ProgramDayInfo(
            name: name,
            isHeavy: isHeavy,
            exercises: exerciseInfos
        )
    }
    
    private func makeReadiness(score: Double) -> ReadinessScore {
        ReadinessScore(
            date: Date(),
            overallScore: score,
            sleepScore: score,
            hrvScore: score,
            restingHRScore: score,
            trainingLoadScore: score,
            subjectiveScore: nil,
            recommendation: "Test",
            label: ReadinessLabel.from(score: score)
        )
    }
}
