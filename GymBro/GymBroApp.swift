import SwiftUI
import SwiftData
import GymBroCore

@main
struct GymBroApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(for: [
            Workout.self,
            Exercise.self,
            ExerciseSet.self,
            Program.self,
            ProgramDay.self,
            UserProfile.self,
            ChatMessage.self
        ])
    }
}
