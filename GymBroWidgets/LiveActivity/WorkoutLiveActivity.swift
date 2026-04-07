import SwiftUI
import WidgetKit
import ActivityKit

/// The Live Activity widget rendering for Lock Screen and Dynamic Island.
struct WorkoutLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: WorkoutActivityAttributes.self) { context in
            // Lock Screen / Banner presentation
            LockScreenLiveActivityView(context: context)
                .activityBackgroundTint(.black.opacity(0.7))
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded regions
                DynamicIslandExpandedRegion(.leading) {
                    expandedLeading(context: context)
                }

                DynamicIslandExpandedRegion(.trailing) {
                    expandedTrailing(context: context)
                }

                DynamicIslandExpandedRegion(.center) {
                    expandedCenter(context: context)
                }

                DynamicIslandExpandedRegion(.bottom) {
                    expandedBottom(context: context)
                }
            } compactLeading: {
                // Compact left: timer or set count
                compactLeading(context: context)
            } compactTrailing: {
                // Compact right: set count or timer
                compactTrailing(context: context)
            } minimal: {
                // Minimal: just timer
                minimalView(context: context)
            }
            .widgetURL(URL(string: "gymbro://active-workout"))
        }
    }

    // MARK: - Dynamic Island Compact

    @ViewBuilder
    private func compactLeading(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        if let endDate = context.state.restTimerEndDate {
            // Show countdown timer
            HStack(spacing: 4) {
                Image(systemName: "timer")
                    .font(.caption2)
                Text(timerInterval: context.state.restTimerStartDate...endDate, countsDown: true)
                    .font(.system(.caption, design: .monospaced, weight: .bold))
                    .monospacedDigit()
                    .frame(width: 40)
            }
        } else {
            Label {
                Text("\(context.state.completedSets)")
                    .font(.system(.caption, design: .rounded, weight: .bold))
            } icon: {
                Image(systemName: "checkmark.circle.fill")
                    .font(.caption2)
            }
        }
    }

    @ViewBuilder
    private func compactTrailing(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        if context.state.restTimerEndDate != nil {
            Text("\(context.state.completedSets) sets")
                .font(.system(.caption2, design: .rounded, weight: .medium))
        } else {
            Image(systemName: "figure.strengthtraining.traditional")
                .font(.caption2)
        }
    }

    // MARK: - Dynamic Island Minimal

    @ViewBuilder
    private func minimalView(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        if let endDate = context.state.restTimerEndDate {
            Text(timerInterval: context.state.restTimerStartDate...endDate, countsDown: true)
                .font(.system(.caption2, design: .monospaced, weight: .bold))
                .monospacedDigit()
        } else {
            Image(systemName: "figure.strengthtraining.traditional")
                .font(.caption)
        }
    }

    // MARK: - Dynamic Island Expanded

    @ViewBuilder
    private func expandedLeading(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Exercise")
                .font(.caption2)
                .foregroundStyle(.secondary)

            Text(context.state.exerciseName)
                .font(.subheadline)
                .fontWeight(.semibold)
                .lineLimit(1)
        }
    }

    @ViewBuilder
    private func expandedTrailing(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        VStack(alignment: .trailing, spacing: 2) {
            Text("Set")
                .font(.caption2)
                .foregroundStyle(.secondary)

            if context.state.totalPlannedSets > 0 {
                Text("\(context.state.currentSetNumber)/\(context.state.totalPlannedSets)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
            } else {
                Text("Set \(context.state.currentSetNumber)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
            }
        }
    }

    @ViewBuilder
    private func expandedCenter(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        if let endDate = context.state.restTimerEndDate {
            // Rest timer countdown
            VStack(spacing: 4) {
                Text("Rest")
                    .font(.caption2)
                    .foregroundStyle(.secondary)

                Text(timerInterval: context.state.restTimerStartDate...endDate, countsDown: true)
                    .font(.system(.title2, design: .monospaced, weight: .bold))
                    .monospacedDigit()
                    .foregroundStyle(.orange)
            }
        } else {
            // Last set info
            HStack(spacing: 12) {
                VStack(spacing: 2) {
                    Text("Weight")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(String(format: "%.1f kg", context.state.lastWeight))
                        .font(.subheadline)
                        .fontWeight(.medium)
                }

                VStack(spacing: 2) {
                    Text("Reps")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text("\(context.state.lastReps)")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
            }
        }
    }

    @ViewBuilder
    private func expandedBottom(context: ActivityViewContext<WorkoutActivityAttributes>) -> some View {
        HStack {
            Label("\(context.state.completedSets) sets", systemImage: "checkmark.circle")
                .font(.caption)
                .foregroundStyle(.secondary)

            Spacer()

            Label(formatVolume(context.state.totalVolume), systemImage: "scalemass")
                .font(.caption)
                .foregroundStyle(.secondary)

            Spacer()

            Label(formatDuration(context.state.elapsedSeconds), systemImage: "clock")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Helpers

    private func formatVolume(_ volume: Double) -> String {
        if volume >= 1000 {
            return String(format: "%.1fk kg", volume / 1000)
        }
        return String(format: "%.0f kg", volume)
    }

    private func formatDuration(_ seconds: Int) -> String {
        let hours = seconds / 3600
        let minutes = (seconds % 3600) / 60
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        }
        return "\(minutes)m"
    }
}
