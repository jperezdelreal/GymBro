import SwiftUI
import SwiftData
import GymBroCore
import GymBroUI

@main
struct GymBroApp: App {
    @State private var authService = AuthenticationService()
    @State private var syncService = CloudKitSyncService()

    var body: some Scene {
        WindowGroup {
            ContentView(
                authService: authService,
                syncService: syncService
            )
                .task {
                    await authService.checkExistingCredential()
                    if authService.isSignedIn {
                        await syncService.checkAccountStatus()
                        syncService.startMonitoring()
                    }
                }
        }
        .modelContainer(
            for: [
                Workout.self,
                Exercise.self,
                ExerciseSet.self,
                Program.self,
                ProgramDay.self,
                ProgramWeek.self,
                PlannedExercise.self,
                UserProfile.self,
                ChatMessage.self,
                HealthMetric.self,
                HealthBaseline.self,
                ConflictResolutionLog.self
            ],
            configurations: CloudKitSyncService.makeModelConfiguration(
                isSignedIn: authService.isSignedIn
            )
        )
    }
}
