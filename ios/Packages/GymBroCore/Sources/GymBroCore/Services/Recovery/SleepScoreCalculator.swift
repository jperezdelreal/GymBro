import Foundation

/// Calculates a 0-100 sleep quality score from duration, efficiency, and consistency.
public struct SleepScoreCalculator: Sendable {
    /// Target sleep range (hours).
    public let targetMinHours: Double
    public let targetMaxHours: Double

    public init(targetMinHours: Double = 7.0, targetMaxHours: Double = 9.0) {
        self.targetMinHours = targetMinHours
        self.targetMaxHours = targetMaxHours
    }

    /// Compute a single-night sleep score from a SleepRecord.
    /// - Parameters:
    ///   - totalMinutes: Total sleep minutes for the night.
    ///   - stages: Optional sleep stage breakdown for efficiency scoring.
    ///   - recentDurations: Past 7 nights of total sleep minutes for consistency scoring.
    /// - Returns: Score 0-100.
    public func score(
        totalMinutes: Double,
        stages: SleepStageBreakdown? = nil,
        recentDurations: [Double] = []
    ) -> Double {
        let durationScore = scoreDuration(totalMinutes)
        let efficiencyScore = scoreEfficiency(stages: stages, totalMinutes: totalMinutes)
        let consistencyScore = scoreConsistency(recentDurations)

        // Weighted: duration 50%, efficiency 30%, consistency 20%
        return durationScore * 0.5 + efficiencyScore * 0.3 + consistencyScore * 0.2
    }

    // MARK: - Components

    /// Duration score: 100 if within target range, degrades linearly outside.
    func scoreDuration(_ totalMinutes: Double) -> Double {
        let hours = totalMinutes / 60.0
        let minTarget = targetMinHours
        let maxTarget = targetMaxHours

        if hours >= minTarget && hours <= maxTarget {
            return 100.0
        } else if hours < minTarget {
            // Linear decay: 0h = 0 score
            return max(0, (hours / minTarget) * 100.0)
        } else {
            // Over-sleeping: gentle penalty, 12h+ = 60 score
            let excess = hours - maxTarget
            return max(60, 100.0 - (excess / 3.0) * 40.0)
        }
    }

    /// Efficiency score: ratio of actual sleep vs time in bed, plus deep/REM bonus.
    func scoreEfficiency(stages: SleepStageBreakdown?, totalMinutes: Double) -> Double {
        guard let stages, stages.inBedMinutes > 0 else {
            // No stage data — assume decent efficiency
            return totalMinutes > 0 ? 70.0 : 0.0
        }

        let sleepEfficiency = stages.totalSleepMinutes / (stages.inBedMinutes + stages.totalSleepMinutes)
        let efficiencyPart = min(sleepEfficiency / 0.85, 1.0) * 60.0

        // Deep + REM should be ~40% of total sleep for quality rest
        let qualitySleep = stages.deepMinutes + stages.remMinutes
        let qualityRatio = stages.totalSleepMinutes > 0
            ? qualitySleep / stages.totalSleepMinutes
            : 0
        let qualityPart = min(qualityRatio / 0.40, 1.0) * 40.0

        return efficiencyPart + qualityPart
    }

    /// Consistency score: low variance in sleep duration over past 7 nights.
    func scoreConsistency(_ recentDurations: [Double]) -> Double {
        guard recentDurations.count >= 3 else {
            // Not enough data for consistency — neutral score
            return 70.0
        }

        let mean = recentDurations.reduce(0, +) / Double(recentDurations.count)
        guard mean > 0 else { return 0 }

        let variance = recentDurations.map { ($0 - mean) * ($0 - mean) }
            .reduce(0, +) / Double(recentDurations.count)
        let cv = (variance.squareRoot()) / mean

        // CV < 0.05 = very consistent (100), CV > 0.3 = very inconsistent (0)
        return max(0, min(100, (1.0 - cv / 0.3) * 100.0))
    }
}
