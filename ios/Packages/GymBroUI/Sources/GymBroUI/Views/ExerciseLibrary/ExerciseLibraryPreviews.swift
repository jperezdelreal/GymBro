import SwiftUI
import SwiftData
import GymBroCore

// MARK: - ExerciseLibraryView Previews

#Preview("Exercise Library — Loaded") {
    ExerciseLibraryView()
        .preferredColorScheme(.dark)
}

#Preview("Exercise Library — Empty") {
    NavigationStack {
        ContentUnavailableView {
            Label("No Exercises", systemImage: "dumbbell")
        } description: {
            Text("Add your first exercise to get started.")
        }
        .navigationTitle("Exercise Library")
    }
    .preferredColorScheme(.dark)
}
