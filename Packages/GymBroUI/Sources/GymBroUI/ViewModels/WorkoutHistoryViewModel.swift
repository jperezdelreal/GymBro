import Foundation
import Observation
import SwiftData
import GymBroCore
import os

@MainActor
@Observable
public final class WorkoutHistoryViewModel {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WorkoutHistory")

    var workouts: [Workout] = []
    var isLoading: Bool = false
    var errorMessage: String?
    private var modelContext: ModelContext?
    
    public init() {}
    
    func setup(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadWorkouts()
    }
    
    private func loadWorkouts() {
        guard let modelContext = modelContext else { return }
        
        isLoading = true
        errorMessage = nil
        
        let descriptor = FetchDescriptor<Workout>(
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        
        do {
            workouts = try modelContext.fetch(descriptor)
        } catch {
            errorMessage = "Could not load workout history. Pull down to retry."
            workouts = []
        }
        
        isLoading = false
    }
    
    func retry() {
        loadWorkouts()
    }
}
