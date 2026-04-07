import WidgetKit
import SwiftUI

// MARK: - Next Workout Widget (Medium)

struct NextWorkoutWidget: Widget {
    let kind = "NextWorkoutWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: NextWorkoutConfigurationIntent.self,
            provider: NextWorkoutTimelineProvider()
        ) { entry in
            NextWorkoutWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Next Workout")
        .description("See your next scheduled workout and readiness score.")
        .supportedFamilies([.systemMedium, .accessoryRectangular])
    }
}

// MARK: - Home Screen Medium

struct NextWorkoutWidgetView: View {
    let entry: NextWorkoutEntry

    @Environment(\.widgetFamily) var family

    var body: some View {
        switch family {
        case .systemMedium:
            systemMediumView
        case .accessoryRectangular:
            rectangularView
        default:
            systemMediumView
        }
    }

    private var systemMediumView: some View {
        HStack(spacing: 16) {
            // Left: Next workout info
            VStack(alignment: .leading, spacing: 8) {
                Label("Next Workout", systemImage: "figure.strengthtraining.traditional")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                if let name = entry.workoutName {
                    Text(name)
                        .font(.title2)
                        .fontWeight(.bold)
                        .lineLimit(1)

                    Text("\(entry.exerciseCount) exercises")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else if entry.isRestDay {
                    Text("Rest Day")
                        .font(.title2)
                        .fontWeight(.bold)

                    Text("Recovery is training too")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else {
                    Text("No workout planned")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            // Right: Readiness gauge
            readinessGauge
        }
        .widgetURL(URL(string: "gymbro://workout"))
    }

    private var readinessGauge: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .trim(from: 0, to: 0.75)
                    .stroke(.quaternary, lineWidth: 6)
                    .rotationEffect(.degrees(135))

                Circle()
                    .trim(from: 0, to: 0.75 * Double(entry.readinessScore) / 100)
                    .stroke(readinessColor, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(135))

                Text("\(entry.readinessScore)")
                    .font(.system(.title3, design: .rounded, weight: .bold))
            }
            .frame(width: 64, height: 64)

            Text("Readiness")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private var readinessColor: Color {
        switch entry.readinessScore {
        case 80...100: .green
        case 60..<80: .yellow
        case 40..<60: .orange
        default: .red
        }
    }

    // MARK: - Lock Screen Rectangular

    private var rectangularView: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let name = entry.workoutName {
                Text(name)
                    .font(.headline)
                    .widgetAccentable()

                Text("\(entry.exerciseCount) exercises • Readiness \(entry.readinessScore)%")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else if entry.isRestDay {
                Text("Rest Day 😴")
                    .font(.headline)
                    .widgetAccentable()

                Text("Recover and come back stronger")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .widgetURL(URL(string: "gymbro://workout"))
    }
}

// MARK: - Previews

#Preview("Medium", as: .systemMedium) {
    NextWorkoutWidget()
} timeline: {
    NextWorkoutEntry(date: .now, workoutName: "Push Day", exerciseCount: 6, readinessScore: 85, isRestDay: false)
    NextWorkoutEntry(date: .now, workoutName: nil, exerciseCount: 0, readinessScore: 95, isRestDay: true)
}

#Preview("Rectangular", as: .accessoryRectangular) {
    NextWorkoutWidget()
} timeline: {
    NextWorkoutEntry(date: .now, workoutName: "Chest & Triceps", exerciseCount: 5, readinessScore: 78, isRestDay: false)
}
