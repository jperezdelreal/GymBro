import Foundation

/// Linear regression-based trend forecaster for e1RM time series.
/// Computes expected trajectory and flags when actual performance deviates below forecast.
public struct TrendForecaster: Sendable {
    /// Number of standard errors below forecast to consider a significant deviation.
    public let deviationThreshold: Double

    public init(deviationThreshold: Double = 1.5) {
        self.deviationThreshold = deviationThreshold
    }

    /// Returns a forecast deviation score from 0.0 (on track) to 1.0 (far below expected).
    /// Uses linear regression on the first 2/3 of data to predict the final 1/3.
    public func analyze(values: [Double]) -> Double {
        guard values.count >= 6 else { return 0 }

        let trainCount = (values.count * 2) / 3
        let trainValues = Array(values.prefix(trainCount))
        let testValues = Array(values.suffix(values.count - trainCount))

        // Fit linear regression on training data
        let regression = linearRegression(values: trainValues)

        // Predict values for the test range
        var totalDeviation: Double = 0
        for (i, actual) in testValues.enumerated() {
            let x = Double(trainCount + i)
            let predicted = regression.intercept + regression.slope * x
            let residual = predicted - actual
            if residual > 0 {
                totalDeviation += residual
            }
        }

        let avgDeviation = totalDeviation / Double(testValues.count)

        // Normalize by the standard error of the training residuals
        let residuals = trainValues.enumerated().map { (i, val) in
            val - (regression.intercept + regression.slope * Double(i))
        }
        let residualStd = standardDeviation(residuals)

        guard residualStd > 0 else {
            return avgDeviation > 0 ? min(avgDeviation / (trainValues.last ?? 1.0) * 10.0, 1.0) : 0
        }

        let normalizedDeviation = avgDeviation / residualStd
        return min(normalizedDeviation / (deviationThreshold * 2.0), 1.0)
    }

    // MARK: - Internal

    struct RegressionResult {
        let slope: Double
        let intercept: Double
    }

    func linearRegression(values: [Double]) -> RegressionResult {
        let n = Double(values.count)
        guard n >= 2 else { return RegressionResult(slope: 0, intercept: values.first ?? 0) }

        let xs = (0..<values.count).map { Double($0) }
        let sumX = xs.reduce(0, +)
        let sumY = values.reduce(0, +)
        let sumXY = zip(xs, values).reduce(0.0) { $0 + $1.0 * $1.1 }
        let sumX2 = xs.reduce(0.0) { $0 + $1 * $1 }

        let denominator = n * sumX2 - sumX * sumX
        guard denominator != 0 else { return RegressionResult(slope: 0, intercept: sumY / n) }

        let slope = (n * sumXY - sumX * sumY) / denominator
        let intercept = (sumY - slope * sumX) / n

        return RegressionResult(slope: slope, intercept: intercept)
    }

    func standardDeviation(_ values: [Double]) -> Double {
        let count = Double(values.count)
        guard count > 1 else { return 0 }
        let mean = values.reduce(0, +) / count
        let variance = values.reduce(0.0) { $0 + ($1 - mean) * ($1 - mean) } / (count - 1)
        return sqrt(variance)
    }
}
