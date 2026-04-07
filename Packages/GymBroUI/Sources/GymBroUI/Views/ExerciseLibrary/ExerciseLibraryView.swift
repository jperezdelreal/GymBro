import SwiftUI
import SwiftData
import GymBroCore

public struct ExerciseLibraryView: View {
    @State private var viewModel = ExerciseLibraryViewModel()
    @Environment(\.modelContext) private var modelContext
    @State private var searchText = ""
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Loading exercises…")
                } else if let error = viewModel.errorMessage {
                    ContentUnavailableView {
                        Label("Something Went Wrong", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(error)
                    } actions: {
                        Button("Retry") { viewModel.retry() }
                    }
                } else if viewModel.exercises.isEmpty {
                    ContentUnavailableView {
                        Label("No Exercises", systemImage: "dumbbell")
                    } description: {
                        Text("Add your first exercise to get started.")
                    }
                } else {
                    List(viewModel.exercises) { exercise in
                        Text(exercise.name)
                    }
                }
            }
            .searchable(text: $searchText)
            .navigationTitle("Exercise Library")
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }
}
