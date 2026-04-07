import SwiftUI
import GymBroUI

struct ContentView: View {
    var body: some View {
        TabView {
            Tab("Workout", systemImage: "figure.strengthtraining.traditional") {
                WorkoutTab()
            }

            Tab("History", systemImage: "chart.line.uptrend.xyaxis") {
                HistoryTab()
            }

            Tab("Programs", systemImage: "calendar") {
                ProgramsTab()
            }

            Tab("Coach", systemImage: "brain.head.profile") {
                CoachTab()
            }

            Tab("Profile", systemImage: "person.circle") {
                ProfileTab()
            }
        }
    }
}

struct WorkoutTab: View {
    var body: some View {
        NavigationStack {
            Text("Workout")
                .navigationTitle("Workout")
        }
    }
}

struct HistoryTab: View {
    var body: some View {
        NavigationStack {
            Text("History")
                .navigationTitle("History")
        }
    }
}

struct ProgramsTab: View {
    var body: some View {
        NavigationStack {
            Text("Programs")
                .navigationTitle("Programs")
        }
    }
}

struct CoachTab: View {
    var body: some View {
        NavigationStack {
            CoachChatView()
                .navigationTitle("Coach")
        }
    }
}

struct ProfileTab: View {
    var body: some View {
        NavigationStack {
            Text("Profile")
                .navigationTitle("Profile")
        }
    }
}

#Preview {
    ContentView()
}
