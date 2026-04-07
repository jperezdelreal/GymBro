import WidgetKit
import SwiftUI

// MARK: - Weekly Summary Widget (Large)

struct WeeklySummaryWidget: Widget {
    let kind = "WeeklySummaryWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: WeeklySummaryConfigurationIntent.self,
            provider: WeeklySummaryTimelineProvider()
        ) { entry in
            WeeklySummaryWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Weekly Summary")
        .description("Overview of your weekly training progress.")
        .supportedFamilies([.systemLarge])
    }
}

// MARK: - Home Screen Large

struct WeeklySummaryWidgetView: View {
    let entry: WeeklySummaryEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Label("This Week", systemImage: "calendar")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                Spacer()
                Text("🔥 \(entry.streak)")
                    .font(.callout)
            }

            Divider()

            // Stats grid
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                statCard(
                    title: "Volume",
                    value: formatVolume(entry.totalVolume),
                    icon: "scalemass.fill",
                    color: .blue
                )

                statCard(
                    title: "Workouts",
                    value: "\(entry.workoutCount)",
                    icon: "figure.strengthtraining.traditional",
                    color: .green
                )

                statCard(
                    title: "PRs This Week",
                    value: "\(entry.recentPRs)",
                    icon: "trophy.fill",
                    color: .yellow
                )

                statCard(
                    title: "Streak",
                    value: "\(entry.streak) days",
                    icon: "flame.fill",
                    color: .orange
                )
            }

            Divider()

            // Next workout preview
            HStack {
                Image(systemName: "arrow.right.circle.fill")
                    .foregroundStyle(.blue)
                if let next = entry.nextWorkoutName {
                    Text("Up next: \(next)")
                        .font(.subheadline)
                        .fontWeight(.medium)
                } else {
                    Text("No workout scheduled")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
        .widgetURL(URL(string: "gymbro://history"))
    }

    private func statCard(title: String, value: String, icon: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .foregroundStyle(color)
                    .font(.caption)
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Text(value)
                .font(.system(.title3, design: .rounded, weight: .bold))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func formatVolume(_ volume: Double) -> String {
        if volume >= 1000 {
            return String(format: "%.1fk kg", volume / 1000)
        }
        return String(format: "%.0f kg", volume)
    }
}

// MARK: - Readiness Widget (Lock Screen Circular Gauge)

struct ReadinessWidget: Widget {
    let kind = "ReadinessWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: NextWorkoutConfigurationIntent.self,
            provider: NextWorkoutTimelineProvider()
        ) { entry in
            ReadinessWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Readiness")
        .description("Your current training readiness score.")
        .supportedFamilies([.accessoryCircular])
    }
}

struct ReadinessWidgetView: View {
    let entry: NextWorkoutEntry

    var body: some View {
        Gauge(value: Double(entry.readinessScore), in: 0...100) {
            Image(systemName: "bolt.fill")
        } currentValueLabel: {
            Text("\(entry.readinessScore)")
                .font(.system(.body, design: .rounded, weight: .bold))
        }
        .gaugeStyle(.accessoryCircular)
        .widgetURL(URL(string: "gymbro://workout"))
    }
}

// MARK: - Previews

#Preview("Large", as: .systemLarge) {
    WeeklySummaryWidget()
} timeline: {
    WeeklySummaryEntry(date: .now, totalVolume: 14250, workoutCount: 4, recentPRs: 2, streak: 7, nextWorkoutName: "Legs")
}

#Preview("Readiness Circular", as: .accessoryCircular) {
    ReadinessWidget()
} timeline: {
    NextWorkoutEntry(date: .now, workoutName: "Push", exerciseCount: 5, readinessScore: 82, isRestDay: false)
}
