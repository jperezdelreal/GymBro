import SwiftUI
import SwiftData
import GymBroCore
import GymBroUI

@main
struct GymBroApp: App {
    @State private var authService = AuthenticationService()
    @State private var syncService = CloudKitSyncService()
    @State private var conflictService = ConflictResolutionService()

    private let healthKitCoordinator = HealthKitCoordinator(
        healthKitService: HealthKitManager()
    )

    var body: some Scene {
        WindowGroup {
            ContentView(
                authService: authService,
                syncService: syncService,
                healthKitCoordinator: healthKitCoordinator
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
                ReadinessScore.self,
                SubjectiveCheckIn.self,
                ConflictResolutionLog.self
            ],
            configurations: CloudKitSyncService.makeModelConfiguration(
                isSignedIn: authService.isSignedIn
            )
        )
    }
}
