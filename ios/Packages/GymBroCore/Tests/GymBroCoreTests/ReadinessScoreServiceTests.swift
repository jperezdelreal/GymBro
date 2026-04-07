import XCTest
@testable import GymBroCore

final class ReadinessScoreServiceTests: XCTestCase {

    private var service: ReadinessScoreService!

    override func setUp() {
        super.setUp()
        service = ReadinessScoreService()
    }

    // MARK: - Full Calculation

    func testFullInputProducesValidScore() {
        let input = makeFullInput()
        let result = service.calculate(from: input)

        XCTAssertGreaterThanOrEqual(result.overallScore, 0)
        XCTAssertLessThanOrEqual(result.overallScore, 100)
        XCTAssertFalse(result.recommendation.isEmpty)
    }

    func testExcellentRecoveryScoresHigh() {
        let input = ReadinessScoreService.Input(
            sleepRecord: SleepRecord(
                date: Date(),
                totalMinutes: 480,
                stages: SleepStageBreakdown(
                    inBedMinutes: 500,
                    asleepMinutes: 0,
                    awakeMinutes: 20,
                    remMinutes: 100,
                    deepMinutes: 90,
                    coreMinutes: 290
                )
            ),
            recentSleepDurations: [470, 480, 475, 480, 470, 485, 480],
            currentHRV: 55.0,
            hrvBaseline: HealthBaseline(
                type: .heartRateVariability,
                averageValue: 40.0,
                standardDeviation: 5.0,
                sampleCount: 30
            ),
            currentRestingHR: 50.0,
            rhrBaseline: HealthBaseline(
                type: .restingHeartRate,
                averageValue: 58.0,
                standardDeviation: 3.0,
                sampleCount: 30
            ),
            dailyTrainingVolumes: Array(repeating: 5000.0, count: 28)
        )

        let result = service.calculate(from: input)
        XCTAssertGreaterThanOrEqual(result.overallScore, 80)
        XCTAssertTrue(
            result.label == .excellent || result.label == .good,
            "Expected excellent or good, got \(result.label.rawValue) with score \(result.overallScore)"
        )
    }

    func testPoorRecoveryScoresLow() {
        let input = ReadinessScoreService.Input(
            sleepRecord: SleepRecord(
                date: Date(),
                totalMinutes: 180,
                stages: SleepStageBreakdown(
                    inBedMinutes: 300,
                    asleepMinutes: 100,
                    awakeMinutes: 120,
                    remMinutes: 20,
                    deepMinutes: 10,
                    coreMinutes: 50
                )
            ),
            recentSleepDurations: [180, 300, 200, 420, 180, 250, 300],
            currentHRV: 20.0,
            hrvBaseline: HealthBaseline(
                type: .heartRateVariability,
                averageValue: 40.0,
                standardDeviation: 5.0,
                sampleCount: 30
            ),
            currentRestingHR: 72.0,
            rhrBaseline: HealthBaseline(
                type: .restingHeartRate,
                averageValue: 58.0,
                standardDeviation: 3.0,
                sampleCount: 30
            ),
            dailyTrainingVolumes: [2000, 2000, 2000, 2000, 10000, 12000, 15000]
        )

        let result = service.calculate(from: input)
        XCTAssertLessThan(result.overallScore, 60)
        XCTAssertTrue(
            result.label == .poor || result.label == .moderate,
            "Expected poor or moderate, got \(result.label.rawValue)"
        )
    }

    // MARK: - Missing Data Handling

    func testEmptyInputReturnsNeutralScore() {
        let input = ReadinessScoreService.Input()
        let result = service.calculate(from: input)

        XCTAssertEqual(result.overallScore, 50.0)
        XCTAssertEqual(result.sleepScore, 0)
        XCTAssertEqual(result.hrvScore, 0)
        XCTAssertEqual(result.restingHRScore, 0)
        XCTAssertEqual(result.trainingLoadScore, 0)
        XCTAssertNil(result.subjectiveScore)
    }

    func testSleepOnlyInputUsesFullWeight() {
        let input = ReadinessScoreService.Input(
            sleepRecord: SleepRecord(
                date: Date(),
                totalMinutes: 480,
                stages: SleepStageBreakdown(asleepMinutes: 480)
            )
        )

        let result = service.calculate(from: input)
        XCTAssertGreaterThan(result.overallScore, 0)
        XCTAssertGreaterThan(result.sleepScore, 0)
        // Other factors should be 0 since not provided
        XCTAssertEqual(result.hrvScore, 0)
        XCTAssertEqual(result.restingHRScore, 0)
    }

    func testWeightRedistribution() {
        // With only HRV and RHR available, their weights should redistribute to sum to 1.0
        let baseline = HealthBaseline(
            type: .heartRateVariability,
            averageValue: 40.0,
            standardDeviation: 5.0,
            sampleCount: 30
        )

        let input1 = ReadinessScoreService.Input(
            currentHRV: 40.0,
            hrvBaseline: baseline
        )

        let result1 = service.calculate(from: input1)
        // With only HRV at baseline (z=0 → score=70), overall should be ~70
        XCTAssertEqual(result1.overallScore, 70.0, accuracy: 1.0)
    }

    // MARK: - Individual Factor Scoring

    func testHRVScoringAboveBaseline() {
        let baseline = HealthBaseline(
            type: .heartRateVariability,
            averageValue: 40.0,
            standardDeviation: 5.0,
            sampleCount: 30
        )

        let score = service.scoreHRV(current: 50.0, baseline: baseline)
        // z = 2.0, score = 70 + 2*15 = 100
        XCTAssertEqual(score, 100.0, accuracy: 1.0)
    }

    func testHRVScoringBelowBaseline() {
        let baseline = HealthBaseline(
            type: .heartRateVariability,
            averageValue: 40.0,
            standardDeviation: 5.0,
            sampleCount: 30
        )

        let score = service.scoreHRV(current: 30.0, baseline: baseline)
        // z = -2.0, score = 70 + (-2)*15 = 40
        XCTAssertEqual(score, 40.0, accuracy: 1.0)
    }

    func testHRVScoringAtBaseline() {
        let baseline = HealthBaseline(
            type: .heartRateVariability,
            averageValue: 40.0,
            standardDeviation: 5.0,
            sampleCount: 30
        )

        let score = service.scoreHRV(current: 40.0, baseline: baseline)
        XCTAssertEqual(score, 70.0, accuracy: 0.1)
    }

    func testRestingHRScoringBelowBaseline() {
        let baseline = HealthBaseline(
            type: .restingHeartRate,
            averageValue: 60.0,
            standardDeviation: 3.0,
            sampleCount: 30
        )

        // Lower RHR = better recovery
        let score = service.scoreRestingHR(current: 54.0, baseline: baseline)
        // z = -2.0, score = 70 - (-2)*15 = 100
        XCTAssertEqual(score, 100.0, accuracy: 1.0)
    }

    func testRestingHRScoringAboveBaseline() {
        let baseline = HealthBaseline(
            type: .restingHeartRate,
            averageValue: 60.0,
            standardDeviation: 3.0,
            sampleCount: 30
        )

        // Higher RHR = worse recovery
        let score = service.scoreRestingHR(current: 66.0, baseline: baseline)
        // z = 2.0, score = 70 - 2*15 = 40
        XCTAssertEqual(score, 40.0, accuracy: 1.0)
    }

    // MARK: - Readiness Labels

    func testReadinessLabelThresholds() {
        XCTAssertEqual(ReadinessLabel.from(score: 95), .excellent)
        XCTAssertEqual(ReadinessLabel.from(score: 90), .excellent)
        XCTAssertEqual(ReadinessLabel.from(score: 89), .good)
        XCTAssertEqual(ReadinessLabel.from(score: 70), .good)
        XCTAssertEqual(ReadinessLabel.from(score: 69), .moderate)
        XCTAssertEqual(ReadinessLabel.from(score: 50), .moderate)
        XCTAssertEqual(ReadinessLabel.from(score: 49), .poor)
        XCTAssertEqual(ReadinessLabel.from(score: 10), .poor)
        XCTAssertEqual(ReadinessLabel.from(score: 0), .poor)
    }

    func testReadinessLabelDisplayNames() {
        XCTAssertEqual(ReadinessLabel.excellent.displayName, "Excellent")
        XCTAssertEqual(ReadinessLabel.good.displayName, "Good")
        XCTAssertEqual(ReadinessLabel.moderate.displayName, "Moderate")
        XCTAssertEqual(ReadinessLabel.poor.displayName, "Poor")
    }

    // MARK: - Recommendations

    func testRecommendationsNotEmpty() {
        for label in ReadinessLabel.allCases {
            let rec = ReadinessScoreService.recommendation(for: label)
            XCTAssertFalse(rec.isEmpty, "Recommendation for \(label.rawValue) should not be empty")
        }
    }

    func testExcellentRecommendationContainsGoHeavy() {
        let rec = ReadinessScoreService.recommendation(for: .excellent)
        XCTAssertTrue(rec.lowercased().contains("heavy") || rec.lowercased().contains("push"))
    }

    func testPoorRecommendationContainsRest() {
        let rec = ReadinessScoreService.recommendation(for: .poor)
        XCTAssertTrue(rec.lowercased().contains("rest"))
    }

    // MARK: - Subjective Check-In

    func testSubjectiveCheckInNormalization() {
        // Best case: energy 5, soreness 1, motivation 5
        let bestCase = SubjectiveCheckIn(date: Date(), energy: 5, soreness: 1, motivation: 5)
        XCTAssertEqual(bestCase.normalizedScore, 100.0, accuracy: 0.1)

        // Worst case: energy 1, soreness 5, motivation 1
        let worstCase = SubjectiveCheckIn(date: Date(), energy: 1, soreness: 5, motivation: 1)
        XCTAssertEqual(worstCase.normalizedScore, 0.0, accuracy: 0.1)

        // Middle: energy 3, soreness 3, motivation 3
        let mid = SubjectiveCheckIn(date: Date(), energy: 3, soreness: 3, motivation: 3)
        XCTAssertEqual(mid.normalizedScore, 50.0, accuracy: 1.0)
    }

    func testSubjectiveCheckInClampsValues() {
        let clamped = SubjectiveCheckIn(date: Date(), energy: 10, soreness: -1, motivation: 7)
        // Should clamp to 5, 1, 5
        let bestCase = SubjectiveCheckIn(date: Date(), energy: 5, soreness: 1, motivation: 5)
        XCTAssertEqual(clamped.normalizedScore, bestCase.normalizedScore, accuracy: 0.1)
    }

    func testSubjectiveFactorIncluded() {
        let checkIn = SubjectiveCheckIn(date: Date(), energy: 5, soreness: 1, motivation: 5)

        let input = ReadinessScoreService.Input(subjectiveCheckIn: checkIn)
        let result = service.calculate(from: input)

        XCTAssertNotNil(result.subjectiveScore)
        XCTAssertEqual(result.subjectiveScore!, 100.0, accuracy: 0.1)
    }

    // MARK: - Score Bounds

    func testScoreAlwaysClamped() {
        // Extreme inputs shouldn't produce scores outside 0-100
        let extremeInput = ReadinessScoreService.Input(
            currentHRV: 100.0,
            hrvBaseline: HealthBaseline(
                type: .heartRateVariability,
                averageValue: 10.0,
                standardDeviation: 1.0,
                sampleCount: 30
            )
        )

        let result = service.calculate(from: extremeInput)
        XCTAssertLessThanOrEqual(result.overallScore, 100)
        XCTAssertGreaterThanOrEqual(result.overallScore, 0)
    }

    // MARK: - Helpers

    private func makeFullInput() -> ReadinessScoreService.Input {
        ReadinessScoreService.Input(
            sleepRecord: SleepRecord(
                date: Date(),
                totalMinutes: 420,
                stages: SleepStageBreakdown(
                    inBedMinutes: 450,
                    asleepMinutes: 50,
                    awakeMinutes: 30,
                    remMinutes: 80,
                    deepMinutes: 60,
                    coreMinutes: 230
                )
            ),
            recentSleepDurations: [420, 400, 440, 410, 430, 420, 415],
            currentHRV: 42.0,
            hrvBaseline: HealthBaseline(
                type: .heartRateVariability,
                averageValue: 40.0,
                standardDeviation: 5.0,
                sampleCount: 30
            ),
            currentRestingHR: 57.0,
            rhrBaseline: HealthBaseline(
                type: .restingHeartRate,
                averageValue: 58.0,
                standardDeviation: 3.0,
                sampleCount: 30
            ),
            dailyTrainingVolumes: [5000, 5500, 4800, 5200, 5100, 4900, 5300],
            subjectiveCheckIn: SubjectiveCheckIn(
                date: Date(), energy: 4, soreness: 2, motivation: 4
            )
        )
    }
}
