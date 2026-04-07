import SwiftUI
import SwiftData
import GymBroCore
import GymBroUI

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var unfinishedWorkout: Workout?
    @State private var showRecoverySheet = false
    @State private var resumedWorkoutViewModel: ActiveWorkoutViewModel?
    @State private var showResumedWorkout = false

    var body: some View {
        TabView {
            Tab("Workout", systemImage: "figure.strengthtraining.traditional") {
                NavigationStack {
                    WorkoutTab()
                        .navigationDestination(isPresented: $showResumedWorkout) {
                            if let viewModel = resumedWorkoutViewModel {
                                ActiveWorkoutView(viewModel: viewModel)
                            }
                        }
                }
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
        .task {
            checkForUnfinishedWorkout()
        }
        .sheet(isPresented: $showRecoverySheet) {
            if let workout = unfinishedWorkout {
                WorkoutRecoveryView(
                    workout: workout,
                    onResume: { workout in
                        resumeWorkout(workout)
                        showRecoverySheet = false
                    },
                    onDiscard: {
                        unfinishedWorkout = nil
                        showRecoverySheet = false
                    }
                )
                .presentationDetents([.medium, .large])
                .interactiveDismissDisabled()
            }
        }
    }

    private func checkForUnfinishedWorkout() {
        let service = WorkoutRecoveryService(modelContext: modelContext)
        if let workout = service.findUnfinishedWorkout() {
            service.cleanupStaleWorkouts(excluding: workout.id)
            unfinishedWorkout = workout
            showRecoverySheet = true
        }
    }

    private func resumeWorkout(_ workout: Workout) {
        let exercises = workout.exercises
        let lastExercise = exercises.last

        resumedWorkoutViewModel = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: lastExercise.map { [$0] } ?? []
        )
        showResumedWorkout = true
        unfinishedWorkout = nil
    }
}

struct WorkoutTab: View {
    var body: some View {
        Text("Workout")
            .navigationTitle("Workout")
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
