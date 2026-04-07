import Foundation
import Observation
import SwiftData
import GymBroCore
import os

@MainActor
@Observable
public final class CreateExerciseViewModel {
    private static let logger = Logger(subsystem: "com.gymbro", category: "CreateExercise")

    // MARK: - Form Fields

    var name: String = ""
    var category: ExerciseCategory = .compound
    var equipment: Equipment = .barbell
    var selectedPrimaryMuscles: Set<String> = []
    var selectedSecondaryMuscles: Set<String> = []
    var instructions: String = ""

    // MARK: - State

    var validationError: String?
    var isSaving: Bool = false
    var didSave: Bool = false

    private var modelContext: ModelContext?

    // MARK: - Constants

    static let allMuscleGroups: [String] = [
        "Chest", "Upper Back", "Lats", "Front Delts", "Side Delts", "Rear Delts",
        "Biceps", "Triceps", "Forearms", "Core", "Quadriceps", "Hamstrings",
        "Glutes", "Calves", "Traps", "Lower Back", "Hip Flexors", "Adductors",
        "Abductors"
    ]

    public init() {}

    func setup(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Validation

    var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var isNameEmpty: Bool {
        trimmedName.isEmpty
    }

    var hasPrimaryMuscle: Bool {
        !selectedPrimaryMuscles.isEmpty
    }

    var canSave: Bool {
        !isNameEmpty && hasPrimaryMuscle && !isSaving
    }

    /// Validates form and returns an error message if invalid, nil if valid.
    func validate() -> String? {
        if isNameEmpty {
            return "Exercise name is required."
        }

        if !hasPrimaryMuscle {
            return "Select at least one primary muscle group."
        }

        // Check duplicate name
        if isDuplicateName() {
            return "An exercise named \"\(trimmedName)\" already exists."
        }

        return nil
    }

    func isDuplicateName() -> Bool {
        guard let modelContext = modelContext else { return false }
        let searchName = trimmedName.lowercased()
        let descriptor = FetchDescriptor<Exercise>()

        guard let exercises = try? modelContext.fetch(descriptor) else { return false }
        return exercises.contains { $0.name.lowercased() == searchName }
    }

    // MARK: - Muscle Group Helpers

    /// Muscles available for secondary selection (excludes those already selected as primary).
    var availableSecondaryMuscles: [String] {
        Self.allMuscleGroups.filter { !selectedPrimaryMuscles.contains($0) }
    }

    func togglePrimaryMuscle(_ muscle: String) {
        if selectedPrimaryMuscles.contains(muscle) {
            selectedPrimaryMuscles.remove(muscle)
        } else {
            selectedPrimaryMuscles.insert(muscle)
            // Remove from secondary if it was there
            selectedSecondaryMuscles.remove(muscle)
        }
    }

    func toggleSecondaryMuscle(_ muscle: String) {
        if selectedSecondaryMuscles.contains(muscle) {
            selectedSecondaryMuscles.remove(muscle)
        } else {
            selectedSecondaryMuscles.insert(muscle)
        }
    }

    // MARK: - Save

    func saveExercise() {
        guard let modelContext = modelContext else { return }

        if let error = validate() {
            validationError = error
            return
        }

        isSaving = true
        validationError = nil

        let muscleGroups = selectedPrimaryMuscles.map {
            MuscleGroup(name: $0, isPrimary: true)
        } + selectedSecondaryMuscles.map {
            MuscleGroup(name: $0, isPrimary: false)
        }

        let exercise = Exercise(
            name: trimmedName,
            category: category,
            equipment: equipment,
            instructions: instructions.trimmingCharacters(in: .whitespacesAndNewlines),
            muscleGroups: muscleGroups,
            isCustom: true,
            source: .custom
        )

        modelContext.insert(exercise)

        do {
            try modelContext.save()
            Self.logger.info("Custom exercise saved: \(self.trimmedName)")
            didSave = true
        } catch {
            Self.logger.error("Failed to save custom exercise: \(error.localizedDescription)")
            validationError = "Failed to save exercise. Please try again."
        }

        isSaving = false
    }
}
