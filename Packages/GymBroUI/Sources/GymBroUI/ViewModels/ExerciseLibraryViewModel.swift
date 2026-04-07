import Foundation
import Observation
import SwiftData
import GymBroCore
import os

@MainActor
@Observable
public final class ExerciseLibraryViewModel {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ExerciseLibrary")

    var exercises: [Exercise] = []
    var isLoading: Bool = false
    var errorMessage: String?
    private var modelContext: ModelContext?
    
    public init() {}
    
    func setup(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadExercises()
    }
    
    private func loadExercises() {
        guard let modelContext = modelContext else { return }
        
        isLoading = true
        errorMessage = nil
        
        let descriptor = FetchDescriptor<Exercise>(
            sortBy: [SortDescriptor(\.name, order: .forward)]
        )
        
        do {
            exercises = try modelContext.fetch(descriptor)
        } catch {
            errorMessage = "Could not load exercises. Pull down to retry."
            exercises = []
        }
        
        isLoading = false
    }
    
    func retry() {
        loadExercises()
    }
    
    func filteredExercises(
        searchText: String,
        category: ExerciseCategory?,
        equipment: Equipment?
    ) -> [Exercise] {
        var filtered = exercises
        
        if let category = category {
            filtered = filtered.filter { $0.category == category }
        }
        
        if let equipment = equipment {
            filtered = filtered.filter { $0.equipment == equipment }
        }
        
        if !searchText.isEmpty {
            let lowercasedSearch = searchText.lowercased()
            filtered = filtered.filter { exercise in
                exercise.name.lowercased().contains(lowercasedSearch) ||
                exercise.muscleGroups.contains { $0.name.lowercased().contains(lowercasedSearch) }
            }
        }
        
        return filtered
    }
}
