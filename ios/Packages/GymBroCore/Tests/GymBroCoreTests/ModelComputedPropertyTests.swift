import XCTest
@testable import GymBroCore

/// Comprehensive model computed property tests.
/// Replaces the near-useless GymBroCoreTests.swift assertions (issue #27).
final class ModelComputedPropertyTests: XCTestCase {

    // MARK: - ExerciseSet.volume

    func testVolume_normalValues() {
        let set = ExerciseSet(weightKg: 100, reps: 5)
        XCTAssertEqual(set.volume, 500)
    }

    func testVolume_zeroWeight() {
        let set = ExerciseSet(weightKg: 0, reps: 10)
        XCTAssertEqual(set.volume, 0)
    }

    func testVolume_zeroReps() {
        let set = ExerciseSet(weightKg: 100, reps: 0)
        XCTAssertEqual(set.volume, 0)
    }

    func testVolume_highValues() {
        let set = ExerciseSet(weightKg: 300, reps: 20)
        XCTAssertEqual(set.volume, 6000)
    }

    func testVolume_fractionalWeight() {
        let set = ExerciseSet(weightKg: 22.5, reps: 8)
        XCTAssertEqual(set.volume, 180.0, accuracy: 0.001)
    }

    // MARK: - ExerciseSet.estimatedOneRepMax

    func testE1RM_normalValues() {
        let set = ExerciseSet(weightKg: 100, reps: 5)
        // 100 * (1 + 5/30) = 116.67
        XCTAssertEqual(set.estimatedOneRepMax, 116.67, accuracy: 0.1)
    }

    func testE1RM_singleRep() {
        let set = ExerciseSet(weightKg: 150, reps: 1)
        // 150 * (1 + 1/30) = 155.0
        XCTAssertEqual(set.estimatedOneRepMax, 155.0, accuracy: 0.1)
    }

    func testE1RM_zeroReps_returnsWeight() {
        let set = ExerciseSet(weightKg: 100, reps: 0)
        XCTAssertEqual(set.estimatedOneRepMax, 100, "Zero reps should return base weight")
    }

    func testE1RM_zeroWeight() {
        let set = ExerciseSet(weightKg: 0, reps: 5)
        XCTAssertEqual(set.estimatedOneRepMax, 0)
    }

    // MARK: - ExerciseSet.isWarmup

    func testIsWarmup_warmupSet() {
        let set = ExerciseSet(weightKg: 50, reps: 10, setType: .warmup)
        XCTAssertTrue(set.isWarmup)
    }

    func testIsWarmup_workingSet() {
        let set = ExerciseSet(weightKg: 100, reps: 5, setType: .working)
        XCTAssertFalse(set.isWarmup)
    }

    func testIsWarmup_dropSet() {
        let set = ExerciseSet(weightKg: 80, reps: 8, setType: .drop)
        XCTAssertFalse(set.isWarmup)
    }

    func testIsWarmup_amrapSet() {
        let set = ExerciseSet(weightKg: 90, reps: 12, setType: .amrap)
        XCTAssertFalse(set.isWarmup)
    }

    // MARK: - ExerciseSet.weightInUnit

    func testWeightInUnit_metric() {
        let set = ExerciseSet(weightKg: 100, reps: 5)
        XCTAssertEqual(set.weightInUnit(.metric), 100)
    }

    func testWeightInUnit_imperial() {
        let set = ExerciseSet(weightKg: 100, reps: 5)
        XCTAssertEqual(set.weightInUnit(.imperial), 220.462, accuracy: 0.1)
    }

    func testWeightInUnit_zeroWeight() {
        let set = ExerciseSet(weightKg: 0, reps: 5)
        XCTAssertEqual(set.weightInUnit(.imperial), 0)
    }

    // MARK: - Workout.totalVolume

    func testWorkoutTotalVolume_noSets() {
        let workout = Workout()
        XCTAssertEqual(workout.totalVolume, 0)
    }

    func testWorkoutTotalVolume_withSets() {
        let workout = Workout()
        let set1 = ExerciseSet(workout: workout, weightKg: 100, reps: 5)
        let set2 = ExerciseSet(workout: workout, weightKg: 80, reps: 8)
        workout.sets = [set1, set2]
        // 500 + 640 = 1140
        XCTAssertEqual(workout.totalVolume, 1140, accuracy: 0.1)
    }

    // MARK: - Workout.totalSets (excludes warmups)

    func testWorkoutTotalSets_excludesWarmups() {
        let workout = Workout()
        let warmup = ExerciseSet(workout: workout, weightKg: 50, reps: 10, setType: .warmup)
        let working1 = ExerciseSet(workout: workout, weightKg: 100, reps: 5, setType: .working)
        let working2 = ExerciseSet(workout: workout, weightKg: 100, reps: 5, setType: .working)
        workout.sets = [warmup, working1, working2]
        XCTAssertEqual(workout.totalSets, 2, "Should exclude warmup sets")
    }

    func testWorkoutTotalSets_countsDrop() {
        let workout = Workout()
        let working = ExerciseSet(workout: workout, weightKg: 100, reps: 5, setType: .working)
        let drop = ExerciseSet(workout: workout, weightKg: 70, reps: 10, setType: .drop)
        workout.sets = [working, drop]
        XCTAssertEqual(workout.totalSets, 2, "Should count working and drop sets")
    }

    // MARK: - Workout.duration

    func testDuration_bothTimesSet() {
        let workout = Workout()
        workout.startTime = Date()
        workout.endTime = Date().addingTimeInterval(3600) // 1 hour
        XCTAssertEqual(workout.duration!, 3600, accuracy: 1)
    }

    func testDuration_noStartTime_returnsNil() {
        let workout = Workout()
        workout.endTime = Date()
        XCTAssertNil(workout.duration)
    }

    func testDuration_noEndTime_returnsNil() {
        let workout = Workout()
        workout.startTime = Date()
        XCTAssertNil(workout.duration)
    }

    func testDuration_bothNil_returnsNil() {
        let workout = Workout()
        XCTAssertNil(workout.duration)
    }

    // MARK: - Exercise.restTime

    func testRestTime_compoundDefault() {
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        XCTAssertEqual(exercise.restTime, 180)
    }

    func testRestTime_isolationDefault() {
        let exercise = Exercise(name: "Curl", category: .isolation, equipment: .dumbbell)
        XCTAssertEqual(exercise.restTime, 90)
    }

    func testRestTime_accessoryDefault() {
        let exercise = Exercise(name: "Face Pull", category: .accessory, equipment: .cable)
        XCTAssertEqual(exercise.restTime, 60)
    }

    func testRestTime_customOverride() {
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell, defaultRestSeconds: 300)
        XCTAssertEqual(exercise.restTime, 300, "Custom rest should override category default")
    }

    // MARK: - UserProfile Defaults

    func testUserProfile_defaultValues() {
        let profile = UserProfile()
        XCTAssertEqual(profile.unitSystem, .metric)
        XCTAssertEqual(profile.experienceLevel, .intermediate)
        XCTAssertEqual(profile.defaultRestSeconds, 120)
    }

    // MARK: - BodyweightEntry.weightInUnit

    func testBodyweightInUnit_metric() {
        let entry = BodyweightEntry(date: Date(), weightKg: 80)
        XCTAssertEqual(entry.weightInUnit(.metric), 80)
    }

    func testBodyweightInUnit_imperial() {
        let entry = BodyweightEntry(date: Date(), weightKg: 80)
        XCTAssertEqual(entry.weightInUnit(.imperial), 176.37, accuracy: 0.1)
    }

    // MARK: - ChatMessage

    func testChatMessage_defaultNotStreaming() {
        let msg = ChatMessage(role: .user, content: "Hello")
        XCTAssertFalse(msg.isStreaming)
    }

    func testChatMessage_streamingFlag() {
        let msg = ChatMessage(role: .assistant, content: "Thinking...", isStreaming: true)
        XCTAssertTrue(msg.isStreaming)
    }

    func testChatMessage_roles() {
        XCTAssertEqual(MessageRole.system.rawValue, "system")
        XCTAssertEqual(MessageRole.user.rawValue, "user")
        XCTAssertEqual(MessageRole.assistant.rawValue, "assistant")
    }

    // MARK: - SetType Raw Values

    func testSetType_rawValues() {
        XCTAssertEqual(SetType.warmup.rawValue, "warmup")
        XCTAssertEqual(SetType.working.rawValue, "working")
        XCTAssertEqual(SetType.drop.rawValue, "drop")
        XCTAssertEqual(SetType.backoff.rawValue, "backoff")
        XCTAssertEqual(SetType.amrap.rawValue, "amrap")
    }

    // MARK: - ExerciseCategory Raw Values

    func testExerciseCategory_rawValues() {
        XCTAssertEqual(ExerciseCategory.compound.rawValue, "compound")
        XCTAssertEqual(ExerciseCategory.isolation.rawValue, "isolation")
        XCTAssertEqual(ExerciseCategory.accessory.rawValue, "accessory")
    }

    // MARK: - Equipment Raw Values

    func testEquipment_allCases() {
        let expected = ["barbell", "dumbbell", "kettlebell", "machine", "cable", "bodyweight", "band", "other"]
        let actual = [
            Equipment.barbell, .dumbbell, .kettlebell, .machine,
            .cable, .bodyweight, .band, .other
        ].map(\.rawValue)
        XCTAssertEqual(actual, expected)
    }

    // MARK: - PlateauAnalysis

    func testPlateauAnalysis_isPlateaued_aboveThreshold() {
        let analysis = PlateauAnalysis(
            exerciseId: UUID(),
            exerciseName: "Bench",
            compositeScore: 0.7,
            forecastScore: 0.8,
            changePointScore: 0.6,
            rollingAverageScore: 0.7,
            progressState: .plateaued
        )
        XCTAssertTrue(analysis.isPlateaued)
    }

    func testPlateauAnalysis_isPlateaued_belowThreshold() {
        let analysis = PlateauAnalysis(
            exerciseId: UUID(),
            exerciseName: "Bench",
            compositeScore: 0.3,
            forecastScore: 0.2,
            changePointScore: 0.4,
            rollingAverageScore: 0.3,
            progressState: .stalling
        )
        XCTAssertFalse(analysis.isPlateaued)
    }

    func testPlateauAnalysis_progressStateProperty() {
        let analysis = PlateauAnalysis(
            exerciseId: UUID(),
            exerciseName: "Squat",
            compositeScore: 0.5,
            forecastScore: 0.5,
            changePointScore: 0.5,
            rollingAverageScore: 0.5,
            progressState: .recovering
        )
        XCTAssertEqual(analysis.progressState, .recovering)
        analysis.progressState = .progressing
        XCTAssertEqual(analysis.progressStateRaw, "progressing")
    }

    // MARK: - TimeWindow

    func testTimeWindow_days() {
        XCTAssertEqual(TimeWindow.oneWeek.days, 7)
        XCTAssertEqual(TimeWindow.oneMonth.days, 30)
        XCTAssertEqual(TimeWindow.threeMonths.days, 90)
        XCTAssertEqual(TimeWindow.sixMonths.days, 180)
        XCTAssertEqual(TimeWindow.oneYear.days, 365)
        XCTAssertNil(TimeWindow.allTime.days)
    }

    func testTimeWindow_startDate_allTime_returnsNil() {
        XCTAssertNil(TimeWindow.allTime.startDate())
    }

    func testTimeWindow_startDate_oneWeek_isSevenDaysAgo() {
        let start = TimeWindow.oneWeek.startDate()!
        let expected = Calendar.current.date(byAdding: .day, value: -7, to: Date())!
        let diff = abs(start.timeIntervalSince(expected))
        XCTAssertLessThan(diff, 2, "Start date should be approximately 7 days ago")
    }

    // MARK: - ProgressState

    func testProgressState_rawValues() {
        XCTAssertEqual(ProgressState.progressing.rawValue, "progressing")
        XCTAssertEqual(ProgressState.stalling.rawValue, "stalling")
        XCTAssertEqual(ProgressState.plateaued.rawValue, "plateaued")
        XCTAssertEqual(ProgressState.recovering.rawValue, "recovering")
    }
}
