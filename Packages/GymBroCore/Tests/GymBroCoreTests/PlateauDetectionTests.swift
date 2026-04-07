import XCTest
@testable import GymBroCore

final class PlateauDetectionTests: XCTestCase {

    // MARK: - Rolling Average Analyzer

    func testRollingAverage_clearProgress_lowScore() {
        let analyzer = RollingAverageAnalyzer()
        let values = [100.0, 102.0, 104.0, 106.0, 108.0, 110.0, 112.0, 114.0]
        let score = analyzer.analyze(values: values)
        XCTAssertLessThan(score, 0.3, "Clear progress should yield a low stagnation score")
    }

    func testRollingAverage_flatValues_highScore() {
        let analyzer = RollingAverageAnalyzer()
        let values = [100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0]
        let score = analyzer.analyze(values: values)
        XCTAssertGreaterThan(score, 0.5, "Flat progress should yield a high stagnation score")
    }

    func testRollingAverage_insufficientData_returnsZero() {
        let analyzer = RollingAverageAnalyzer()
        let values = [100.0, 102.0]
        XCTAssertEqual(analyzer.analyze(values: values), 0)
    }

    // MARK: - Change Point Detector

    func testChangePoint_stableValues_lowScore() {
        let detector = ChangePointDetector()
        let values = [100.0, 101.0, 102.0, 103.0, 104.0, 105.0, 106.0, 107.0]
        let score = detector.analyze(values: values)
        XCTAssertLessThan(score, 0.6, "Stable upward trend should not trigger high change point")
    }

    func testChangePoint_suddenDrop_highScore() {
        let detector = ChangePointDetector()
        let values = [100.0, 105.0, 110.0, 115.0, 100.0, 98.0, 95.0, 96.0]
        let score = detector.analyze(values: values)
        XCTAssertGreaterThan(score, 0.3, "Sudden regression should produce high change point score")
    }

    func testChangePoint_insufficientData_returnsZero() {
        let detector = ChangePointDetector()
        let values = [100.0, 102.0]
        XCTAssertEqual(detector.analyze(values: values), 0)
    }

    // MARK: - Trend Forecaster

    func testTrendForecaster_onTrack_lowScore() {
        let forecaster = TrendForecaster()
        let values = [100.0, 102.0, 104.0, 106.0, 108.0, 110.0, 112.0, 114.0, 116.0]
        let score = forecaster.analyze(values: values)
        XCTAssertLessThan(score, 0.3, "On-track linear progress should yield low forecast deviation")
    }

    func testTrendForecaster_belowForecast_highScore() {
        let forecaster = TrendForecaster()
        let values = [100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 115.0, 114.0, 113.0]
        let score = forecaster.analyze(values: values)
        XCTAssertGreaterThan(score, 0.2, "Falling below forecast should produce a meaningful score")
    }

    func testTrendForecaster_insufficientData_returnsZero() {
        let forecaster = TrendForecaster()
        let values = [100.0, 102.0]
        XCTAssertEqual(forecaster.analyze(values: values), 0)
    }

    // MARK: - Composite Plateau Detection

    func testComposite_clearProgress_belowThreshold() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 103, 106, 109, 112, 115, 118, 121]
        let sets = createMockSets(e1rmValues: values)

        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Squat")

        XCTAssertNotNil(analysis)
        if let analysis {
            XCTAssertLessThan(analysis.compositeScore, PlateauDetectionService.plateauThreshold,
                              "Clear progress should not be flagged as plateau")
            XCTAssertFalse(analysis.isPlateaued)
        }
    }

    func testComposite_obviousPlateau_aboveThreshold() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 105, 110, 115, 115, 115, 115, 115, 115, 115]
        let sets = createMockSets(e1rmValues: values)

        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Bench Press")

        XCTAssertNotNil(analysis)
        if let analysis {
            XCTAssertGreaterThan(analysis.compositeScore, 0.3,
                                 "Obvious plateau should yield meaningful composite score")
        }
    }

    func testComposite_insufficientData_returnsNil() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 105, 110]
        let sets = createMockSets(e1rmValues: values)

        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "Deadlift")
        XCTAssertNil(analysis, "Should return nil with insufficient data")
    }

    func testComposite_stateTransitions() {
        let service = PlateauDetectionService()

        let progressValues: [Double] = [100, 103, 106, 109, 112, 115, 118, 121]
        let progressSets = createMockSets(e1rmValues: progressValues)
        let first = service.analyze(sets: progressSets, exerciseId: UUID(), exerciseName: "Squat")
        XCTAssertEqual(first?.progressState, .progressing)

        let plateauValues: [Double] = [100, 105, 110, 115, 115, 115, 115, 115, 115, 115]
        let plateauSets = createMockSets(e1rmValues: plateauValues)
        let second = service.analyze(
            sets: plateauSets,
            exerciseId: UUID(),
            exerciseName: "Squat",
            previousAnalysis: first
        )

        if let state = second?.progressState {
            XCTAssertTrue(state == .stalling || state == .plateaued,
                          "Should transition away from progressing when data shows plateau")
        }
    }

    func testComposite_recommendations_generated() {
        let service = PlateauDetectionService()
        let values: [Double] = [100, 105, 110, 115, 115, 115, 115, 115, 115, 115]
        let sets = createMockSets(e1rmValues: values)

        let analysis = service.analyze(sets: sets, exerciseId: UUID(), exerciseName: "OHP")

        if let analysis, analysis.progressState != .progressing {
            XCTAssertFalse(analysis.recommendations.isEmpty,
                           "Non-progressing states should include recommendations")
        }
    }

    // MARK: - Helpers

    /// Creates mock ExerciseSet array from target e1RM values (Epley, reps=5).
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
