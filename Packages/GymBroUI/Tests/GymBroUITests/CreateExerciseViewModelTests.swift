import Testing
import SwiftData
import Foundation
@testable import GymBroUI
@testable import GymBroCore

@MainActor
@Suite("CreateExerciseViewModel Tests")
struct CreateExerciseViewModelTests {

    // MARK: - Helpers

    private func makeContext() throws -> ModelContext {
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        let container = try ModelContainer(
            for: Exercise.self, MuscleGroup.self,
            configurations: config
        )
        return container.mainContext
    }

    private func makeViewModel(context: ModelContext) -> CreateExerciseViewModel {
        let vm = CreateExerciseViewModel()
        vm.setup(modelContext: context)
        return vm
    }

    // MARK: - Name Validation

    @Test("Empty name fails validation")
    func emptyNameFails() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = ""
        vm.selectedPrimaryMuscles = ["Chest"]

        let error = vm.validate()
        #expect(error == "Exercise name is required.")
        #expect(vm.canSave == false)
    }

    @Test("Whitespace-only name fails validation")
    func whitespaceNameFails() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = "   \n  "
        vm.selectedPrimaryMuscles = ["Chest"]

        let error = vm.validate()
        #expect(error == "Exercise name is required.")
    }

    @Test("Valid name passes")
    func validNamePasses() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = "Landmine Press"
        vm.selectedPrimaryMuscles = ["Chest"]

        let error = vm.validate()
        #expect(error == nil)
    }

    // MARK: - Duplicate Detection

    @Test("Duplicate name detected case-insensitively")
    func duplicateNameDetected() throws {
        let ctx = try makeContext()
        let existing = Exercise(
            name: "Barbell Squat",
            category: .compound,
            equipment: .barbell,
            isCustom: false
        )
        ctx.insert(existing)
        try ctx.save()

        let vm = makeViewModel(context: ctx)
        vm.name = "barbell squat"
        vm.selectedPrimaryMuscles = ["Quadriceps"]

        let error = vm.validate()
        #expect(error?.contains("already exists") == true)
    }

    @Test("Unique name passes duplicate check")
    func uniqueNamePasses() throws {
        let ctx = try makeContext()
        let existing = Exercise(
            name: "Barbell Squat",
            category: .compound,
            equipment: .barbell,
            isCustom: false
        )
        ctx.insert(existing)
        try ctx.save()

        let vm = makeViewModel(context: ctx)
        vm.name = "Zercher Squat"
        vm.selectedPrimaryMuscles = ["Quadriceps"]

        #expect(vm.isDuplicateName() == false)
    }

    // MARK: - Muscle Group Validation

    @Test("No primary muscles fails validation")
    func noPrimaryMusclesFails() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = "Some Exercise"

        let error = vm.validate()
        #expect(error == "Select at least one primary muscle group.")
    }

    @Test("Primary muscle selected passes")
    func primaryMusclePasses() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = "Some Exercise"
        vm.selectedPrimaryMuscles = ["Chest"]

        let error = vm.validate()
        #expect(error == nil)
    }

    // MARK: - canSave Computed Property

    @Test("canSave requires name and primary muscle")
    func canSaveRequirements() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)

        // Both empty
        #expect(vm.canSave == false)

        // Name only
        vm.name = "Test"
        #expect(vm.canSave == false)

        // Muscle only
        vm.name = ""
        vm.selectedPrimaryMuscles = ["Chest"]
        #expect(vm.canSave == false)

        // Both set
        vm.name = "Test"
        #expect(vm.canSave == true)
    }

    // MARK: - Muscle Group Toggle Logic

    @Test("Toggle primary muscle adds and removes")
    func togglePrimaryMuscle() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)

        vm.togglePrimaryMuscle("Chest")
        #expect(vm.selectedPrimaryMuscles.contains("Chest"))

        vm.togglePrimaryMuscle("Chest")
        #expect(!vm.selectedPrimaryMuscles.contains("Chest"))
    }

    @Test("Selecting primary removes from secondary")
    func primaryRemovesSecondary() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)

        vm.toggleSecondaryMuscle("Chest")
        #expect(vm.selectedSecondaryMuscles.contains("Chest"))

        vm.togglePrimaryMuscle("Chest")
        #expect(vm.selectedPrimaryMuscles.contains("Chest"))
        #expect(!vm.selectedSecondaryMuscles.contains("Chest"))
    }

    @Test("Available secondary excludes primary selections")
    func availableSecondaryExcludesPrimary() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)

        vm.togglePrimaryMuscle("Chest")
        vm.togglePrimaryMuscle("Triceps")

        let available = vm.availableSecondaryMuscles
        #expect(!available.contains("Chest"))
        #expect(!available.contains("Triceps"))
        #expect(available.contains("Biceps"))
    }

    // MARK: - Save Flow

    @Test("Successful save sets didSave flag")
    func successfulSave() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = "Custom Press"
        vm.category = .compound
        vm.equipment = .barbell
        vm.selectedPrimaryMuscles = ["Chest", "Triceps"]
        vm.selectedSecondaryMuscles = ["Front Delts"]
        vm.instructions = "Press the bar"

        vm.saveExercise()

        #expect(vm.didSave == true)
        #expect(vm.validationError == nil)

        // Verify persisted
        let descriptor = FetchDescriptor<Exercise>()
        let exercises = try ctx.fetch(descriptor)
        #expect(exercises.count == 1)

        let saved = exercises.first!
        #expect(saved.name == "Custom Press")
        #expect(saved.isCustom == true)
        #expect(saved.source == .custom)
        #expect(saved.category == .compound)
        #expect(saved.equipment == .barbell)
        #expect(saved.muscleGroups.count == 3)
        #expect(saved.muscleGroups.filter(\.isPrimary).count == 2)
    }

    @Test("Save with invalid data does not persist")
    func invalidSaveDoesNotPersist() throws {
        let ctx = try makeContext()
        let vm = makeViewModel(context: ctx)
        vm.name = ""

        vm.saveExercise()

        #expect(vm.didSave == false)
        #expect(vm.validationError != nil)

        let descriptor = FetchDescriptor<Exercise>()
        let exercises = try ctx.fetch(descriptor)
        #expect(exercises.count == 0)
    }

    // MARK: - Muscle Group List

    @Test("All 19 muscle groups present")
    func allMuscleGroupsPresent() {
        #expect(CreateExerciseViewModel.allMuscleGroups.count == 19)
        #expect(CreateExerciseViewModel.allMuscleGroups.contains("Chest"))
        #expect(CreateExerciseViewModel.allMuscleGroups.contains("Quadriceps"))
        #expect(CreateExerciseViewModel.allMuscleGroups.contains("Abductors"))
    }

    // MARK: - ExerciseCategory CaseIterable

    @Test("ExerciseCategory includes cardio and is iterable")
    func exerciseCategoryCases() {
        let cases = ExerciseCategory.allCases
        #expect(cases.count == 4)
        #expect(cases.contains(.compound))
        #expect(cases.contains(.isolation))
        #expect(cases.contains(.accessory))
        #expect(cases.contains(.cardio))
    }
}
