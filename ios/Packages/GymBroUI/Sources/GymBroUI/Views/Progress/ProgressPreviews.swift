import SwiftUI
import Charts
import GymBroCore

// MARK: - ProgressDashboardView Previews

#Preview("Progress Dashboard") {
    ProgressDashboardView()
        .preferredColorScheme(.dark)
}

#Preview("Progress — Empty") {
    NavigationStack {
        ScrollView {
            ContentUnavailableView {
                Label("No Progress Data", systemImage: "chart.bar")
            } description: {
                Text("Complete a few workouts to see your progress trends here.")
            }
        }
        .navigationTitle("Progress")
    }
    .gymBroDarkBackground()
}

#Preview("Sparkline") {
    VStack(spacing: GymBroSpacing.lg) {
        SparklineView(
            data: [100, 102, 105, 103, 108, 110, 109, 112, 115, 118, 120, 122],
            color: GymBroColors.accentGreen
        )
        .frame(height: 60)

        SparklineView(
            data: [120, 118, 115, 112, 110, 108, 106, 105],
            color: GymBroColors.accentRed
        )
        .frame(height: 60)
    }
    .padding(GymBroSpacing.lg)
    .gymBroDarkBackground()
}

#Preview("Insight Card") {
    VStack(spacing: GymBroSpacing.md) {
        InsightCardView(
            insight: ProgressInsight(
                title: "Bench plateau detected",
                subtitle: "Try varying rep ranges or adding pause reps",
                icon: "exclamationmark.triangle.fill",
                accentColor: GymBroColors.accentAmber,
                chartType: .plateau
            )
        ) {}

        InsightCardView(
            insight: ProgressInsight(
                title: "Volume up 12%",
                subtitle: "Week-over-week change",
                icon: "arrow.up.right.circle.fill",
                accentColor: GymBroColors.accentGreen,
                chartType: .volume
            )
        ) {}

        InsightCardView(
            insight: ProgressInsight(
                title: "3 personal records",
                subtitle: "Bench Press — e1RM",
                icon: "trophy.fill",
                accentColor: GymBroColors.accentAmber,
                chartType: .prs
            )
        ) {}
    }
    .padding(GymBroSpacing.md)
    .gymBroDarkBackground()
}
