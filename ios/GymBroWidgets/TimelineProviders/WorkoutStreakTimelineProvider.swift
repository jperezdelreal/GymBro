import WidgetKit
import SwiftUI
import SwiftData
import AppIntents

// MARK: - Timeline Entry

struct WorkoutStreakEntry: TimelineEntry {
    let date: Date
    let streak: Int
    let daysSinceLastWorkout: Int
}

// MARK: - AppIntent Configuration

struct WorkoutStreakConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Workout Streak"
    static var description: IntentDescription = "Shows your current workout streak."
}

// MARK: - Timeline Provider

struct WorkoutStreakTimelineProvider: AppIntentTimelineProvider {
    typealias Entry = WorkoutStreakEntry
    typealias Intent = WorkoutStreakConfigurationIntent

    func placeholder(in context: Context) -> WorkoutStreakEntry {
        WorkoutStreakEntry(date: .now, streak: 5, daysSinceLastWorkout: 0)
    }

    func snapshot(for configuration: WorkoutStreakConfigurationIntent, in context: Context) async -> WorkoutStreakEntry {
        await fetchEntry()
    }

    func timeline(for configuration: WorkoutStreakConfigurationIntent, in context: Context) async -> Timeline<WorkoutStreakEntry> {
        let entry = await fetchEntry()
        // Refresh every 30 minutes
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 30, to: entry.date) ?? entry.date
        return Timeline(entries: [entry], policy: .after(nextUpdate))
    }

    @MainActor
    private func fetchEntry() -> WorkoutStreakEntry {
        do {
            let container = try ModelContainer(for: Workout.self)
            let provider = WidgetDataProvider(modelContext: container.mainContext)
            return WorkoutStreakEntry(
                date: .now,
                streak: provider.currentStreak(),
                daysSinceLastWorkout: provider.daysSinceLastWorkout()
            )
        } catch {
            return WorkoutStreakEntry(date: .now, streak: 0, daysSinceLastWorkout: 0)
        }
    }
}
