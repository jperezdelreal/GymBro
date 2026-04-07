import SwiftUI
import Charts
import GymBroCore

// MARK: - ProgressDashboardView Previews

#Preview("Progress Dashboard") {
    ProgressDashboardView()
        .preferredColorScheme(.dark)
}

#Preview("Progress — Empty") {
    NavigationStack {
        ScrollView {
            ContentUnavailableView {
                Label("No Progress Data", systemImage: "chart.bar")
            } description: {
                Text("Complete a few workouts to see your progress trends here.")
            }
        }
        .navigationTitle("Progress")
    }
    .gymBroDarkBackground()
}
