import SwiftUI
import WidgetKit
import GymBroCore

/// WidgetKit complication definitions for GymBro on Apple Watch.
/// Provides: active workout status and rest timer countdown.
///
/// NOTE: Requires WidgetKit extension target in Xcode project.
/// This file defines the timeline provider and entry types
/// for Watch complications.

// MARK: - Timeline Entry

struct WorkoutComplicationEntry: TimelineEntry {
    let date: Date
    let isWorkoutActive: Bool
    let exerciseName: String?
    let setNumber: Int?
    let restTimerRemaining: Int?
    let totalSetsCompleted: Int?

    static var placeholder: WorkoutComplicationEntry {
        WorkoutComplicationEntry(
            date: Date(),
            isWorkoutActive: true,
            exerciseName: "Bench Press",
            setNumber: 3,
            restTimerRemaining: nil,
            totalSetsCompleted: 8
        )
    }

    static var empty: WorkoutComplicationEntry {
        WorkoutComplicationEntry(
            date: Date(),
            isWorkoutActive: false,
            exerciseName: nil,
            setNumber: nil,
            restTimerRemaining: nil,
            totalSetsCompleted: nil
        )
    }
}

// MARK: - Timeline Provider

struct WorkoutComplicationProvider: TimelineProvider {
    func placeholder(in context: Context) -> WorkoutComplicationEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (WorkoutComplicationEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WorkoutComplicationEntry>) -> Void) {
        let entry = currentEntry()
        // Refresh every 30 seconds during active workout, otherwise every 15 minutes
        let refreshInterval: TimeInterval = entry.isWorkoutActive ? 30 : 900
        let nextUpdate = Date().addingTimeInterval(refreshInterval)
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
        completion(timeline)
    }

    private func currentEntry() -> WorkoutComplicationEntry {
        let state = WatchConnectivityService.shared.currentWorkoutState
        let timerState = WatchConnectivityService.shared.currentRestTimerState

        guard let state, state.isActive else {
            return .empty
        }

        return WorkoutComplicationEntry(
            date: Date(),
            isWorkoutActive: true,
            exerciseName: state.exerciseName,
            setNumber: state.setNumber,
            restTimerRemaining: timerState?.isActive == true ? timerState?.remainingSeconds : nil,
            totalSetsCompleted: state.totalSetsCompleted
        )
    }
}

// MARK: - Complication Views

/// Circular complication: shows workout status icon or rest timer countdown.
struct WorkoutCircularComplication: View {
    let entry: WorkoutComplicationEntry

    var body: some View {
        if entry.isWorkoutActive {
            if let remaining = entry.restTimerRemaining {
                // Rest timer mode
                ZStack {
                    AccessoryWidgetBackground()
                    VStack(spacing: 0) {
                        Image(systemName: "timer")
                            .font(.system(size: 10))
                        Text("\(remaining)s")
                            .font(.system(size: 14, weight: .bold, design: .rounded))
                            .monospacedDigit()
                    }
                }
            } else {
                // Active workout mode
                ZStack {
                    AccessoryWidgetBackground()
                    VStack(spacing: 0) {
                        Image(systemName: "dumbbell.fill")
                            .font(.system(size: 12))
                        if let sets = entry.totalSetsCompleted {
                            Text("\(sets)")
                                .font(.system(size: 14, weight: .bold, design: .rounded))
                        }
                    }
                }
            }
        } else {
            // No active workout
            ZStack {
                AccessoryWidgetBackground()
                Image(systemName: "dumbbell")
                    .font(.system(size: 16))
                    .foregroundStyle(.secondary)
            }
        }
    }
}

/// Rectangular complication: shows exercise name, set number, and status.
struct WorkoutRectangularComplication: View {
    let entry: WorkoutComplicationEntry

    var body: some View {
        if entry.isWorkoutActive {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Image(systemName: "dumbbell.fill")
                        .font(.system(size: 10))
                        .foregroundStyle(.green)
                    Text("GymBro")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(.secondary)
                }

                if let name = entry.exerciseName {
                    Text(name)
                        .font(.system(size: 12, weight: .bold, design: .rounded))
                        .lineLimit(1)
                }

                HStack(spacing: 8) {
                    if let setNum = entry.setNumber {
                        Text("Set \(setNum)")
                            .font(.system(size: 10))
                    }
                    if let remaining = entry.restTimerRemaining {
                        Text("Rest: \(remaining)s")
                            .font(.system(size: 10))
                            .foregroundStyle(.orange)
                    }
                }
            }
        } else {
            HStack(spacing: 6) {
                Image(systemName: "dumbbell")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                Text("No Active Workout")
                    .font(.system(size: 11))
                    .foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: - Widget Bundle

struct GymBroWatchWidgets: WidgetBundle {
    var body: some Widget {
        WorkoutStatusWidget()
    }
}

struct WorkoutStatusWidget: Widget {
    let kind = "com.gymbro.workout-status"

    var body: some WidgetConfiguration {
        StaticConfiguration(
            kind: kind,
            provider: WorkoutComplicationProvider()
        ) { entry in
            WorkoutCircularComplication(entry: entry)
        }
        .configurationDisplayName("Workout Status")
        .description("Shows your active workout status and rest timer.")
        .supportedFamilies([
            .accessoryCircular,
            .accessoryRectangular,
            .accessoryInline
        ])
    }
}
