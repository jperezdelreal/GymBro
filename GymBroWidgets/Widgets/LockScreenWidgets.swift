import WidgetKit
import SwiftUI

// MARK: - Lock Screen Widgets Collection

struct LockScreenInlineWidget: Widget {
    let kind = "LockScreenInlineWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: NextWorkoutConfigurationIntent.self,
            provider: NextWorkoutTimelineProvider()
        ) { entry in
            LockScreenInlineView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Workout Preview")
        .description("Quick glance at your next workout.")
        .supportedFamilies([.accessoryInline])
    }
}

struct LockScreenInlineView: View {
    let entry: NextWorkoutEntry

    var body: some View {
        if let name = entry.workoutName {
            Text("💪 \(name) • \(entry.exerciseCount) exercises")
        } else if entry.isRestDay {
            Text("😴 Rest Day — Readiness \(entry.readinessScore)%")
        } else {
            Text("🏋️ Ready to train")
        }
    }
}

// MARK: - StandBy Widget (Rectangular, used in StandBy mode)

struct StandByWorkoutWidget: Widget {
    let kind = "StandByWorkoutWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: NextWorkoutConfigurationIntent.self,
            provider: NextWorkoutTimelineProvider()
        ) { entry in
            StandByWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("GymBro StandBy")
        .description("Today's readiness and next workout for StandBy mode.")
        .supportedFamilies([.systemSmall])
    }
}

struct StandByWidgetView: View {
    let entry: NextWorkoutEntry

    var body: some View {
        VStack(spacing: 8) {
            // Readiness score dominant
            ZStack {
                Circle()
                    .trim(from: 0, to: 0.75)
                    .stroke(.quaternary, lineWidth: 8)
                    .rotationEffect(.degrees(135))

                Circle()
                    .trim(from: 0, to: 0.75 * Double(entry.readinessScore) / 100)
                    .stroke(readinessGradient, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(135))

                VStack(spacing: 0) {
                    Text("\(entry.readinessScore)")
                        .font(.system(size: 28, weight: .bold, design: .rounded))

                    Text("ready")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 80, height: 80)

            if let name = entry.workoutName {
                Text(name)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .lineLimit(1)
            } else if entry.isRestDay {
                Text("Rest Day")
                    .font(.caption)
                    .fontWeight(.semibold)
            }
        }
        .widgetURL(URL(string: "gymbro://workout"))
    }

    private var readinessGradient: AngularGradient {
        AngularGradient(
            colors: [.red, .orange, .yellow, .green],
            center: .center,
            startAngle: .degrees(135),
            endAngle: .degrees(135 + 270 * Double(entry.readinessScore) / 100)
        )
    }
}

// MARK: - Previews

#Preview("Inline", as: .accessoryInline) {
    LockScreenInlineWidget()
} timeline: {
    NextWorkoutEntry(date: .now, workoutName: "Chest & Triceps", exerciseCount: 5, readinessScore: 82, isRestDay: false)
}

#Preview("StandBy", as: .systemSmall) {
    StandByWorkoutWidget()
} timeline: {
    NextWorkoutEntry(date: .now, workoutName: "Push Day", exerciseCount: 6, readinessScore: 88, isRestDay: false)
    NextWorkoutEntry(date: .now, workoutName: nil, exerciseCount: 0, readinessScore: 95, isRestDay: true)
}
