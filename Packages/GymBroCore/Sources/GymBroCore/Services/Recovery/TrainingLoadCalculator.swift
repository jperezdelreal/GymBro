import Foundation

/// Calculates training load metrics using EWMA (Exponentially Weighted Moving Average).
/// Implements Acute:Chronic Workload Ratio (ACWR) for fatigue modeling.
public struct TrainingLoadCalculator: Sendable {
    /// EWMA decay factor for acute load (7-day emphasis).
    public let acuteAlpha: Double
    /// EWMA decay factor for chronic load (28-day emphasis).
    public let chronicAlpha: Double

    /// Initialize with standard ACWR time constants.
    /// alpha = 2 / (N + 1) where N is the rolling window in days.
    public init(acuteDays: Int = 7, chronicDays: Int = 28) {
        self.acuteAlpha = 2.0 / (Double(acuteDays) + 1.0)
        self.chronicAlpha = 2.0 / (Double(chronicDays) + 1.0)
    }

    /// Result of training load analysis.
    public struct TrainingLoad: Sendable, Equatable {
        /// Short-term (7-day) EWMA load.
        public let acuteLoad: Double
        /// Long-term (28-day) EWMA load.
        public let chronicLoad: Double
        /// Acute:Chronic Workload Ratio. Ideal range: 0.8 - 1.3.
        public let acwr: Double
        /// Training Stress Balance: chronic - acute. Negative = accumulated fatigue.
        public let trainingStressBalance: Double

        public init(acuteLoad: Double, chronicLoad: Double) {
            self.acuteLoad = acuteLoad
            self.chronicLoad = chronicLoad
            self.acwr = chronicLoad > 0 ? acuteLoad / chronicLoad : 1.0
            self.trainingStressBalance = chronicLoad - acuteLoad
        }
    }

    /// Calculate training load from daily volume data (most recent last).
    /// Volume can be tonnage (weight × reps), number of hard sets, or active energy.
    /// - Parameter dailyVolumes: Array of daily training volumes, chronologically ordered.
    /// - Returns: Current training load metrics.
    public func calculate(dailyVolumes: [Double]) -> TrainingLoad {
        guard !dailyVolumes.isEmpty else {
            return TrainingLoad(acuteLoad: 0, chronicLoad: 0)
        }

        var acuteEWMA = dailyVolumes[0]
        var chronicEWMA = dailyVolumes[0]

        for i in 1..<dailyVolumes.count {
            acuteEWMA = acuteAlpha * dailyVolumes[i] + (1.0 - acuteAlpha) * acuteEWMA
            chronicEWMA = chronicAlpha * dailyVolumes[i] + (1.0 - chronicAlpha) * chronicEWMA
        }

        return TrainingLoad(acuteLoad: acuteEWMA, chronicLoad: chronicEWMA)
    }

    /// Convert ACWR to a 0-100 readiness-style score.
    /// Ideal ACWR (0.8-1.3) = high score. Too high (>1.5) or too low (<0.5) = low score.
    public func scoreFromACWR(_ acwr: Double) -> Double {
        // Sweet spot: 0.8 - 1.3
        if acwr >= 0.8 && acwr <= 1.3 {
            return 85.0 + (1.0 - abs(acwr - 1.05) / 0.25) * 15.0
        }

        // High risk zone: ACWR > 1.5 — spike in training load
        if acwr > 1.5 {
            return max(10, 85.0 - (acwr - 1.5) * 100.0)
        }

        // Moderate elevation: 1.3 - 1.5
        if acwr > 1.3 {
            return 85.0 - (acwr - 1.3) * 150.0
        }

        // Under-training: ACWR < 0.8 — detraining risk, moderate score
        if acwr >= 0.5 {
            return 60.0 + (acwr - 0.5) / 0.3 * 25.0
        }

        // Very low: significant detraining
        return max(40, acwr / 0.5 * 60.0)
    }

    /// Score based on Training Stress Balance (TSB).
    /// Negative TSB = accumulated fatigue. TSB < -25 triggers deload per AI_ML_APPROACH.md.
    public func scoreFromTSB(_ tsb: Double) -> Double {
        if tsb >= 0 {
            // Fresh/rested — good score
            return min(100, 80.0 + tsb * 0.5)
        }
        // Fatigued — linearly decrease. TSB = -25 → score ~50, TSB = -50 → score ~25
        return max(10, 80.0 + tsb * 1.2)
    }

    /// Combined training load score: blend ACWR and TSB scores.
    public func score(for load: TrainingLoad) -> Double {
        let acwrScore = scoreFromACWR(load.acwr)
        let tsbScore = scoreFromTSB(load.trainingStressBalance)
        return acwrScore * 0.6 + tsbScore * 0.4
    }
}
