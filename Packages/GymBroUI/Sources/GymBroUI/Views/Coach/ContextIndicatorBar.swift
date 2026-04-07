import SwiftUI
import GymBroCore

/// Context indicator bar showing what data the AI coach can see.
/// Displays workout count, weeks of data, and last workout date.
struct ContextIndicatorBar: View {
    let summary: CoachContextSummary

    var body: some View {
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: "chart.bar.fill")
                .font(.caption2)
                .foregroundStyle(GymBroColors.accentCyan)

            if summary.workoutCount > 0 {
                Text(contextText)
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textSecondary)
            } else {
                Text("No workouts tracked yet -- start training to unlock insights")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
            }

            Spacer()
        }
        .padding(.horizontal, GymBroSpacing.md)
        .padding(.vertical, GymBroSpacing.sm)
        .background(GymBroColors.surfacePrimary)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityText)
    }

    private var contextText: String {
        var parts: [String] = []
        parts.append("\(summary.workoutCount) workout\(summary.workoutCount == 1 ? "" : "s") tracked")
        if summary.weeksOfData > 0 {
            parts.append("\(summary.weeksOfData) week\(summary.weeksOfData == 1 ? "" : "s") of data")
        }
        return parts.joined(separator: " \u{00B7} ")
    }

    private var accessibilityText: String {
        if summary.workoutCount == 0 {
            return "No workouts tracked yet"
        }
        return "\(summary.workoutCount) workouts tracked over \(summary.weeksOfData) weeks"
    }
}

#Preview("Context Indicator -- Data") {
    ContextIndicatorBar(summary: CoachContextSummary(
        workoutCount: 12,
        weeksOfData: 3,
        lastWorkoutDate: Date()
    ))
    .gymBroDarkBackground()
}

#Preview("Context Indicator -- Empty") {
    ContextIndicatorBar(summary: CoachContextSummary())
        .gymBroDarkBackground()
}
