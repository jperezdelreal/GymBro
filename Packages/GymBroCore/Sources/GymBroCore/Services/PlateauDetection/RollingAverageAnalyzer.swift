import Foundation

/// Analyzes e1RM trend data using rolling averages to detect stagnation.
/// Computes short-term and long-term moving averages, then measures rate of change.
public struct RollingAverageAnalyzer: Sendable {
    /// Window sizes in number of data points (sessions).
    public let shortWindow: Int
    public let longWindow: Int
    /// Minimum percentage improvement per session to consider "progressing".
    public let stagnationThreshold: Double

    public init(shortWindow: Int = 3, longWindow: Int = 6, stagnationThreshold: Double = 0.5) {
        self.shortWindow = shortWindow
        self.longWindow = longWindow
        self.stagnationThreshold = stagnationThreshold
    }

    /// Returns a stagnation score from 0.0 (clear progress) to 1.0 (fully stalled).
    /// Requires at least `longWindow` data points to produce a meaningful result.
    public func analyze(values: [Double]) -> Double {
        guard values.count >= longWindow else { return 0 }

        let shortMA = movingAverage(values: values, window: shortWindow)
        let longMA = movingAverage(values: values, window: longWindow)

        guard let recentShort = shortMA.last, let recentLong = longMA.last, recentLong > 0 else {
            return 0
        }

        // Rate of change: percentage difference between short and long MA
        let rateOfChange = ((recentShort - recentLong) / recentLong) * 100.0

        // Also check the slope of the short-term MA
        let shortSlope = slope(of: Array(shortMA.suffix(shortWindow)))

        // Combine signals
        if rateOfChange > stagnationThreshold && shortSlope > 0 {
            return 0  // Clear progress
        } else if rateOfChange < -stagnationThreshold {
            return min(1.0, abs(rateOfChange) / 5.0)  // Declining
        } else {
            // Flat — map closeness to zero into a 0.3–0.8 range
            let flatness = 1.0 - min(abs(rateOfChange) / stagnationThreshold, 1.0)
            return 0.3 + flatness * 0.5
        }
    }

    // MARK: - Internal

    func movingAverage(values: [Double], window: Int) -> [Double] {
        guard values.count >= window else { return values }
        var result: [Double] = []
        for i in (window - 1)..<values.count {
            let windowSlice = values[(i - window + 1)...i]
            let avg = windowSlice.reduce(0, +) / Double(window)
            result.append(avg)
        }
        return result
    }

    func slope(of values: [Double]) -> Double {
        guard values.count >= 2 else { return 0 }
        let n = Double(values.count)
        let xs = (0..<values.count).map { Double($0) }
        let sumX = xs.reduce(0, +)
        let sumY = values.reduce(0, +)
        let sumXY = zip(xs, values).reduce(0.0) { $0 + $1.0 * $1.1 }
        let sumX2 = xs.reduce(0.0) { $0 + $1 * $1 }
        let denominator = n * sumX2 - sumX * sumX
        guard denominator != 0 else { return 0 }
        return (n * sumXY - sumX * sumY) / denominator
    }
}
