import Testing
import Foundation
import SwiftData
@testable import GymBroCore

@Suite("WidgetDataProvider Tests")
struct WidgetDataProviderTests {

    @Test("WidgetSnapshot placeholder has reasonable values")
    func placeholderValues() {
        let placeholder = WidgetSnapshot.placeholder
        #expect(placeholder.streak == 5)
        #expect(placeholder.weeklyVolume == 12500)
        #expect(placeholder.weeklyWorkoutCount == 4)
        #expect(placeholder.recentPRs == 2)
        #expect(placeholder.readinessScore == 85)
        #expect(placeholder.nextWorkout?.name == "Push Day")
    }

    @Test("NextWorkoutInfo stores values correctly")
    func nextWorkoutInfo() {
        let info = NextWorkoutInfo(name: "Pull Day", scheduledDate: nil, exerciseCount: 5)
        #expect(info.name == "Pull Day")
        #expect(info.scheduledDate == nil)
        #expect(info.exerciseCount == 5)
    }

    @Test("NextWorkoutInfo with scheduled date")
    func nextWorkoutInfoWithDate() {
        let date = Date()
        let info = NextWorkoutInfo(name: "Legs", scheduledDate: date, exerciseCount: 8)
        #expect(info.name == "Legs")
        #expect(info.scheduledDate == date)
        #expect(info.exerciseCount == 8)
    }

    @Test("WidgetSnapshot stores all fields")
    func snapshotFields() {
        let next = NextWorkoutInfo(name: "Upper", scheduledDate: nil, exerciseCount: 6)
        let snapshot = WidgetSnapshot(
            streak: 3,
            daysSinceLastWorkout: 1,
            weeklyVolume: 8000,
            weeklyWorkoutCount: 3,
            recentPRs: 1,
            nextWorkout: next,
            readinessScore: 72
        )

        #expect(snapshot.streak == 3)
        #expect(snapshot.daysSinceLastWorkout == 1)
        #expect(snapshot.weeklyVolume == 8000)
        #expect(snapshot.weeklyWorkoutCount == 3)
        #expect(snapshot.recentPRs == 1)
        #expect(snapshot.readinessScore == 72)
        #expect(snapshot.nextWorkout?.name == "Upper")
    }
}
