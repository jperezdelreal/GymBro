import Foundation

/// Simplified CUSUM (Cumulative Sum) algorithm for change-point detection.
/// Detects when the distribution of e1RM values shifts — indicating a plateau
/// or regression that simple averages might miss.
public struct ChangePointDetector: Sendable {
    /// Sensitivity: lower values detect smaller shifts. Typical range: 0.5–2.0.
    public let sensitivity: Double
    /// Drift parameter — allowance for small fluctuations before flagging.
    public let drift: Double

    public init(sensitivity: Double = 1.0, drift: Double = 0.5) {
        self.sensitivity = sensitivity
        self.drift = drift
    }

    /// Returns a change-point score from 0.0 (stable trend) to 1.0 (strong distribution shift).
    /// Values near 1.0 indicate that recent performance has shifted significantly from the prior trend.
    public func analyze(values: [Double]) -> Double {
        guard values.count >= 4 else { return 0 }

        // Compute the target (expected) as the mean of the first half
        let midpoint = values.count / 2
        let baseline = Array(values.prefix(midpoint))
        let recent = Array(values.suffix(values.count - midpoint))

        let baselineMean = baseline.reduce(0, +) / Double(baseline.count)
        let baselineStd = standardDeviation(baseline)

        guard baselineStd > 0 else { return 0 }

        // Normalize recent values relative to baseline
        let normalized = recent.map { ($0 - baselineMean) / baselineStd }

        // CUSUM for negative shift (performance drop)
        var cusumNeg: Double = 0
        var maxCusumNeg: Double = 0

        for value in normalized {
            cusumNeg = max(0, cusumNeg - value - drift)
            maxCusumNeg = max(maxCusumNeg, cusumNeg)
        }

        // Also run CUSUM for stagnation: values close to zero (no improvement)
        let recentMean = recent.reduce(0, +) / Double(recent.count)
        let improvementRatio = (recentMean - baselineMean) / baselineMean

        // Combine: high CUSUM negative = regression, low improvement = stagnation
        let regressionScore = min(maxCusumNeg / (sensitivity * 5.0), 1.0)
        let stagnationScore: Double
        if improvementRatio < 0.01 {
            stagnationScore = min(1.0 - improvementRatio * 10.0, 1.0)
        } else {
            stagnationScore = max(0, 0.5 - improvementRatio * 5.0)
        }

        return min(max(regressionScore, stagnationScore), 1.0)
    }

    // MARK: - Internal

    func standardDeviation(_ values: [Double]) -> Double {
        let count = Double(values.count)
        guard count > 1 else { return 0 }
        let mean = values.reduce(0, +) / count
        let variance = values.reduce(0.0) { $0 + ($1 - mean) * ($1 - mean) } / (count - 1)
        return sqrt(variance)
    }
}
