import SwiftUI
import GymBroCore

public struct WorkoutSummaryView: View {
    let summary: WorkoutSummary
    @Environment(\.dismiss) private var dismiss

    @ScaledMetric(relativeTo: .largeTitle) private var celebrationIconSize: CGFloat = 80
    @ScaledMetric(relativeTo: .title) private var titleSize: CGFloat = 32
    @ScaledMetric(relativeTo: .title2) private var cardIconSize: CGFloat = 28
    @ScaledMetric(relativeTo: .title2) private var cardValueSize: CGFloat = 24

    public init(summary: WorkoutSummary) {
        self.summary = summary
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: GymBroSpacing.xl) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: celebrationIconSize))
                        .foregroundStyle(GymBroColors.accentGreen)
                        .padding(.top, GymBroSpacing.xxl)
                        .accessibilityHidden(true)

                    Text("Workout Complete!")
                        .font(.system(size: titleSize, weight: .bold))
                        .foregroundStyle(GymBroColors.textPrimary)

                    VStack(spacing: GymBroSpacing.md + GymBroSpacing.xs) {
                        summaryCard(
                            icon: "clock.fill",
                            title: "Duration",
                            value: formatDuration(summary.duration),
                            color: GymBroColors.accentCyan
                        )

                        summaryCard(
                            icon: "scalemass.fill",
                            title: "Total Volume",
                            value: String(format: "%.0f kg", summary.totalVolume),
                            color: GymBroColors.accentGreen
                        )

                        summaryCard(
                            icon: "chart.bar.fill",
                            title: "Sets Completed",
                            value: "\(summary.totalSets)",
                            color: GymBroColors.accentAmber
                        )

                        if summary.personalRecords > 0 {
                            summaryCard(
                                icon: "star.fill",
                                title: "Personal Records",
                                value: "\(summary.personalRecords)",
                                color: GymBroColors.accentAmber
                            )
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.lg)

                    Button {
                        dismiss()
                    } label: {
                        Text("Done")
                    }
                    .buttonStyle(.gymBroPrimary)
                    .padding(.horizontal, GymBroSpacing.lg)
                    .padding(.top, GymBroSpacing.md + GymBroSpacing.xs)
                }
                .padding(.bottom, GymBroSpacing.xxl)
            }
            .gymBroDarkBackground()
            .navigationTitle("Summary")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func summaryCard(icon: String, title: String, value: String, color: Color) -> some View {
        GymBroCard(accent: color) {
            HStack(spacing: GymBroSpacing.md) {
                Image(systemName: icon)
                    .font(.system(size: cardIconSize))
                    .foregroundStyle(color)
                    .frame(width: 50)

                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    Text(title)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                    Text(value)
                        .font(GymBroTypography.monoNumber(size: cardValueSize))
                        .foregroundStyle(GymBroColors.textPrimary)
                }

                Spacer()
            }
        }
    }

    private func formatDuration(_ duration: TimeInterval) -> String {
        let hours = Int(duration) / 3600
        let minutes = Int(duration) / 60 % 60

        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
}

// MARK: - Preview

#Preview("Workout Summary") {
    WorkoutSummaryView(summary: WorkoutSummary(
        duration: 3720,
        totalVolume: 8450,
        totalSets: 18,
        personalRecords: 2
    ))
}

#Preview("Workout Summary — No PRs") {
    WorkoutSummaryView(summary: WorkoutSummary(
        duration: 2100,
        totalVolume: 3200,
        totalSets: 12,
        personalRecords: 0
    ))
}
