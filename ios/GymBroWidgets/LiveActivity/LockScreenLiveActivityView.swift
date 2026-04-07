import SwiftUI
import WidgetKit
import ActivityKit

/// Lock Screen banner view for the workout Live Activity.
struct LockScreenLiveActivityView: View {
    let context: ActivityViewContext<WorkoutActivityAttributes>

    var body: some View {
        VStack(spacing: 12) {
            // Top: Exercise and set info
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(context.state.exerciseName)
                        .font(.headline)
                        .fontWeight(.bold)
                        .lineLimit(1)

                    if context.state.totalPlannedSets > 0 {
                        Text("Set \(context.state.currentSetNumber) of \(context.state.totalPlannedSets)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    } else {
                        Text("Set \(context.state.currentSetNumber)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }

                Spacer()

                // Timer or working indicator
                if let endDate = context.state.restTimerEndDate {
                    restTimerView(endDate: endDate)
                } else {
                    workingIndicator
                }
            }

            // Bottom: Stats bar
            HStack {
                statPill(icon: "checkmark.circle", value: "\(context.state.completedSets) sets")
                Spacer()
                statPill(icon: "scalemass", value: formatVolume(context.state.totalVolume))
                Spacer()
                statPill(icon: "clock", value: formatDuration(context.state.elapsedSeconds))
            }
        }
        .padding()
    }

    // MARK: - Rest Timer

    private func restTimerView(endDate: Date) -> some View {
        VStack(spacing: 2) {
            Text("REST")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.orange)

            Text(timerInterval: context.state.restTimerStartDate...endDate, countsDown: true)
                .font(.system(.title, design: .monospaced, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(.orange)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(.orange.opacity(0.15), in: RoundedRectangle(cornerRadius: 10))
    }

    private var workingIndicator: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(.green)
                .frame(width: 8, height: 8)
            Text("Working")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(.green)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(.green.opacity(0.15), in: RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Stat Pill

    private func statPill(icon: String, value: String) -> some View {
        Label(value, systemImage: icon)
            .font(.caption)
            .foregroundStyle(.secondary)
    }

    // MARK: - Formatting

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
