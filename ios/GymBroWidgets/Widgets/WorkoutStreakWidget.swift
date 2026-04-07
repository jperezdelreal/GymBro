import WidgetKit
import SwiftUI

// MARK: - Workout Streak Widget (Small)

struct WorkoutStreakWidget: Widget {
    let kind = "WorkoutStreakWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: WorkoutStreakConfigurationIntent.self,
            provider: WorkoutStreakTimelineProvider()
        ) { entry in
            WorkoutStreakWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Workout Streak")
        .description("Track your consecutive workout days.")
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

// MARK: - Home Screen Small

struct WorkoutStreakWidgetView: View {
    let entry: WorkoutStreakEntry

    @Environment(\.widgetFamily) var family

    var body: some View {
        switch family {
        case .systemSmall:
            systemSmallView
        case .accessoryCircular:
            circularView
        case .accessoryRectangular:
            rectangularView
        case .accessoryInline:
            inlineView
        default:
            systemSmallView
        }
    }

    private var systemSmallView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "flame.fill")
                    .foregroundStyle(.orange)
                    .font(.title2)
                Spacer()
            }

            Spacer()

            Text("\(entry.streak)")
                .font(.system(size: 48, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)

            Text(entry.streak == 1 ? "day streak" : "day streak")
                .font(.caption)
                .foregroundStyle(.secondary)

            if entry.daysSinceLastWorkout > 0 {
                Text("\(entry.daysSinceLastWorkout)d since last workout")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .widgetURL(URL(string: "gymbro://workout"))
    }

    // MARK: - Lock Screen Circular

    private var circularView: some View {
        ZStack {
            AccessoryWidgetBackground()

            VStack(spacing: 1) {
                Image(systemName: "flame.fill")
                    .font(.caption)
                Text("\(entry.streak)")
                    .font(.system(.title2, design: .rounded, weight: .bold))
            }
        }
        .widgetURL(URL(string: "gymbro://workout"))
    }

    // MARK: - Lock Screen Rectangular

    private var rectangularView: some View {
        HStack(spacing: 8) {
            Image(systemName: "flame.fill")
                .font(.title3)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(entry.streak)-day streak")
                    .font(.headline)
                    .widgetAccentable()

                if entry.daysSinceLastWorkout > 0 {
                    Text("Last workout \(entry.daysSinceLastWorkout)d ago")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Text("Trained today 💪")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
        }
        .widgetURL(URL(string: "gymbro://workout"))
    }

    // MARK: - Lock Screen Inline

    private var inlineView: some View {
        Text("🔥 \(entry.streak)-day streak")
            .widgetURL(URL(string: "gymbro://workout"))
    }
}

// MARK: - Previews

#Preview("Small", as: .systemSmall) {
    WorkoutStreakWidget()
} timeline: {
    WorkoutStreakEntry(date: .now, streak: 5, daysSinceLastWorkout: 0)
    WorkoutStreakEntry(date: .now, streak: 0, daysSinceLastWorkout: 3)
}

#Preview("Circular", as: .accessoryCircular) {
    WorkoutStreakWidget()
} timeline: {
    WorkoutStreakEntry(date: .now, streak: 12, daysSinceLastWorkout: 0)
}

#Preview("Rectangular", as: .accessoryRectangular) {
    WorkoutStreakWidget()
} timeline: {
    WorkoutStreakEntry(date: .now, streak: 7, daysSinceLastWorkout: 1)
}
