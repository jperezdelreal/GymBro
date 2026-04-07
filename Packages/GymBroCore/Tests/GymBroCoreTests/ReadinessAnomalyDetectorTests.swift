import XCTest
@testable import GymBroCore

final class ReadinessAnomalyDetectorTests: XCTestCase {
    var detector: ReadinessAnomalyDetector!
    
    override func setUp() {
        super.setUp()
        detector = ReadinessAnomalyDetector()
    }
    
    override func tearDown() {
        detector = nil
        super.tearDown()
    }
    
    // MARK: - Empty Input Tests
    
    func testEmptyScoresReturnsNoAnomalies() {
        let anomalies = detector.detect(scores: [])
        XCTAssertTrue(anomalies.isEmpty)
    }
    
    func testInsufficientHistoricalDataReturnsNoAnomalies() {
        // Need at least 3 historical days + today
        let scores = [
            makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 1, hrv: 70, rhr: 70, sleep: 70),
        ]
        
        let anomalies = detector.detect(scores: scores)
        XCTAssertTrue(anomalies.isEmpty, "Need at least 3 historical data points")
    }
    
    // MARK: - HRV Drop Detection Tests
    
    func testDetectsHRVDrop20Percent() {
        // Baseline: HRV score ~85 (z ≈ +1)
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 85, rhr: 70, sleep: 70),
        ]
        
        // Today: HRV score drops to ~55 (z ≈ -1, ~20-25% drop)
        let today = makeReadinessScore(daysAgo: 0, hrv: 55, rhr: 70, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertFalse(anomalies.isEmpty, "Should detect HRV drop")
        XCTAssertTrue(anomalies.contains { $0.type == .hrvDrop })
        
        let hrvAnomaly = anomalies.first { $0.type == .hrvDrop }
        XCTAssertNotNil(hrvAnomaly)
        XCTAssertTrue(
            hrvAnomaly?.severity == .medium || hrvAnomaly?.severity == .high,
            "HRV drop should have medium or high severity"
        )
    }
    
    func testNoHRVAnomalyWhenWithinThreshold() {
        // Baseline: HRV score 70
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 70, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 72, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 68, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 71, rhr: 70, sleep: 70),
        ]
        
        // Today: HRV score 65 (small drop, but <20%)
        let today = makeReadinessScore(daysAgo: 0, hrv: 65, rhr: 70, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertFalse(anomalies.contains { $0.type == .hrvDrop }, "Small HRV variation should not trigger anomaly")
    }
    
    func testHRVAnomalySeverityScalesWithDrop() {
        // Baseline: HRV score 85
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 85, rhr: 70, sleep: 70),
        ]
        
        // Today: HRV score 40 (very large drop, >30%)
        let today = makeReadinessScore(daysAgo: 0, hrv: 40, rhr: 70, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        let hrvAnomaly = anomalies.first { $0.type == .hrvDrop }
        XCTAssertEqual(hrvAnomaly?.severity, .high, "Very large HRV drop should be high severity")
    }
    
    // MARK: - Resting HR Spike Detection Tests
    
    func testDetectsRHRSpike10Percent() {
        // Baseline: RHR score ~85 (z ≈ -1, better than average)
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 70, rhr: 85, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 70, rhr: 85, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 70, rhr: 85, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 70, rhr: 85, sleep: 70),
        ]
        
        // Today: RHR score drops to 55 (z ≈ +1, worse, >10% spike in actual HR)
        let today = makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 55, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertTrue(anomalies.contains { $0.type == .rhrSpike }, "Should detect RHR spike")
        
        let rhrAnomaly = anomalies.first { $0.type == .rhrSpike }
        XCTAssertNotNil(rhrAnomaly)
        XCTAssertTrue(
            rhrAnomaly?.severity == .medium || rhrAnomaly?.severity == .high
        )
    }
    
    func testNoRHRAnomalyWhenWithinThreshold() {
        // Baseline: RHR score 70
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 70, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 70, rhr: 72, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 70, rhr: 68, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 70, rhr: 71, sleep: 70),
        ]
        
        // Today: RHR score 65 (small change)
        let today = makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 65, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertFalse(anomalies.contains { $0.type == .rhrSpike })
    }
    
    // MARK: - Sleep Drop Detection Tests
    
    func testDetectsSleepDrop() {
        // Baseline: good sleep (score 80)
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 70, rhr: 70, sleep: 80),
            makeReadinessScore(daysAgo: 6, hrv: 70, rhr: 70, sleep: 80),
            makeReadinessScore(daysAgo: 5, hrv: 70, rhr: 70, sleep: 80),
            makeReadinessScore(daysAgo: 4, hrv: 70, rhr: 70, sleep: 80),
        ]
        
        // Today: poor sleep (score 40, drop of 40 points)
        let today = makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 70, sleep: 40)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertTrue(anomalies.contains { $0.type == .sleepDrop })
        
        let sleepAnomaly = anomalies.first { $0.type == .sleepDrop }
        XCTAssertNotNil(sleepAnomaly)
        XCTAssertEqual(sleepAnomaly?.severity, .high, "Large sleep drop should be high severity")
    }
    
    func testNoSleepAnomalyWhenWithinThreshold() {
        // Baseline: sleep score 70
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 70, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 70, rhr: 70, sleep: 72),
            makeReadinessScore(daysAgo: 5, hrv: 70, rhr: 70, sleep: 68),
            makeReadinessScore(daysAgo: 4, hrv: 70, rhr: 70, sleep: 71),
        ]
        
        // Today: sleep score 55 (15 point drop, below 25 threshold)
        let today = makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 70, sleep: 55)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertFalse(anomalies.contains { $0.type == .sleepDrop })
    }
    
    // MARK: - Multi-Factor Decline Tests
    
    func testDetectsMultiFactorDecline() {
        // Baseline: all factors good
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 6, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 5, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 4, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
        ]
        
        // Today: multiple factors declined (HRV, RHR, Sleep all down >15 points)
        let today = makeReadinessScore(daysAgo: 0, hrv: 55, rhr: 55, sleep: 55, trainingLoad: 80)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        XCTAssertTrue(anomalies.contains { $0.type == .multiFactorDecline })
        
        let multiAnomaly = anomalies.first { $0.type == .multiFactorDecline }
        XCTAssertNotNil(multiAnomaly)
        XCTAssertGreaterThanOrEqual(multiAnomaly?.affectedMetrics.count ?? 0, 2)
    }
    
    func testNoMultiFactorAnomalyWithSingleFactorDecline() {
        // Baseline: all factors good
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 6, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 5, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 4, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
        ]
        
        // Today: only HRV declined
        let today = makeReadinessScore(daysAgo: 0, hrv: 55, rhr: 80, sleep: 80, trainingLoad: 80)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        // Should detect HRV drop but NOT multi-factor decline
        XCTAssertFalse(anomalies.contains { $0.type == .multiFactorDecline })
    }
    
    func testMultiFactorSeverityScalesWithFactorCount() {
        // Baseline: all factors good
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 6, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 5, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 4, hrv: 80, rhr: 80, sleep: 80, trainingLoad: 80),
        ]
        
        // Today: all 4 factors declined
        let today = makeReadinessScore(daysAgo: 0, hrv: 55, rhr: 55, sleep: 55, trainingLoad: 55)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        let multiAnomaly = anomalies.first { $0.type == .multiFactorDecline }
        XCTAssertEqual(multiAnomaly?.severity, .high, "3+ factors should be high severity")
        XCTAssertGreaterThanOrEqual(multiAnomaly?.affectedMetrics.count ?? 0, 3)
    }
    
    // MARK: - Multiple Anomalies Tests
    
    func testCanDetectMultipleAnomaliesSimultaneously() {
        // Baseline: all good
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 85, rhr: 85, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 6, hrv: 85, rhr: 85, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 5, hrv: 85, rhr: 85, sleep: 80, trainingLoad: 80),
            makeReadinessScore(daysAgo: 4, hrv: 85, rhr: 85, sleep: 80, trainingLoad: 80),
        ]
        
        // Today: HRV drop, RHR spike, sleep drop all present
        let today = makeReadinessScore(daysAgo: 0, hrv: 50, rhr: 50, sleep: 45, trainingLoad: 80)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        // Should detect individual anomalies plus multi-factor
        XCTAssertGreaterThanOrEqual(anomalies.count, 2)
        
        let types = Set(anomalies.map(\.type))
        XCTAssertTrue(types.contains(.multiFactorDecline))
    }
    
    // MARK: - Edge Cases
    
    func testHandlesMissingHRVData() {
        // Baseline: HRV data missing (score = 0)
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 0, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 0, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 0, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 0, rhr: 70, sleep: 70),
        ]
        
        let today = makeReadinessScore(daysAgo: 0, hrv: 0, rhr: 70, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        // Should not crash, should not detect HRV anomaly
        XCTAssertFalse(anomalies.contains { $0.type == .hrvDrop })
    }
    
    func testHandlesInconsistentDataAvailability() {
        // Mixed data availability
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 70, rhr: 0, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 0, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 70, rhr: 70, sleep: 0),
            makeReadinessScore(daysAgo: 4, hrv: 70, rhr: 70, sleep: 70),
        ]
        
        let today = makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 70, sleep: 70)
        
        // Should not crash with inconsistent data
        XCTAssertNoThrow {
            _ = detector.detect(scores: baseline + [today])
        }
    }
    
    func testGracefullyHandlesZeroBaseline() {
        // Edge case: all historical data is zero
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 0, rhr: 0, sleep: 0),
            makeReadinessScore(daysAgo: 6, hrv: 0, rhr: 0, sleep: 0),
            makeReadinessScore(daysAgo: 5, hrv: 0, rhr: 0, sleep: 0),
            makeReadinessScore(daysAgo: 4, hrv: 0, rhr: 0, sleep: 0),
        ]
        
        let today = makeReadinessScore(daysAgo: 0, hrv: 70, rhr: 70, sleep: 70)
        
        // Should not crash
        XCTAssertNoThrow {
            _ = detector.detect(scores: baseline + [today])
        }
    }
    
    // MARK: - Anomaly Message Tests
    
    func testAnomalyMessagesAreInformative() {
        let baseline = [
            makeReadinessScore(daysAgo: 7, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 6, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 5, hrv: 85, rhr: 70, sleep: 70),
            makeReadinessScore(daysAgo: 4, hrv: 85, rhr: 70, sleep: 70),
        ]
        
        let today = makeReadinessScore(daysAgo: 0, hrv: 55, rhr: 70, sleep: 70)
        
        let anomalies = detector.detect(scores: baseline + [today])
        
        let hrvAnomaly = anomalies.first { $0.type == .hrvDrop }
        XCTAssertNotNil(hrvAnomaly)
        
        // Message should contain useful info
        XCTAssertFalse(hrvAnomaly?.message.isEmpty ?? true)
        XCTAssertFalse(hrvAnomaly?.recommendation.isEmpty ?? true)
        XCTAssertTrue(hrvAnomaly?.message.contains("%") ?? false, "Should include percentage")
    }
    
    // MARK: - Helper Methods
    
    private func makeReadinessScore(
        daysAgo: Int,
        hrv: Double,
        rhr: Double,
        sleep: Double,
        trainingLoad: Double = 70
    ) -> ReadinessScore {
        let date = Date().addingTimeInterval(-Double(daysAgo * 24 * 3600))
        return ReadinessScore(
            date: date,
            overallScore: (hrv + rhr + sleep + trainingLoad) / 4.0,
            sleepScore: sleep,
            hrvScore: hrv,
            restingHRScore: rhr,
            trainingLoadScore: trainingLoad,
            subjectiveScore: nil,
            recommendation: "Test",
            label: .good
        )
    }
}
