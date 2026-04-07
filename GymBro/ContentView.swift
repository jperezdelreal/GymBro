import SwiftUI
import GymBroUI

struct ContentView: View {
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            WorkoutTab()
                .tabItem {
                    Label("Workout", systemImage: "figure.strengthtraining.traditional")
                }
                .tag(0)
            
            HistoryTab()
                .tabItem {
                    Label("History", systemImage: "chart.line.uptrend.xyaxis")
                }
                .tag(1)
            
            ProgramsTab()
                .tabItem {
                    Label("Programs", systemImage: "calendar")
                }
                .tag(2)
            
            CoachTab()
                .tabItem {
                    Label("Coach", systemImage: "brain.head.profile")
                }
                .tag(3)
            
            ProfileTab()
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }
                .tag(4)
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
