import Foundation

/// Protocol-based abstraction for the AI coach backend.
/// Enables swapping cloud (Azure OpenAI) with on-device models in the future.
public protocol AICoachService {
    /// Send a message and receive a complete response.
    func sendMessage(_ message: String, context: CoachContext) async throws -> String

    /// Send a message and receive a streaming response (tokens arrive incrementally).
    func streamMessage(_ message: String, context: CoachContext) -> AsyncThrowingStream<String, Error>
}

/// Contextual data passed to the AI coach for every request.
public struct CoachContext {
    public let userProfile: UserProfileSnapshot?
    public let recentWorkouts: [WorkoutSnapshot]
    public let activeProgram: ProgramSnapshot?
    public let personalRecords: [PRSnapshot]

    public init(
        userProfile: UserProfileSnapshot? = nil,
        recentWorkouts: [WorkoutSnapshot] = [],
        activeProgram: ProgramSnapshot? = nil,
        personalRecords: [PRSnapshot] = []
    ) {
        self.userProfile = userProfile
        self.recentWorkouts = recentWorkouts
        self.activeProgram = activeProgram
        self.personalRecords = personalRecords
    }
}

// MARK: - Lightweight snapshots (decoupled from SwiftData @Model objects)

public struct UserProfileSnapshot {
    public let experienceLevel: String
    public let unitSystem: String
    public let bodyweightKg: Double?

    public init(experienceLevel: String, unitSystem: String, bodyweightKg: Double? = nil) {
        self.experienceLevel = experienceLevel
        self.unitSystem = unitSystem
        self.bodyweightKg = bodyweightKg
    }
}

public struct WorkoutSnapshot {
    public let date: Date
    public let exercises: [ExerciseSnapshot]
    public let totalVolume: Double
    public let durationMinutes: Double?

    public init(date: Date, exercises: [ExerciseSnapshot], totalVolume: Double, durationMinutes: Double? = nil) {
        self.date = date
        self.exercises = exercises
        self.totalVolume = totalVolume
        self.durationMinutes = durationMinutes
    }
}

public struct ExerciseSnapshot {
    public let name: String
    public let sets: Int
    public let bestWeight: Double
    public let bestReps: Int
    public let avgRpe: Double?

    public init(name: String, sets: Int, bestWeight: Double, bestReps: Int, avgRpe: Double? = nil) {
        self.name = name
        self.sets = sets
        self.bestWeight = bestWeight
        self.bestReps = bestReps
        self.avgRpe = avgRpe
    }
}

public struct ProgramSnapshot {
    public let name: String
    public let periodization: String
    public let weekNumber: Int
    public let frequencyPerWeek: Int

    public init(name: String, periodization: String, weekNumber: Int, frequencyPerWeek: Int) {
        self.name = name
        self.periodization = periodization
        self.weekNumber = weekNumber
        self.frequencyPerWeek = frequencyPerWeek
    }
}

public struct PRSnapshot {
    public let exerciseName: String
    public let weightKg: Double
    public let reps: Int
    public let date: Date

    public init(exerciseName: String, weightKg: Double, reps: Int, date: Date) {
        self.exerciseName = exerciseName
        self.weightKg = weightKg
        self.reps = reps
        self.date = date
    }
}
