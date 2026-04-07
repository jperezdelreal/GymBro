import SwiftUI
import SwiftData
import GymBroCore

public struct ExerciseLibraryView: View {
    @State private var viewModel = ExerciseLibraryViewModel()
    @Environment(\.modelContext) private var modelContext
    @State private var searchText = ""
    @State private var showCreateExercise = false
    
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
                    } actions: {
                        Button("Add Exercise") { showCreateExercise = true }
                            .buttonStyle(.gymBroPrimary)
                            .frame(maxWidth: 200)
                    }
                } else {
                    List(viewModel.exercises) { exercise in
                        NavigationLink(destination: ExerciseDetailView(exercise: exercise)) {
                            ExerciseLibraryRow(exercise: exercise)
                        }
                        .listRowBackground(GymBroColors.background)
                        .listRowSeparatorTint(GymBroColors.border)
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .searchable(text: $searchText)
            .navigationTitle("Exercise Library")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showCreateExercise = true
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(GymBroColors.accentGreen)
                    }
                    .accessibilityLabel("Add Exercise")
                }
            }
            .sheet(isPresented: $showCreateExercise, onDismiss: {
                viewModel.retry()
            }) {
                CreateExerciseView()
            }
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }
}
