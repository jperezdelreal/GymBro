import WidgetKit
import SwiftUI
import SwiftData
import AppIntents

// MARK: - Timeline Entry

struct NextWorkoutEntry: TimelineEntry {
    let date: Date
    let workoutName: String?
    let exerciseCount: Int
    let readinessScore: Int
    let isRestDay: Bool
}

// MARK: - AppIntent Configuration

struct NextWorkoutConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Next Workout"
    static var description: IntentDescription = "Shows your next scheduled workout and readiness score."
}

// MARK: - Timeline Provider

struct NextWorkoutTimelineProvider: AppIntentTimelineProvider {
    typealias Entry = NextWorkoutEntry
    typealias Intent = NextWorkoutConfigurationIntent

    func placeholder(in context: Context) -> NextWorkoutEntry {
        NextWorkoutEntry(
            date: .now,
            workoutName: "Push Day",
            exerciseCount: 6,
            readinessScore: 85,
            isRestDay: false
        )
    }

    func snapshot(for configuration: NextWorkoutConfigurationIntent, in context: Context) async -> NextWorkoutEntry {
        await fetchEntry()
    }

    func timeline(for configuration: NextWorkoutConfigurationIntent, in context: Context) async -> Timeline<NextWorkoutEntry> {
        let entry = await fetchEntry()
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 30, to: entry.date) ?? entry.date
        return Timeline(entries: [entry], policy: .after(nextUpdate))
    }

    @MainActor
    private func fetchEntry() -> NextWorkoutEntry {
        do {
            let container = try ModelContainer(for: Workout.self, Program.self, ProgramDay.self)
            let provider = WidgetDataProvider(modelContext: container.mainContext)
            let next = provider.nextScheduledWorkout()
            let daysSince = provider.daysSinceLastWorkout()

            // Simple readiness heuristic: decrease if many consecutive days, increase on rest
            let readiness = min(100, max(40, 100 - (provider.weeklyWorkoutCount() * 10) + (daysSince * 15)))

            return NextWorkoutEntry(
                date: .now,
                workoutName: next?.name,
                exerciseCount: next?.exerciseCount ?? 0,
                readinessScore: readiness,
                isRestDay: daysSince >= 2 || next == nil
            )
        } catch {
            return NextWorkoutEntry(
                date: .now,
                workoutName: nil,
                exerciseCount: 0,
                readinessScore: 75,
                isRestDay: false
            )
        }
    }
}
