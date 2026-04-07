import SwiftUI
import SwiftData
import GymBroCore

/// ViewModel that drives the progress dashboard.
/// Fetches workout data from SwiftData and computes all progress metrics.
@MainActor
public class ProgressDashboardViewModel: ObservableObject {

    @Published public var selectedTimeWindow: TimeWindow = .threeMonths
    @Published public var selectedExerciseName: String?
    @Published public var availableExercises: [String] = []

    // Chart data
    @Published public var e1rmData: [E1RMDataPoint] = []
    @Published public var volumeData: [VolumeDataPoint] = []
    @Published public var tonnageData: [VolumeDataPoint] = []
    @Published public var frequencyData: [FrequencyDataPoint] = []
    @Published public var muscleBalance: [MuscleGroupBalance] = []
    @Published public var prEvents: [PREvent] = []
    @Published public var plateauAnalyses: [PlateauAnalysis] = []

    @Published public var isLoading: Bool = false

    private let progressService: ProgressTrackingService
    private let plateauService: PlateauDetectionService
    private let e1rmCalculator: E1RMCalculator

    public init(
        progressService: ProgressTrackingService = ProgressTrackingService(),
        plateauService: PlateauDetectionService = PlateauDetectionService(),
        e1rmCalculator: E1RMCalculator = E1RMCalculator()
    ) {
        self.progressService = progressService
        self.plateauService = plateauService
        self.e1rmCalculator = e1rmCalculator
    }

    /// Refreshes all progress data from SwiftData.
    public func refresh(workouts: [Workout], allSets: [ExerciseSet]) {
        isLoading = true

        let exerciseNames = Set(allSets.compactMap { $0.exercise?.name })
        availableExercises = exerciseNames.sorted()

        if selectedExerciseName == nil {
            selectedExerciseName = availableExercises.first
        }

        if let exerciseName = selectedExerciseName {
            let exerciseSets = allSets.filter { $0.exercise?.name == exerciseName }
            e1rmData = progressService.e1rmTrend(
                sets: exerciseSets,
                exerciseName: exerciseName,
                timeWindow: selectedTimeWindow
            )
            prEvents = progressService.detectPRs(sets: exerciseSets, exerciseName: exerciseName)

            if let exerciseId = exerciseSets.first?.exercise?.id {
                if let analysis = plateauService.analyze(
                    sets: exerciseSets,
                    exerciseId: exerciseId,
                    exerciseName: exerciseName
                ) {
                    plateauAnalyses = [analysis]
                } else {
                    plateauAnalyses = []
                }
            }
        }

        volumeData = progressService.weeklyVolume(sets: allSets, timeWindow: selectedTimeWindow)
        tonnageData = progressService.tonnagePerWorkout(workouts: workouts, timeWindow: selectedTimeWindow)
        frequencyData = progressService.weeklyFrequency(workouts: workouts, timeWindow: selectedTimeWindow)
        muscleBalance = progressService.muscleGroupBalance(sets: allSets, timeWindow: selectedTimeWindow)

        isLoading = false
    }
}
