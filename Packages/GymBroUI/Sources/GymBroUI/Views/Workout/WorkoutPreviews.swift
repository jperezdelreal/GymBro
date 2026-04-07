import SwiftUI
import GymBroCore

// MARK: - Workout View Previews

// StartWorkoutView, WorkoutSummaryView, and RestTimerView have #Preview in their source files.
// These additional previews cover variant states for comprehensive design iteration.

#Preview("Start Workout — Dark") {
    StartWorkoutView()
        .preferredColorScheme(.dark)
}

#Preview("Summary — With PRs") {
    WorkoutSummaryView(summary: WorkoutSummary(
        duration: 4200,
        totalVolume: 12500,
        totalSets: 24,
        personalRecords: 3
    ))
}

#Preview("Summary — Quick Session") {
    WorkoutSummaryView(summary: WorkoutSummary(
        duration: 1800,
        totalVolume: 2800,
        totalSets: 9,
        personalRecords: 0
    ))
}
