import SwiftUI
import WatchKit

/// Post-workout summary shown on the Watch after a workout ends.
struct WorkoutSummaryWatchView: View {
    let setsCompleted: Int
    let totalVolume: Double
    let duration: String

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Image(systemName: "trophy.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(.yellow)
                    .accessibilityHidden(true)

                Text("Workout Complete")
                    .font(.system(.headline, design: .rounded))

                Divider()

                VStack(spacing: 8) {
                    summaryRow(
                        icon: "clock",
                        label: "Duration",
                        value: duration
                    )
                    summaryRow(
                        icon: "number",
                        label: "Sets",
                        value: "\(setsCompleted)"
                    )
                    summaryRow(
                        icon: "scalemass",
                        label: "Volume",
                        value: String(format: "%.0f kg", totalVolume)
                    )
                }
                .padding(.horizontal, 4)

                Text("Great work! 💪")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.top, 4)
            }
            .padding(.vertical, 8)
        }
    }

    private func summaryRow(icon: String, label: String, value: String) -> some View {
        HStack {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(.blue)
                .frame(width: 20)
                .accessibilityHidden(true)

            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)

            Spacer()

            Text(value)
                .font(.system(.caption, design: .rounded, weight: .bold))
                .monospacedDigit()
        }
    }
}
