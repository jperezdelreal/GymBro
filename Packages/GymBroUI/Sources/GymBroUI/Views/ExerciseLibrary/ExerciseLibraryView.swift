import SwiftUI
import SwiftData
import GymBroCore

public struct ExerciseLibraryView: View {
    @StateObject private var viewModel = ExerciseLibraryViewModel()
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
            .onAppear {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }
}
