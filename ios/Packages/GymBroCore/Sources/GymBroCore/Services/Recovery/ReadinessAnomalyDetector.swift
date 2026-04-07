import Foundation
import os

/// Detects unusual patterns in readiness metrics that may indicate overreaching, illness, or stress.
///
/// **Anomaly triggers (conservative thresholds for high specificity):**
/// - HRV drop: >20% below 7-day baseline
/// - Resting HR spike: >10% above 7-day baseline
/// - Sleep duration drop: >2 hours below 7-day average
/// - Multi-factor decline: 2+ factors degraded simultaneously
///
/// **Design philosophy:**
/// - High specificity over sensitivity (avoid false alarms)
/// - Always recommend professional consultation for concerning patterns
/// - Graceful degradation when data is missing
public final class ReadinessAnomalyDetector: Sendable {
    private static let logger = Logger(subsystem: "com.gymbro", category: "AnomalyDetector")
    
    // MARK: - Anomaly Thresholds
    
    /// HRV must drop >20% below baseline to trigger alert
    private static let hrvDropThreshold: Double = -0.20
    
    /// Resting HR must spike >10% above baseline to trigger alert
    private static let rhrSpikeThreshold: Double = 0.10
    
    /// Sleep duration must drop >2 hours below average to trigger alert
    private static let sleepDropHours: Double = 2.0
    
    /// Require 2+ degraded factors for multi-factor alert
    private static let multiFactorThreshold = 2
    
    public init() {}
    
    // MARK: - Public API
    
    /// Detect anomalies in readiness scores over recent history.
    /// - Parameter scores: Recent readiness scores (7-14 days recommended)
    /// - Parameter healthMetrics: Recent HealthKit metrics for detailed analysis
    /// - Returns: List of detected anomalies (empty if none)
    public func detect(
        scores: [ReadinessScore],
        healthMetrics: [HealthMetric] = []
    ) -> [ReadinessAnomaly] {
        guard !scores.isEmpty else { return [] }
        
        // Sort by date (newest first)
        let sortedScores = scores.sorted { $0.date > $1.date }
        guard let latest = sortedScores.first else { return [] }
        
        var anomalies: [ReadinessAnomaly] = []
        
        // Calculate baselines from historical data (exclude today)
        let historical = Array(sortedScores.dropFirst())
        guard historical.count >= 3 else {
            Self.logger.info("Insufficient historical data for anomaly detection (need 3+ days)")
            return []
        }
        
        let baselines = calculateBaselines(from: historical)
        
        // Check each anomaly type
        if let hrvAnomaly = detectHRVAnomaly(latest: latest, baseline: baselines.hrv) {
            anomalies.append(hrvAnomaly)
        }
        
        if let rhrAnomaly = detectRHRAnomaly(latest: latest, baseline: baselines.rhr) {
            anomalies.append(rhrAnomaly)
        }
        
        if let sleepAnomaly = detectSleepAnomaly(
            latest: latest,
            baseline: baselines.sleep,
            healthMetrics: healthMetrics
        ) {
            anomalies.append(sleepAnomaly)
        }
        
        // Multi-factor check: are 2+ factors simultaneously degraded?
        if let multiFactorAnomaly = detectMultiFactorDecline(
            latest: latest,
            baselines: baselines
        ) {
            anomalies.append(multiFactorAnomaly)
        }
        
        if !anomalies.isEmpty {
            Self.logger.warning(
                "Detected \(anomalies.count) anomalies on \(latest.date.formatted(date: .abbreviated, time: .omitted))"
            )
        }
        
        return anomalies
    }
    
    // MARK: - Individual Anomaly Detectors
    
    /// Detect sudden HRV drop (>20% below baseline).
    private func detectHRVAnomaly(
        latest: ReadinessScore,
        baseline: Double?
    ) -> ReadinessAnomaly? {
        guard let baseline = baseline, baseline > 0, latest.hrvScore > 0 else {
            return nil
        }
        
        // Convert score back to z-score approximation
        // hrvScore = 70 + z * 15, so z ≈ (score - 70) / 15
        let latestZ = (latest.hrvScore - 70) / 15
        let baselineZ = (baseline - 70) / 15
        let percentChange = (latestZ - baselineZ) / max(abs(baselineZ), 0.5)
        
        guard percentChange < Self.hrvDropThreshold else { return nil }
        
        Self.logger.warning(
            "HRV anomaly detected: \(String(format: "%.1f", percentChange * 100))% below baseline"
        )
        
        return ReadinessAnomaly(
            type: .hrvDrop,
            severity: percentChange < -0.30 ? .high : .medium,
            date: latest.date,
            message: "HRV dropped \(abs(Int(percentChange * 100)))% below your 7-day baseline",
            recommendation: "This may indicate insufficient recovery, illness, or stress. Consider a rest day or light training.",
            affectedMetrics: ["HRV": latest.hrvScore]
        )
    }
    
    /// Detect resting HR spike (>10% above baseline).
    private func detectRHRAnomaly(
        latest: ReadinessScore,
        baseline: Double?
    ) -> ReadinessAnomaly? {
        guard let baseline = baseline, baseline > 0, latest.restingHRScore > 0 else {
            return nil
        }
        
        // RHR score is inverted: 70 - z * 15
        let latestZ = (70 - latest.restingHRScore) / 15
        let baselineZ = (70 - baseline) / 15
        let percentChange = (latestZ - baselineZ) / max(abs(baselineZ), 0.5)
        
        guard percentChange > Self.rhrSpikeThreshold else { return nil }
        
        Self.logger.warning(
            "RHR anomaly detected: \(String(format: "%.1f", percentChange * 100))% above baseline"
        )
        
        return ReadinessAnomaly(
            type: .rhrSpike,
            severity: percentChange > 0.20 ? .high : .medium,
            date: latest.date,
            message: "Resting heart rate is \(Int(percentChange * 100))% higher than your 7-day baseline",
            recommendation: "Elevated resting HR may indicate overtraining, illness, or poor sleep. Monitor for additional symptoms.",
            affectedMetrics: ["Resting HR": latest.restingHRScore]
        )
    }
    
    /// Detect sleep duration drop (>2h below average).
    private func detectSleepAnomaly(
        latest: ReadinessScore,
        baseline: Double?,
        healthMetrics: [HealthMetric]
    ) -> ReadinessAnomaly? {
        guard let baseline = baseline, baseline > 0, latest.sleepScore > 0 else {
            return nil
        }
        
        // Sleep score is complex (duration + quality + consistency), but we can detect large drops
        let scoreDrop = baseline - latest.sleepScore
        
        // If sleep score dropped >25 points, likely significant sleep loss
        guard scoreDrop > 25 else { return nil }
        
        // Try to get actual sleep duration from health metrics
        let recentSleep = healthMetrics
            .filter { $0.type == .sleepDuration }
            .sorted { $0.date > $1.date }
            .first
        
        let durationMessage: String
        if let sleepMetric = recentSleep {
            let hours = sleepMetric.value / 60
            durationMessage = "Sleep duration: \(String(format: "%.1f", hours))h"
        } else {
            durationMessage = "Sleep score dropped \(Int(scoreDrop)) points"
        }
        
        Self.logger.warning("Sleep anomaly detected: \(durationMessage)")
        
        return ReadinessAnomaly(
            type: .sleepDrop,
            severity: scoreDrop > 35 ? .high : .medium,
            date: latest.date,
            message: durationMessage + " — well below your typical range",
            recommendation: "Prioritize sleep tonight. Consider reducing training intensity until sleep normalizes.",
            affectedMetrics: ["Sleep": latest.sleepScore]
        )
    }
    
    /// Detect multi-factor decline (2+ metrics degraded simultaneously).
    private func detectMultiFactorDecline(
        latest: ReadinessScore,
        baselines: Baselines
    ) -> ReadinessAnomaly? {
        var degradedFactors: [String: Double] = [:]
        
        // Check each factor for significant decline
        if let baseline = baselines.hrv, baseline > 0, latest.hrvScore > 0 {
            if latest.hrvScore < baseline - 15 {
                degradedFactors["HRV"] = latest.hrvScore
            }
        }
        
        if let baseline = baselines.rhr, baseline > 0, latest.restingHRScore > 0 {
            if latest.restingHRScore < baseline - 15 {
                degradedFactors["Resting HR"] = latest.restingHRScore
            }
        }
        
        if let baseline = baselines.sleep, baseline > 0, latest.sleepScore > 0 {
            if latest.sleepScore < baseline - 15 {
                degradedFactors["Sleep"] = latest.sleepScore
            }
        }
        
        if let baseline = baselines.trainingLoad, baseline > 0, latest.trainingLoadScore > 0 {
            if latest.trainingLoadScore < baseline - 15 {
                degradedFactors["Training Load"] = latest.trainingLoadScore
            }
        }
        
        guard degradedFactors.count >= Self.multiFactorThreshold else { return nil }
        
        Self.logger.warning(
            "Multi-factor decline detected: \(degradedFactors.keys.joined(separator: ", "))"
        )
        
        let factorList = degradedFactors.keys.sorted().joined(separator: ", ")
        
        return ReadinessAnomaly(
            type: .multiFactorDecline,
            severity: degradedFactors.count >= 3 ? .high : .medium,
            date: latest.date,
            message: "Multiple recovery metrics declined: \(factorList)",
            recommendation: "Significant decline across multiple factors. Take a rest day and monitor symptoms. Consult a healthcare provider if symptoms persist.",
            affectedMetrics: degradedFactors
        )
    }
    
    // MARK: - Baseline Calculation
    
    /// Calculate rolling 7-day baselines for each readiness factor.
    private func calculateBaselines(from scores: [ReadinessScore]) -> Baselines {
        guard !scores.isEmpty else {
            return Baselines(hrv: nil, rhr: nil, sleep: nil, trainingLoad: nil)
        }
        
        let hrvScores = scores.compactMap { $0.hrvScore > 0 ? $0.hrvScore : nil }
        let rhrScores = scores.compactMap { $0.restingHRScore > 0 ? $0.restingHRScore : nil }
        let sleepScores = scores.compactMap { $0.sleepScore > 0 ? $0.sleepScore : nil }
        let loadScores = scores.compactMap { $0.trainingLoadScore > 0 ? $0.trainingLoadScore : nil }
        
        return Baselines(
            hrv: average(hrvScores),
            rhr: average(rhrScores),
            sleep: average(sleepScores),
            trainingLoad: average(loadScores)
        )
    }
    
    /// Calculate average of non-empty array.
    private func average(_ values: [Double]) -> Double? {
        guard !values.isEmpty else { return nil }
        return values.reduce(0, +) / Double(values.count)
    }
}

// MARK: - Supporting Types

/// Detected anomaly in readiness metrics.
public struct ReadinessAnomaly: Codable, Sendable, Equatable {
    public let type: AnomalyType
    public let severity: AnomalySeverity
    public let date: Date
    public let message: String
    public let recommendation: String
    public let affectedMetrics: [String: Double]
    
    public init(
        type: AnomalyType,
        severity: AnomalySeverity,
        date: Date,
        message: String,
        recommendation: String,
        affectedMetrics: [String: Double]
    ) {
        self.type = type
        self.severity = severity
        self.date = date
        self.message = message
        self.recommendation = recommendation
        self.affectedMetrics = affectedMetrics
    }
}

/// Type of anomaly detected.
public enum AnomalyType: String, Codable, Sendable {
    case hrvDrop              // Sudden HRV decline
    case rhrSpike             // Elevated resting HR
    case sleepDrop            // Sleep duration/quality drop
    case multiFactorDecline   // Multiple factors degraded
    
    public var displayName: String {
        switch self {
        case .hrvDrop: return "HRV Drop"
        case .rhrSpike: return "Elevated Resting HR"
        case .sleepDrop: return "Poor Sleep"
        case .multiFactorDecline: return "Multi-Factor Decline"
        }
    }
    
    public var icon: String {
        switch self {
        case .hrvDrop: return "waveform.path.ecg"
        case .rhrSpike: return "heart.fill"
        case .sleepDrop: return "bed.double.fill"
        case .multiFactorDecline: return "exclamationmark.triangle.fill"
        }
    }
}

/// Severity of detected anomaly.
public enum AnomalySeverity: String, Codable, Sendable {
    case medium  // Notable deviation, monitor
    case high    // Significant deviation, action recommended
    
    public var color: String {
        switch self {
        case .medium: return "orange"
        case .high: return "red"
        }
    }
}

/// Internal baseline values for anomaly comparison.
private struct Baselines {
    let hrv: Double?
    let rhr: Double?
    let sleep: Double?
    let trainingLoad: Double?
}
