import SwiftUI
import WidgetKit

/// The main widget bundle that registers all GymBro widgets.
@main
struct GymBroWidgetBundle: WidgetBundle {
    var body: some Widget {
        // Home Screen Widgets
        WorkoutStreakWidget()
        NextWorkoutWidget()
        WeeklySummaryWidget()

        // Lock Screen Widgets
        ReadinessWidget()
        LockScreenInlineWidget()

        // StandBy Widget
        StandByWorkoutWidget()

        // Live Activity
        WorkoutLiveActivity()
    }
}
