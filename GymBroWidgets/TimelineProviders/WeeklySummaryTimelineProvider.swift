import WidgetKit
import SwiftUI
import SwiftData
import AppIntents

// MARK: - Timeline Entry

struct WeeklySummaryEntry: TimelineEntry {
    let date: Date
    let totalVolume: Double
    let workoutCount: Int
    let recentPRs: Int
    let streak: Int
    let nextWorkoutName: String?
}

// MARK: - AppIntent Configuration

struct WeeklySummaryConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Weekly Summary"
    static var description: IntentDescription = "Shows your weekly training volume, workout count, and upcoming workouts."
}

// MARK: - Timeline Provider

struct WeeklySummaryTimelineProvider: AppIntentTimelineProvider {
    typealias Entry = WeeklySummaryEntry
    typealias Intent = WeeklySummaryConfigurationIntent

    func placeholder(in context: Context) -> WeeklySummaryEntry {
        WeeklySummaryEntry(
            date: .now,
            totalVolume: 12500,
            workoutCount: 4,
            recentPRs: 2,
            streak: 5,
            nextWorkoutName: "Legs"
        )
    }

    func snapshot(for configuration: WeeklySummaryConfigurationIntent, in context: Context) async -> WeeklySummaryEntry {
        await fetchEntry()
    }

    func timeline(for configuration: WeeklySummaryConfigurationIntent, in context: Context) async -> Timeline<WeeklySummaryEntry> {
        let entry = await fetchEntry()
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 30, to: entry.date) ?? entry.date
        return Timeline(entries: [entry], policy: .after(nextUpdate))
    }

    @MainActor
    private func fetchEntry() -> WeeklySummaryEntry {
        do {
            let container = try ModelContainer(for: Workout.self, ExerciseSet.self, Program.self, ProgramDay.self)
            let provider = WidgetDataProvider(modelContext: container.mainContext)

            return WeeklySummaryEntry(
                date: .now,
                totalVolume: provider.weeklyVolume(),
                workoutCount: provider.weeklyWorkoutCount(),
                recentPRs: provider.recentPRCount(),
                streak: provider.currentStreak(),
                nextWorkoutName: provider.nextScheduledWorkout()?.name
            )
        } catch {
            return WeeklySummaryEntry(
                date: .now,
                totalVolume: 0,
                workoutCount: 0,
                recentPRs: 0,
                streak: 0,
                nextWorkoutName: nil
            )
        }
    }
}
