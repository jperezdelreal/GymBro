import SwiftUI
import WatchConnectivity
import GymBroCore

@main
struct GymBroWatchApp: App {
    @State private var connectivityService = WatchConnectivityService.shared

    var body: some Scene {
        WindowGroup {
            WatchContentView()
                .task {
                    connectivityService.activate()
                }
        }
    }
}

/// Root view: shows active workout UI when a workout is in progress,
/// otherwise shows a waiting state.
struct WatchContentView: View {
    @State private var connectivityService = WatchConnectivityService.shared

    var body: some View {
        Group {
            if let workoutState = connectivityService.currentWorkoutState,
               workoutState.isActive {
                WatchWorkoutContainerView(initialState: workoutState)
            } else if connectivityService.workoutDidEnd {
                WatchWorkoutEndedView()
            } else {
                WatchIdleView()
            }
        }
    }
}

// MARK: - Idle State

struct WatchIdleView: View {
    @State private var connectivityService = WatchConnectivityService.shared

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "dumbbell.fill")
                .font(.system(size: 36))
                .foregroundStyle(.blue)
                .accessibilityHidden(true)

            Text("GymBro")
                .font(.headline)

            Text("Start a workout\non your iPhone")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            if connectivityService.isReachable {
                Label("iPhone Connected", systemImage: "iphone")
                    .font(.caption2)
                    .foregroundStyle(.green)
            } else {
                Label("iPhone Not Reachable", systemImage: "iphone.slash")
                    .font(.caption2)
                    .foregroundStyle(.orange)
            }
        }
    }
}

// MARK: - Workout Ended

struct WatchWorkoutEndedView: View {
    @State private var connectivityService = WatchConnectivityService.shared

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 40))
                .foregroundStyle(.green)
                .accessibilityHidden(true)

            Text("Workout Complete")
                .font(.headline)

            Text("Great session! 💪")
                .font(.caption)
                .foregroundStyle(.secondary)

            Button("Dismiss") {
                connectivityService.clearWorkoutEnded()
            }
            .font(.caption)
        }
    }
}

// MARK: - Workout Container (TabView for set logging / rest timer)

struct WatchWorkoutContainerView: View {
    let initialState: WatchWorkoutState

    @State private var viewModel: WatchWorkoutViewModel
    @State private var selectedTab: WatchTab = .activeSet

    enum WatchTab: Hashable {
        case activeSet
        case restTimer
    }

    init(initialState: WatchWorkoutState) {
        self.initialState = initialState
        self._viewModel = State(initialValue: WatchWorkoutViewModel(state: initialState))
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            ActiveSetView(viewModel: viewModel)
                .tag(WatchTab.activeSet)

            RestTimerWatchView(viewModel: viewModel)
                .tag(WatchTab.restTimer)
        }
        .tabViewStyle(.verticalPage)
        .onChange(of: viewModel.isRestTimerActive) { _, isActive in
            if isActive {
                selectedTab = .restTimer
            }
        }
    }
}
