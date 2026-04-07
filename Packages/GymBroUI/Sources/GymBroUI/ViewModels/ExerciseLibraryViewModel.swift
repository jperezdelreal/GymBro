import Foundation
import Observation
import SwiftData
import GymBroCore

@MainActor
@Observable
public final class ExerciseLibraryViewModel {
    var exercises: [Exercise] = []
    private var modelContext: ModelContext?
    
    public init() {}
    
    func setup(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadExercises()
    }
    
    private func loadExercises() {
        guard let modelContext = modelContext else { return }
        
        let descriptor = FetchDescriptor<Exercise>(
            sortBy: [SortDescriptor(\.name, order: .forward)]
        )
        
        do {
            exercises = try modelContext.fetch(descriptor)
        } catch {
            print("Failed to fetch exercises: \(error)")
            exercises = []
        }
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
