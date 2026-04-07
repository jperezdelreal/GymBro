import SwiftUI
import SwiftData
import GymBroCore
import GymBroUI

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var unfinishedWorkout: Workout?
    @State private var showRecoverySheet = false
    @State private var resumedWorkoutViewModel: ActiveWorkoutViewModel?
    @State private var showResumedWorkout = false
    @State private var selectedTab = "workout"

    let authService: AuthenticationService
    let syncService: CloudKitSyncService

    var body: some View {
        Group {
            if authService.authState == .unknown {
                ProgressView("Loading…")
            } else {
                mainTabView
            }
        }
    }

    @ViewBuilder
    private var mainTabView: some View {
        TabView(selection: $selectedTab) {
            Tab("Workout", systemImage: "figure.strengthtraining.traditional", value: "workout") {
                NavigationStack {
                    WorkoutTab()
                        .navigationDestination(isPresented: $showResumedWorkout) {
                            if let viewModel = resumedWorkoutViewModel {
                                ActiveWorkoutView(viewModel: viewModel)
                            }
                        }
                }
            }

            Tab("History", systemImage: "chart.line.uptrend.xyaxis", value: "history") {
                HistoryTab()
            }

            Tab("Programs", systemImage: "calendar", value: "programs") {
                ProgramsTab()
            }

            Tab("Coach", systemImage: "brain.head.profile", value: "coach") {
                CoachTab()
            }

            Tab("Profile", systemImage: "person.circle", value: "profile") {
                NavigationStack {
                    ProfileView(authService: authService, syncService: syncService)
                }
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: selectedTab)
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

#Preview {
    ContentView(
        authService: AuthenticationService(),
        syncService: CloudKitSyncService()
    )
}
