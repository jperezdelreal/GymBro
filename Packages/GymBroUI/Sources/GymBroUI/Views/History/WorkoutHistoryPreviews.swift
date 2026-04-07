import SwiftUI
import SwiftData
import GymBroCore

// MARK: - WorkoutHistoryView Previews

#Preview("History — Loaded") {
    WorkoutHistoryView()
        .preferredColorScheme(.dark)
}

#Preview("History — Empty State") {
    NavigationStack {
        ContentUnavailableView {
            Label("No Workouts Yet", systemImage: "figure.strengthtraining.traditional")
        } description: {
            Text("Complete your first workout and it will appear here.")
        }
        .navigationTitle("Workout History")
    }
    .gymBroDarkBackground()
}
