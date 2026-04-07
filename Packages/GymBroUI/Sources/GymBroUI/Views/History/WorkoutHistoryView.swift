import SwiftUI
import SwiftData
import GymBroCore

public struct WorkoutHistoryView: View {
    @State private var viewModel = WorkoutHistoryViewModel()
    @Environment(\.modelContext) private var modelContext
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Loading workouts…")
                } else if let error = viewModel.errorMessage {
                    ContentUnavailableView {
                        Label("Something Went Wrong", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(error)
                    } actions: {
                        Button("Retry") { viewModel.retry() }
                    }
                } else if viewModel.workouts.isEmpty {
                    ContentUnavailableView {
                        Label("No Workouts Yet", systemImage: "figure.strengthtraining.traditional")
                    } description: {
                        Text("Complete your first workout and it will appear here.")
                    }
                } else {
                    List(viewModel.workouts) { workout in
                        VStack(alignment: .leading) {
                            Text(workout.date, style: .date)
                                .font(.headline)
                            Text("\(workout.totalSets) sets • \(Int(workout.totalVolume)) kg")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Workout History")
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }
}
