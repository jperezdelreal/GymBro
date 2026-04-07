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
    @State private var needsOnboarding = false

    let authService: AuthenticationService
    let syncService: CloudKitSyncService
    let healthKitCoordinator: HealthKitCoordinator

    var body: some View {
        Group {
            if authService.authState == .unknown {
                ProgressView("Loading…")
                    .tint(GymBroColors.accentGreen)
            } else if needsOnboarding {
                OnboardingFlowView(
                    onRequestHealthKit: { [healthKitCoordinator] in
                        await healthKitCoordinator.requestAuthorization()
                    },
                    onComplete: {
                        needsOnboarding = false
                        Task {
                            await healthKitCoordinator.syncAndCalculateReadiness(
                                modelContext: modelContext
                            )
                            await healthKitCoordinator.enableBackgroundSync()
                        }
                    }
                )
            } else {
                mainTabView
            }
        }
        .preferredColorScheme(.dark)
        .task {
            checkOnboardingStatus()
        }
    }
    
    private func checkOnboardingStatus() {
        let descriptor = FetchDescriptor<UserProfile>()
        let profiles = (try? modelContext.fetch(descriptor)) ?? []
        
        if let profile = profiles.first {
            needsOnboarding = !profile.hasCompletedOnboarding
        } else {
            needsOnboarding = true
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
        .tint(GymBroColors.accentGreen)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: selectedTab)
        .task {
            checkForUnfinishedWorkout()
            await healthKitCoordinator.syncAndCalculateReadiness(
                modelContext: modelContext
            )
            await healthKitCoordinator.enableBackgroundSync()
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
    @State private var showStartWorkout = false
    
    var body: some View {
        NavigationStack {
            EmptyStateView(
                icon: "figure.strengthtraining.traditional",
                title: "Ready to train?",
                message: "Start your first workout and let's build something amazing",
                actionTitle: "Start Workout"
            ) {
                showStartWorkout = true
            }
            .navigationTitle("Workout")
            .sheet(isPresented: $showStartWorkout) {
                StartWorkoutView()
            }
        }
    }
}

struct HistoryTab: View {
    var body: some View {
        WorkoutHistoryView()
    }
}

struct ProgramsTab: View {
    var body: some View {
        ProgramsTabView()
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
        syncService: CloudKitSyncService(),
        healthKitCoordinator: HealthKitCoordinator(
            healthKitService: MockHealthKitService()
        )
    )
}
