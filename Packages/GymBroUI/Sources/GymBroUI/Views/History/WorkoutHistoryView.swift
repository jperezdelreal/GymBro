import SwiftUI
import SwiftData
import GymBroCore

public struct WorkoutHistoryView: View {
    @State private var viewModel = WorkoutHistoryViewModel()
    @Environment(\.modelContext) private var modelContext
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            List(viewModel.workouts) { workout in
                VStack(alignment: .leading) {
                    Text(workout.date, style: .date)
                        .font(.headline)
                    Text("\(workout.totalSets) sets • \(Int(workout.totalVolume)) kg")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Workout History")
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }
}
