import XCTest
@testable import GymBroCore

final class DeloadAutomationServiceTests: XCTestCase {
    var service: DeloadAutomationService!
    var calendar: Calendar!
    
    override func setUp() {
        super.setUp()
        service = DeloadAutomationService()
        calendar = Calendar.current
    }
    
    override func tearDown() {
        service = nil
        calendar = nil
        super.tearDown()
    }
    
    // MARK: - ACWR Spike Detection Tests
    
    func testACWRSpikeTrigger() throws {
        // Given: 7 days of moderate training followed by a spike
        let baseDate = Date()
        var workouts: [Workout] = []
        
        // Week 1-3: steady baseline (1000kg/day)
        for day in 0..<21 {
            let date = calendar.date(byAdding: .day, value: -21 + day, to: baseDate)!
            let workout = makeWorkout(date: date, volume: 1000)
            workouts.append(workout)
        }
        
        // Week 4: spike to 2000kg/day (100% increase)
        for day in 0..<7 {
            let date = calendar.date(byAdding: .day, value: -7 + day, to: baseDate)!
            let workout = makeWorkout(date: date, volume: 2000)
            workouts.append(workout)
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: workouts,
            readinessScores: [],
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should detect ACWR spike
        XCTAssertTrue(recommendation.shouldDeload, "Should recommend deload on ACWR spike")
        XCTAssertEqual(recommendation.state, .needsDeload, "Should be in needsDeload state")
        XCTAssertGreaterThan(recommendation.acwr, 1.5, "ACWR should exceed 1.5")
        XCTAssertTrue(
            recommendation.triggers.contains { $0.type == .acwrSpike },
            "Should have ACWR spike trigger"
        )
        XCTAssertEqual(
            recommendation.triggers.first { $0.type == .acwrSpike }?.severity,
            .high,
            "ACWR spike should be high severity"
        )
    }
    
    func testNormalACWR() throws {
        // Given: 4 weeks of steady training (no spike)
        let baseDate = Date()
        var workouts: [Workout] = []
        
        for day in 0..<28 {
            let date = calendar.date(byAdding: .day, value: -28 + day, to: baseDate)!
            // Gradual 5% weekly increase (safe)
            let weekNumber = day / 7
            let volume = 1000.0 + Double(weekNumber) * 50.0
            let workout = makeWorkout(date: date, volume: volume)
            workouts.append(workout)
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: workouts,
            readinessScores: [],
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: ACWR should be in safe range
        XCTAssertLessThan(recommendation.acwr, 1.5, "ACWR should be below spike threshold")
        XCTAssertGreaterThan(recommendation.acwr, 0.8, "ACWR should be above detraining threshold")
        XCTAssertFalse(
            recommendation.triggers.contains { $0.type == .acwrSpike },
            "Should not trigger ACWR spike"
        )
    }
    
    // MARK: - Chronic Fatigue Detection Tests
    
    func testChronicFatigueTrigger() throws {
        // Given: 3 consecutive days of low readiness
        let baseDate = Date()
        var readinessScores: [ReadinessScore] = []
        
        for day in 0..<3 {
            let date = calendar.date(byAdding: .day, value: -3 + day, to: baseDate)!
            let score = makeReadinessScore(date: date, overallScore: 35)
            readinessScores.append(score)
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: readinessScores,
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should detect chronic fatigue
        XCTAssertTrue(recommendation.shouldDeload, "Should recommend deload on chronic fatigue")
        XCTAssertTrue(
            recommendation.triggers.contains { $0.type == .chronicFatigue },
            "Should have chronic fatigue trigger"
        )
        XCTAssertEqual(
            recommendation.triggers.first { $0.type == .chronicFatigue }?.severity,
            .high,
            "Chronic fatigue should be high severity"
        )
    }
    
    func testChronicFatigueNotTriggeredWithGoodDays() throws {
        // Given: mix of good and poor readiness (not consecutive)
        let baseDate = Date()
        var readinessScores: [ReadinessScore] = []
        
        readinessScores.append(makeReadinessScore(
            date: calendar.date(byAdding: .day, value: -4, to: baseDate)!,
            overallScore: 35
        ))
        readinessScores.append(makeReadinessScore(
            date: calendar.date(byAdding: .day, value: -3, to: baseDate)!,
            overallScore: 75 // Good day breaks streak
        ))
        readinessScores.append(makeReadinessScore(
            date: calendar.date(byAdding: .day, value: -2, to: baseDate)!,
            overallScore: 35
        ))
        readinessScores.append(makeReadinessScore(
            date: calendar.date(byAdding: .day, value: -1, to: baseDate)!,
            overallScore: 35
        ))
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: readinessScores,
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should NOT trigger chronic fatigue (streak broken)
        XCTAssertFalse(
            recommendation.triggers.contains { $0.type == .chronicFatigue },
            "Should not trigger chronic fatigue with broken streak"
        )
    }
    
    func testChronicFatigueThreshold() throws {
        // Given: 3 consecutive days at exactly threshold (40)
        let baseDate = Date()
        var readinessScores: [ReadinessScore] = []
        
        for day in 0..<3 {
            let date = calendar.date(byAdding: .day, value: -3 + day, to: baseDate)!
            let score = makeReadinessScore(date: date, overallScore: 40)
            readinessScores.append(score)
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: readinessScores,
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should NOT trigger (threshold is exclusive <40)
        XCTAssertFalse(
            recommendation.triggers.contains { $0.type == .chronicFatigue },
            "Should not trigger at exactly threshold value"
        )
    }
    
    // MARK: - Volume Accumulation Tests
    
    func testVolumeAccumulationTrigger() throws {
        // Given: 4 weeks since last deload
        let baseDate = Date()
        let lastDeloadDate = calendar.date(byAdding: .weekOfYear, value: -4, to: baseDate)!
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: [],
            lastDeloadDate: lastDeloadDate,
            currentDate: baseDate
        )
        
        // Then: should trigger volume accumulation
        XCTAssertTrue(
            recommendation.triggers.contains { $0.type == .volumeAccumulation },
            "Should trigger volume accumulation after 4 weeks"
        )
        XCTAssertEqual(
            recommendation.triggers.first { $0.type == .volumeAccumulation }?.severity,
            .moderate,
            "Volume accumulation should be moderate severity"
        )
    }
    
    func testNoVolumeAccumulationBeforeFourWeeks() throws {
        // Given: only 3 weeks since last deload
        let baseDate = Date()
        let lastDeloadDate = calendar.date(byAdding: .weekOfYear, value: -3, to: baseDate)!
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: [],
            lastDeloadDate: lastDeloadDate,
            currentDate: baseDate
        )
        
        // Then: should NOT trigger volume accumulation yet
        XCTAssertFalse(
            recommendation.triggers.contains { $0.type == .volumeAccumulation },
            "Should not trigger volume accumulation before 4 weeks"
        )
    }
    
    func testFirstTimeDeloadSuggestion() throws {
        // Given: never deloaded before (nil lastDeloadDate)
        let baseDate = Date()
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: [],
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should suggest planning a deload
        XCTAssertTrue(
            recommendation.triggers.contains { $0.type == .volumeAccumulation },
            "Should suggest deload planning for first-time users"
        )
    }
    
    // MARK: - State Machine Tests
    
    func testNormalState() throws {
        // Given: good ACWR, recent deload, good readiness
        let baseDate = Date()
        let lastDeloadDate = calendar.date(byAdding: .weekOfYear, value: -2, to: baseDate)!
        
        var workouts: [Workout] = []
        for day in 0..<14 {
            let date = calendar.date(byAdding: .day, value: -14 + day, to: baseDate)!
            let workout = makeWorkout(date: date, volume: 1000)
            workouts.append(workout)
        }
        
        var readinessScores: [ReadinessScore] = []
        for day in 0..<3 {
            let date = calendar.date(byAdding: .day, value: -3 + day, to: baseDate)!
            readinessScores.append(makeReadinessScore(date: date, overallScore: 75))
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: workouts,
            readinessScores: readinessScores,
            lastDeloadDate: lastDeloadDate,
            currentDate: baseDate
        )
        
        // Then: should be in normal state
        XCTAssertEqual(recommendation.state, .normal)
        XCTAssertFalse(recommendation.shouldDeload)
        XCTAssertEqual(recommendation.urgency, .none)
    }
    
    func testOverreachingState() throws {
        // Given: one moderate trigger (volume accumulation only)
        let baseDate = Date()
        let lastDeloadDate = calendar.date(byAdding: .weekOfYear, value: -5, to: baseDate)!
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: [],
            lastDeloadDate: lastDeloadDate,
            currentDate: baseDate
        )
        
        // Then: should be in overreaching state
        XCTAssertEqual(recommendation.state, .overreaching)
        XCTAssertTrue(recommendation.shouldDeload)
        XCTAssertEqual(recommendation.urgency, .recommended)
    }
    
    func testNeedsDeloadState() throws {
        // Given: multiple high severity triggers (ACWR + chronic fatigue)
        let baseDate = Date()
        
        // ACWR spike
        var workouts: [Workout] = []
        for day in 0..<21 {
            let date = calendar.date(byAdding: .day, value: -21 + day, to: baseDate)!
            workouts.append(makeWorkout(date: date, volume: 1000))
        }
        for day in 0..<7 {
            let date = calendar.date(byAdding: .day, value: -7 + day, to: baseDate)!
            workouts.append(makeWorkout(date: date, volume: 2000))
        }
        
        // Chronic fatigue
        var readinessScores: [ReadinessScore] = []
        for day in 0..<3 {
            let date = calendar.date(byAdding: .day, value: -3 + day, to: baseDate)!
            readinessScores.append(makeReadinessScore(date: date, overallScore: 35))
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: workouts,
            readinessScores: readinessScores,
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should be in needsDeload state
        XCTAssertEqual(recommendation.state, .needsDeload)
        XCTAssertTrue(recommendation.shouldDeload)
        XCTAssertEqual(recommendation.urgency, .immediate)
        XCTAssertGreaterThanOrEqual(
            recommendation.triggers.filter { $0.severity == .high }.count,
            2,
            "Should have 2+ high severity triggers"
        )
    }
    
    // MARK: - Deload Week Generation Tests
    
    func testGenerateDeloadWeek() throws {
        // Given: a normal training week
        let baseDate = Date()
        var normalWeek: [Workout] = []
        
        // 4 workouts, 20 sets each, 100kg average per set
        for day in [0, 2, 4, 6] {
            let date = calendar.date(byAdding: .day, value: day, to: baseDate)!
            let workout = makeWorkout(date: date, sets: 20, avgWeight: 100)
            normalWeek.append(workout)
        }
        
        let normalVolume = normalWeek.reduce(0.0) { $0 + $1.totalVolume }
        let normalSets = normalWeek.reduce(0) { $0 + $1.totalSets }
        
        // When: generate deload week
        let deloadWeek = service.generateDeloadWeek(
            normalWeekWorkouts: normalWeek,
            deloadIntensity: 0.65
        )
        
        // Then: should reduce volume appropriately
        XCTAssertEqual(deloadWeek.targetVolumeReduction, 35, "Should reduce volume by 35% (to 65%)")
        XCTAssertTrue(deloadWeek.maintainIntensity, "Should maintain intensity")
        XCTAssertEqual(
            deloadWeek.targetVolume,
            normalVolume * 0.65,
            accuracy: 0.01,
            "Target volume should be 65% of normal"
        )
        XCTAssertEqual(
            deloadWeek.targetSets,
            Int(Double(normalSets) * 0.65),
            "Target sets should be 65% of normal"
        )
    }
    
    func testDeloadWeekCustomIntensity() throws {
        // Given: normal week
        let workout = makeWorkout(date: Date(), sets: 20, avgWeight: 100)
        
        // When: generate with custom 70% intensity
        let deloadWeek = service.generateDeloadWeek(
            normalWeekWorkouts: [workout],
            deloadIntensity: 0.70
        )
        
        // Then: should use custom intensity
        XCTAssertEqual(deloadWeek.targetVolumeReduction, 30, "Should reduce to 70%")
        XCTAssertEqual(
            deloadWeek.targetVolume,
            workout.totalVolume * 0.70,
            accuracy: 0.01
        )
    }
    
    // MARK: - Edge Cases
    
    func testEmptyWorkoutHistory() throws {
        // Given: no workout history
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: [],
            lastDeloadDate: nil,
            currentDate: Date()
        )
        
        // Then: should handle gracefully with normal state
        XCTAssertEqual(recommendation.state, .normal)
        XCTAssertEqual(recommendation.acwr, 1.0, accuracy: 0.01, "Default ACWR should be 1.0")
    }
    
    func testInsufficientReadinessData() throws {
        // Given: only 2 days of readiness (need 3 for chronic fatigue)
        let baseDate = Date()
        var readinessScores: [ReadinessScore] = []
        for day in 0..<2 {
            let date = calendar.date(byAdding: .day, value: day, to: baseDate)!
            readinessScores.append(makeReadinessScore(date: date, overallScore: 30))
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: [],
            readinessScores: readinessScores,
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should not trigger chronic fatigue
        XCTAssertFalse(
            recommendation.triggers.contains { $0.type == .chronicFatigue },
            "Should not trigger with insufficient data"
        )
    }
    
    func testDetrainingDetection() throws {
        // Given: very low training volume (simulating time off)
        let baseDate = Date()
        var workouts: [Workout] = []
        
        // 3 weeks of normal volume
        for day in 0..<21 {
            let date = calendar.date(byAdding: .day, value: -35 + day, to: baseDate)!
            workouts.append(makeWorkout(date: date, volume: 1000))
        }
        
        // 2 weeks of very low volume (vacation/injury)
        for day in 0..<14 {
            let date = calendar.date(byAdding: .day, value: -14 + day, to: baseDate)!
            workouts.append(makeWorkout(date: date, volume: 100))
        }
        
        // When: analyze deload need
        let recommendation = service.analyzeDeloadNeed(
            workouts: workouts,
            readinessScores: [],
            lastDeloadDate: nil,
            currentDate: baseDate
        )
        
        // Then: should detect detraining (ACWR < 0.7)
        XCTAssertLessThan(recommendation.acwr, 0.7, "ACWR should indicate detraining")
        XCTAssertEqual(recommendation.state, .detraining)
        XCTAssertFalse(recommendation.shouldDeload, "Should not recommend deload when detraining")
    }
    
    // MARK: - Helper Methods
    
    private func makeWorkout(
        date: Date,
        volume: Double = 1000,
        sets: Int = 10,
        avgWeight: Double = 100
    ) -> Workout {
        let workout = Workout(date: date)
        
        // Create mock sets to achieve target volume
        let repsPerSet = Int(volume / (Double(sets) * avgWeight))
        
        for _ in 0..<sets {
            let set = ExerciseSet(
                workout: workout,
                exercise: nil,
                setNumber: 1,
                reps: repsPerSet,
                weightKg: avgWeight,
                setType: .working
            )
            workout.sets.append(set)
        }
        
        return workout
    }
    
    private func makeReadinessScore(
        date: Date,
        overallScore: Double
    ) -> ReadinessScore {
        return ReadinessScore(
            date: date,
            overallScore: overallScore,
            sleepScore: 70,
            hrvScore: 70,
            restingHRScore: 70,
            trainingLoadScore: 70,
            subjectiveScore: nil,
            recommendation: "",
            label: .from(score: overallScore)
        )
    }
}
