import Foundation
import SwiftData
import GymBroCore

public class WorkoutHistoryViewModel: ObservableObject {
    @Published var workouts: [Workout] = []
    private var modelContext: ModelContext?
    
    public init() {}
    
    func setup(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadWorkouts()
    }
    
    private func loadWorkouts() {
        guard let modelContext = modelContext else { return }
        
        let descriptor = FetchDescriptor<Workout>(
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        
        do {
            workouts = try modelContext.fetch(descriptor)
        } catch {
            print("Failed to fetch workouts: \(error)")
            workouts = []
        }
    }
}
