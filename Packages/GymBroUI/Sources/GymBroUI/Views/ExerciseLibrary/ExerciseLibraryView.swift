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
            List(viewModel.exercises) { exercise in
                Text(exercise.name)
            }
            .searchable(text: $searchText)
            .navigationTitle("Exercise Library")
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }
}
