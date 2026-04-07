import XCTest
import SwiftData
@testable import GymBroCore

final class SmartDefaultsServiceTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!
    private var sut: SmartDefaultsService!

    @MainActor
    override func setUp() {
        super.setUp()
        do {
            let schema = Schema([
                Exercise.self,
                ExerciseSet.self,
                Workout.self,
                Program.self,
                ProgramDay.self,
                PlannedExercise.self,
                UserProfile.self,
                BodyweightEntry.self,
                MuscleGroup.self,
                ChatMessage.self,
                PlateauAnalysis.self,
            ])
            let config = ModelConfiguration(isStoredInMemoryOnly: true)
            container = try ModelContainer(for: schema, configurations: [config])
            context = container.mainContext
            sut = SmartDefaultsService(modelContext: context)
        } catch {
            XCTFail("Failed to create in-memory ModelContainer: \(error)")
        }
    }

    override func tearDown() {
        sut = nil
        context = nil
        container = nil
        super.tearDown()
    }

    // MARK: - Default Values (no history)

    @MainActor
    func testDefaultValues_compoundBarbell() {
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 62.5, "Compound barbell default 60 + 2.5 progression")
        XCTAssertEqual(reps, 5)
    }

    @MainActor
    func testDefaultValues_compoundDumbbell() {
        let exercise = Exercise(name: "DB Bench", category: .compound, equipment: .dumbbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 22.5, "Compound dumbbell default 20 + 2.5 progression")
        XCTAssertEqual(reps, 8)
    }

    @MainActor
    func testDefaultValues_compoundOtherEquipment() {
        let exercise = Exercise(name: "Leg Press", category: .compound, equipment: .machine)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 42.5, "Compound other default 40 + 2.5 progression")
        XCTAssertEqual(reps, 8)
    }

    @MainActor
    func testDefaultValues_isolation() {
        let exercise = Exercise(name: "Bicep Curl", category: .isolation, equipment: .dumbbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 16.25, "Isolation default 15 + 1.25 progression")
        XCTAssertEqual(reps, 10)
    }

    @MainActor
    func testDefaultValues_accessory() {
        let exercise = Exercise(name: "Face Pull", category: .accessory, equipment: .cable)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 10.0, "Accessory default 10 + 0 progression")
        XCTAssertEqual(reps, 12)
    }

    // MARK: - With History

    @MainActor
    func testSmartDefaults_withHistory_appliesProgression() {
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date())
        context.insert(workout)

        // Create completed working sets
        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date()
            context.insert(set)
        }

        try? context.save()

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 102.5, "Should add 2.5kg compound progression to average weight of 100")
        XCTAssertEqual(reps, 5)
    }

    @MainActor
    func testSmartDefaults_withHistory_isolationProgression() {
        let exercise = Exercise(name: "Curl", category: .isolation, equipment: .dumbbell)
        context.insert(exercise)

        let workout = Workout(date: Date())
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 20.0,
                reps: 10,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date()
            context.insert(set)
        }

        try? context.save()

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 21.25, "Should add 1.25kg isolation progression")
        XCTAssertEqual(reps, 10)
    }

    @MainActor
    func testSmartDefaults_ignoresWarmupSets() {
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date())
        context.insert(workout)

        // Warmup set — should be ignored
        let warmup = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 50.0,
            reps: 10,
            setType: .warmup,
            setNumber: 1
        )
        warmup.completedAt = Date()
        context.insert(warmup)

        // Working set
        let working = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100.0,
            reps: 5,
            setType: .working,
            setNumber: 2
        )
        working.completedAt = Date()
        context.insert(working)

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 102.5, "Should only use working sets for default calculation")
    }

    @MainActor
    func testSmartDefaults_ignoresIncompleteSets() {
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date())
        context.insert(workout)

        // Incomplete set (no completedAt)
        let incomplete = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 200.0,
            reps: 1,
            setType: .working,
            setNumber: 1
        )
        context.insert(incomplete)

        try? context.save()

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        // Should fall back to defaults since no completed sets
        XCTAssertEqual(weight, 62.5, "Should return default when no completed sets")
        XCTAssertEqual(reps, 5)
    }
}
