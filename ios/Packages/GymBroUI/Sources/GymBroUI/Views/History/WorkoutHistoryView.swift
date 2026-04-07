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
                        NavigationLink {
                            WorkoutDetailView(workout: workout)
                        } label: {
                            workoutRow(workout)
                        }
                        .listRowBackground(GymBroColors.surfaceSecondary)
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .gymBroDarkBackground()
            .navigationTitle("Workout History")
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }

    // MARK: - Row

    private func workoutRow(_ workout: Workout) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
            HStack {
                Text(workout.date, style: .date)
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.textPrimary)
                Spacer()
                if let duration = workout.duration {
                    Text(formatDuration(duration))
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }

            HStack(spacing: GymBroSpacing.md) {
                Label("\(workout.totalSets) sets", systemImage: "chart.bar.fill")
                Label(String(format: "%.0f kg", workout.totalVolume), systemImage: "scalemass.fill")
                Label("\(workout.exercises.count) exercises", systemImage: "dumbbell.fill")
            }
            .font(GymBroTypography.caption)
            .foregroundStyle(GymBroColors.textSecondary)
        }
        .padding(.vertical, GymBroSpacing.xs)
    }

    // MARK: - Helpers

    private func formatDuration(_ duration: TimeInterval) -> String {
        let hours = Int(duration) / 3600
        let minutes = Int(duration) / 60 % 60
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        }
        return "\(minutes)m"
    }
}
