import Foundation
import SwiftData
import os

/// Calculates daily readiness score (0-100) from HealthKit signals and training load.
///
/// Weighted factors (per AI_ML_APPROACH.md Decision #4):
///   - Sleep quality: 35%
///   - HRV vs baseline: 30%
///   - Resting HR vs baseline: 15%
///   - Training load (ACWR): 15%
///   - Subjective check-in: 5%
///
/// When data is missing, weights are redistributed among available factors.
public final class ReadinessScoreService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "Readiness")

    // Factor weights per AI_ML_APPROACH.md
    private static let weights: [(factor: Factor, weight: Double)] = [
        (.sleep, 0.35),
        (.hrv, 0.30),
        (.restingHR, 0.15),
        (.trainingLoad, 0.15),
        (.subjective, 0.05)
    ]

    private let sleepCalculator: SleepScoreCalculator
    private let trainingLoadCalculator: TrainingLoadCalculator

    public init(
        sleepCalculator: SleepScoreCalculator = SleepScoreCalculator(),
        trainingLoadCalculator: TrainingLoadCalculator = TrainingLoadCalculator()
    ) {
        self.sleepCalculator = sleepCalculator
        self.trainingLoadCalculator = trainingLoadCalculator
    }

    // MARK: - Factor enum

    enum Factor: String {
        case sleep, hrv, restingHR, trainingLoad, subjective
    }

    // MARK: - Input

    /// Raw inputs for readiness calculation. Supply whatever data is available.
    public struct Input {
        public var sleepRecord: SleepRecord?
        public var recentSleepDurations: [Double]
        public var currentHRV: Double?
        public var hrvBaseline: HealthBaseline?
        public var currentRestingHR: Double?
        public var rhrBaseline: HealthBaseline?
        public var dailyTrainingVolumes: [Double]
        public var subjectiveCheckIn: SubjectiveCheckIn?

        public init(
            sleepRecord: SleepRecord? = nil,
            recentSleepDurations: [Double] = [],
            currentHRV: Double? = nil,
            hrvBaseline: HealthBaseline? = nil,
            currentRestingHR: Double? = nil,
            rhrBaseline: HealthBaseline? = nil,
            dailyTrainingVolumes: [Double] = [],
            subjectiveCheckIn: SubjectiveCheckIn? = nil
        ) {
            self.sleepRecord = sleepRecord
            self.recentSleepDurations = recentSleepDurations
            self.currentHRV = currentHRV
            self.hrvBaseline = hrvBaseline
            self.currentRestingHR = currentRestingHR
            self.rhrBaseline = rhrBaseline
            self.dailyTrainingVolumes = dailyTrainingVolumes
            self.subjectiveCheckIn = subjectiveCheckIn
        }
    }

    // MARK: - Calculation

    /// Calculate readiness score from available inputs.
    /// Missing factors are excluded and weights redistributed proportionally.
    public func calculate(from input: Input, date: Date = Date()) -> ReadinessScore {
        var factorScores: [(factor: Factor, weight: Double, score: Double)] = []

        // Sleep
        if let sleepRecord = input.sleepRecord {
            let score = sleepCalculator.score(
                totalMinutes: sleepRecord.totalMinutes,
                stages: sleepRecord.stages,
                recentDurations: input.recentSleepDurations
            )
            factorScores.append((.sleep, Self.weights[0].weight, score))
        }

        // HRV
        if let hrv = input.currentHRV, let baseline = input.hrvBaseline {
            let score = scoreHRV(current: hrv, baseline: baseline)
            factorScores.append((.hrv, Self.weights[1].weight, score))
        }

        // Resting HR
        if let rhr = input.currentRestingHR, let baseline = input.rhrBaseline {
            let score = scoreRestingHR(current: rhr, baseline: baseline)
            factorScores.append((.restingHR, Self.weights[2].weight, score))
        }

        // Training Load
        if !input.dailyTrainingVolumes.isEmpty {
            let load = trainingLoadCalculator.calculate(dailyVolumes: input.dailyTrainingVolumes)
            let score = trainingLoadCalculator.score(for: load)
            factorScores.append((.trainingLoad, Self.weights[3].weight, score))
        }

        // Subjective
        if let checkIn = input.subjectiveCheckIn {
            factorScores.append((.subjective, Self.weights[4].weight, checkIn.normalizedScore))
        }

        // Redistribute weights among available factors
        let (overall, individual) = computeWeightedScore(factorScores)

        let label = ReadinessLabel.from(score: overall)
        let recommendation = Self.recommendation(for: label)

        Self.logger.info(
            "Readiness calculated: \(overall, privacy: .public) (\(label.rawValue)) from \(factorScores.count) factors"
        )

        return ReadinessScore(
            date: date,
            overallScore: overall,
            sleepScore: individual[.sleep] ?? 0,
            hrvScore: individual[.hrv] ?? 0,
            restingHRScore: individual[.restingHR] ?? 0,
            trainingLoadScore: individual[.trainingLoad] ?? 0,
            subjectiveScore: individual[.subjective],
            recommendation: recommendation,
            label: label
        )
    }

    // MARK: - HRV Scoring

    /// HRV: higher is better. Positive z-score = above baseline = good recovery.
    func scoreHRV(current: Double, baseline: HealthBaseline) -> Double {
        let z = baseline.zScore(for: current)
        // z = 0 → 70 (average), z = +2 → 100, z = -2 → 30
        return clampScore(70.0 + z * 15.0)
    }

    // MARK: - Resting HR Scoring

    /// RHR: lower is better. Negative z-score = below baseline = good recovery.
    func scoreRestingHR(current: Double, baseline: HealthBaseline) -> Double {
        let z = baseline.zScore(for: current)
        // z = 0 → 70 (average), z = -2 → 100, z = +2 → 30
        return clampScore(70.0 - z * 15.0)
    }

    // MARK: - Weighted Score

    /// Compute overall score with weight redistribution for missing factors.
    private func computeWeightedScore(
        _ factors: [(factor: Factor, weight: Double, score: Double)]
    ) -> (overall: Double, individual: [Factor: Double]) {
        guard !factors.isEmpty else {
            return (50.0, [:])
        }

        let totalWeight = factors.map(\.weight).reduce(0, +)
        var overall = 0.0
        var individual: [Factor: Double] = [:]

        for (factor, weight, score) in factors {
            let normalizedWeight = weight / totalWeight
            overall += score * normalizedWeight
            individual[factor] = score
        }

        return (clampScore(overall), individual)
    }

    // MARK: - Recommendations

    /// Training recommendation per readiness label.
    /// Per AI_ML_APPROACH.md: 80+ push, 60-79 normal, 40-59 reduce, <40 rest.
    static func recommendation(for label: ReadinessLabel) -> String {
        switch label {
        case .excellent:
            return "Go heavy — you're fully recovered. Push intensity or volume today."
        case .good:
            return "Moderate volume — solid recovery. Train as planned."
        case .moderate:
            return "Light/deload — consider reducing intensity by 10-15%. Focus on technique."
        case .poor:
            return "Rest day — your body needs recovery. Light mobility or active rest only."
        }
    }

    // MARK: - Helpers

    private func clampScore(_ score: Double) -> Double {
        min(100, max(0, score))
    }
}
