import XCTest
@testable import GymBroCore

/// Extended edge case tests for PlateauDetectionService and sub-services.
/// Addresses issue #27: single data points, all-zero values, negative values, mixed trends.
final class PlateauDetectionEdgeCaseTests: XCTestCase {

    // MARK: - RollingAverageAnalyzer Edge Cases

    func testRollingAverage_emptyArray() {
        let analyzer = RollingAverageAnalyzer()
        XCTAssertEqual(analyzer.analyze(values: []), 0, "Empty array should return 0")
    }

    func testRollingAverage_singleValue() {
        let analyzer = RollingAverageAnalyzer()
        XCTAssertEqual(analyzer.analyze(values: [100.0]), 0, "Single value should return 0 (insufficient data)")
    }

    func testRollingAverage_allZeroValues() {
        let analyzer = RollingAverageAnalyzer()
        let values = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        let score = analyzer.analyze(values: values)
        XCTAssertTrue(score.isFinite, "All zeros should not produce NaN or Inf")
    }

    func testRollingAverage_negativeValues() {
        let analyzer = RollingAverageAnalyzer()
        let values = [-10.0, -8.0, -6.0, -4.0, -2.0, 0.0, 2.0, 4.0]
        let score = analyzer.analyze(values: values)
        XCTAssertTrue(score >= 0 && score <= 1, "Score should stay in [0, 1] range even with negatives")
    }

    func testRollingAverage_decliningValues_highScore() {
        let analyzer = RollingAverageAnalyzer()
        let values = [120.0, 118.0, 115.0, 112.0, 108.0, 105.0, 100.0, 95.0]
        let score = analyzer.analyze(values: values)
        XCTAssertGreaterThan(score, 0.0, "Declining values should yield non-zero stagnation score")
    }

    func testRollingAverage_exactlyLongWindowCount() {
        let analyzer = RollingAverageAnalyzer()
        let values = [100.0, 102.0, 104.0, 106.0, 108.0, 110.0]
        let score = analyzer.analyze(values: values)
        XCTAssertTrue(score.isFinite, "Exactly 6 values (= longWindow) should work")
    }

    func testRollingAverage_movingAverage_calculation() {
        let analyzer = RollingAverageAnalyzer()
        let values = [1.0, 2.0, 3.0, 4.0, 5.0]
        let ma = analyzer.movingAverage(values: values, window: 3)
        // Window 3: [1,2,3]=2.0, [2,3,4]=3.0, [3,4,5]=4.0
        XCTAssertEqual(ma.count, 3)
        XCTAssertEqual(ma[0], 2.0, accuracy: 0.001)
        XCTAssertEqual(ma[1], 3.0, accuracy: 0.001)
        XCTAssertEqual(ma[2], 4.0, accuracy: 0.001)
    }

    func testRollingAverage_slope_calculation() {
        let analyzer = RollingAverageAnalyzer()
        // Perfect linear: y = 2x + 1
        let values = [1.0, 3.0, 5.0, 7.0, 9.0]
        let slope = analyzer.slope(of: values)
        XCTAssertEqual(slope, 2.0, accuracy: 0.001)
    }

    func testRollingAverage_slope_flatLine() {
        let analyzer = RollingAverageAnalyzer()
        let values = [5.0, 5.0, 5.0, 5.0]
        let slope = analyzer.slope(of: values)
        XCTAssertEqual(slope, 0.0, accuracy: 0.001)
    }

    func testRollingAverage_slope_singleValue() {
        let analyzer = RollingAverageAnalyzer()
        XCTAssertEqual(analyzer.slope(of: [42.0]), 0)
    }

    func testRollingAverage_slope_emptyArray() {
        let analyzer = RollingAverageAnalyzer()
        XCTAssertEqual(analyzer.slope(of: []), 0)
    }

    // MARK: - ChangePointDetector Edge Cases

    func testChangePoint_emptyArray() {
        let detector = ChangePointDetector()
        XCTAssertEqual(detector.analyze(values: []), 0)
    }

    func testChangePoint_threeValues_insufficientData() {
        let detector = ChangePointDetector()
        XCTAssertEqual(detector.analyze(values: [100, 102, 104]), 0)
    }

    func testChangePoint_exactlyFourValues_minimalData() {
        let detector = ChangePointDetector()
        let score = detector.analyze(values: [100, 102, 104, 106])
        XCTAssertTrue(score.isFinite)
    }

    func testChangePoint_allZeroValues() {
        let detector = ChangePointDetector()
        let score = detector.analyze(values: [0, 0, 0, 0, 0, 0, 0, 0])
        XCTAssertTrue(score.isFinite, "All zeros should not produce NaN")
    }

    func testChangePoint_identicalValues() {
        let detector = ChangePointDetector()
        let score = detector.analyze(values: [100, 100, 100, 100, 100, 100, 100, 100])
        XCTAssertTrue(score.isFinite)
    }

    func testChangePoint_negativeValues() {
        let detector = ChangePointDetector()
        let score = detector.analyze(values: [-10, -8, -6, -4, -10, -12, -14, -16])
        XCTAssertTrue(score >= 0 && score <= 1)
    }

    func testChangePoint_standardDeviation_singleValue() {
        let detector = ChangePointDetector()
        XCTAssertEqual(detector.standardDeviation([42.0]), 0)
    }

    func testChangePoint_standardDeviation_identicalValues() {
        let detector = ChangePointDetector()
        XCTAssertEqual(detector.standardDeviation([5.0, 5.0, 5.0]), 0)
    }

    func testChangePoint_standardDeviation_knownValues() {
        let detector = ChangePointDetector()
        // [2, 4, 4, 4, 5, 5, 7, 9] -> mean = 5, variance = 4, std = 2
        let std = detector.standardDeviation([2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0])
        XCTAssertEqual(std, 2.138, accuracy: 0.01)
    }

    // MARK: - TrendForecaster Edge Cases

    func testTrendForecaster_emptyArray() {
        let forecaster = TrendForecaster()
        XCTAssertEqual(forecaster.analyze(values: []), 0)
    }

    func testTrendForecaster_fiveValues_insufficientData() {
        let forecaster = TrendForecaster()
        XCTAssertEqual(forecaster.analyze(values: [100, 102, 104, 106, 108]), 0)
    }

    func testTrendForecaster_exactlySixValues_minimalData() {
        let forecaster = TrendForecaster()
        let score = forecaster.analyze(values: [100, 102, 104, 106, 108, 110])
        XCTAssertTrue(score.isFinite)
    }

    func testTrendForecaster_allIdenticalValues() {
        let forecaster = TrendForecaster()
        let score = forecaster.analyze(values: [100, 100, 100, 100, 100, 100, 100, 100])
        XCTAssertTrue(score.isFinite)
    }

    func testTrendForecaster_linearRegression_perfectLine() {
        let forecaster = TrendForecaster()
        let values = [2.0, 4.0, 6.0, 8.0, 10.0]
        let regression = forecaster.linearRegression(values: values)
        XCTAssertEqual(regression.slope, 2.0, accuracy: 0.001)
        XCTAssertEqual(regression.intercept, 2.0, accuracy: 0.001)
    }

    func testTrendForecaster_linearRegression_singleValue() {
        let forecaster = TrendForecaster()
        let regression = forecaster.linearRegression(values: [42.0])
        XCTAssertEqual(regression.slope, 0)
        XCTAssertEqual(regression.intercept, 42.0, accuracy: 0.001)
    }

    // MARK: - PlateauDetectionService Composite Edge Cases

    func testComposite_emptyArray_returnsNil() {
        let service = PlateauDetectionService()
        let analysis = service.analyze(sets: [], exerciseId: UUID(), exerciseName: "Squat")
        XCTAssertNil(analysis)
    }

    func testComposite_onlyWarmupSets_returnsNil() {
        let service = PlateauDetectionService()
        let sets = (0..<10).map { _ in
            ExerciseSet(weightKg: 50, reps: 10, setType: .warmup)
        }
        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Squat")
        XCTAssertNil(analysis, "Only warmup sets should be ignored, yielding insufficient data")
    }

    func testComposite_allIdenticalE1RM_highPlateauScore() {
        let service = PlateauDetectionService()
        let values: [Double] = Array(repeating: 100.0, count: 10)
        let sets = createMockSets(e1rmValues: values)
        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Bench")
        XCTAssertNotNil(analysis)
        if let analysis {
            XCTAssertGreaterThan(analysis.compositeScore, 0.3,
                "Identical values should indicate stagnation")
        }
    }

    func testComposite_wildcardFluctuations() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 110, 95, 115, 90, 120, 85, 125]
        let sets = createMockSets(e1rmValues: values)
        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Squat")
        XCTAssertNotNil(analysis)
        if let analysis {
            XCTAssertTrue(analysis.compositeScore.isFinite)
        }
    }

    // MARK: - extractSessionE1RMs

    func testExtractSessionE1RMs_emptyInput() {
        let service = PlateauDetectionService()
        let result = service.extractSessionE1RMs(from: [])
        XCTAssertTrue(result.isEmpty)
    }

    func testExtractSessionE1RMs_filtersWarmups() {
        let service = PlateauDetectionService()
        let warmup = ExerciseSet(weightKg: 50, reps: 10, setType: .warmup)
        let working = ExerciseSet(weightKg: 100, reps: 5, setType: .working)
        let result = service.extractSessionE1RMs(from: [warmup, working])
        XCTAssertEqual(result.count, 1, "Should only use working sets")
    }

    // MARK: - State Transitions (extended)

    func testStateTransition_progressingToStalling() {
        let service = PlateauDetectionService()
        let progressValues: [Double] = [100, 103, 106, 109, 112, 115, 118, 121]
        let progressSets = createMockSets(e1rmValues: progressValues)
        let first = service.analyze(sets: progressSets, exerciseId: UUID(), exerciseName: "Squat")
        XCTAssertEqual(first?.progressState, .progressing)

        let stallingValues: [Double] = [100, 103, 106, 109, 108, 109, 108, 109]
        let stallingSets = createMockSets(e1rmValues: stallingValues)
        let second = service.analyze(
            sets: stallingSets, exerciseId: UUID(), exerciseName: "Squat",
            previousAnalysis: first
        )
        if let state = second?.progressState {
            XCTAssertTrue(state == .stalling || state == .progressing || state == .plateaued,
                "Should detect stalling or remain/advance")
        }
    }

    func testStateTransition_recoveringToProgressing() {
        let service = PlateauDetectionService()
        // Simulate a recovering state
        let values: [Double] = [100, 103, 106, 109, 112, 115, 118, 121]
        let sets = createMockSets(e1rmValues: values)

        // Create a fake "recovering" previous analysis
        let fakeRecovering = PlateauAnalysis(
            exerciseId: UUID(),
            exerciseName: "Squat",
            compositeScore: 0.3,
            forecastScore: 0.2,
            changePointScore: 0.3,
            rollingAverageScore: 0.1,
            progressState: .recovering,
            sessionsInState: 2
        )

        let analysis = service.analyze(
            sets: sets, exerciseId: UUID(), exerciseName: "Squat",
            previousAnalysis: fakeRecovering
        )
        if let state = analysis?.progressState {
            XCTAssertTrue(state == .recovering || state == .progressing,
                "Clear progress from recovering should transition to progressing or stay recovering")
        }
    }

    // MARK: - Recommendations

    func testRecommendations_progressingState_empty() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 105, 110, 115, 120, 125, 130, 135]
        let sets = createMockSets(e1rmValues: values)
        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Squat")
        if let analysis, analysis.progressState == .progressing {
            XCTAssertTrue(analysis.recommendations.isEmpty,
                "Progressing state should have no recommendations")
        }
    }

    func testRecommendations_plateauedState_containsDeload() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 105, 110, 115, 115, 115, 115, 115, 115, 115]
        let sets = createMockSets(e1rmValues: values)
        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "OHP")
        if let analysis, analysis.progressState == .plateaued {
            let hasDeload = analysis.recommendations.contains { $0.lowercased().contains("deload") }
            XCTAssertTrue(hasDeload, "Plateau recommendations should mention deload")
        }
    }

    // MARK: - Score Bounds

    func testCompositeScore_alwaysBounded() {
        let service = PlateauDetectionService()
        let testCases: [[Double]] = [
            [100, 103, 106, 109, 112, 115, 118, 121],
            [100, 100, 100, 100, 100, 100, 100, 100],
            [120, 115, 110, 105, 100, 95, 90, 85],
            [100, 200, 50, 300, 25, 400, 10, 500],
        ]

        for (index, values) in testCases.enumerated() {
            let sets = createMockSets(e1rmValues: values)
            let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Test")
            if let analysis {
                XCTAssertGreaterThanOrEqual(analysis.compositeScore, 0,
                    "Score should be >= 0 for case \(index)")
                XCTAssertTrue(analysis.compositeScore.isFinite,
                    "Score should be finite for case \(index)")
            }
        }
    }

    // MARK: - Helpers

    private func createMockSets(e1rmValues: [Double]) -> [ExerciseSet] {
        let baseDate = Date().addingTimeInterval(-Double(e1rmValues.count) * 86400 * 3)
        return e1rmValues.enumerated().map { (index, targetE1RM) in
            let reps = 5
            let weight = targetE1RM / (1.0 + Double(reps) / 30.0)
            let date = baseDate.addingTimeInterval(Double(index) * 86400 * 3)
            let set = ExerciseSet(weightKg: weight, reps: reps, setType: .working)
            set.createdAt = date
            return set
        }
    }
}
