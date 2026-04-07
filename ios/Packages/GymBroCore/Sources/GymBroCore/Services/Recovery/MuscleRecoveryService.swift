import Foundation
import SwiftData
import os

/// Tracks recovery status per muscle group based on training volume and time since last trained.
///
/// **Recovery time factors:**
/// - Volume (sets × reps × weight): Higher volume = longer recovery
/// - Intensity (%1RM or RPE): Heavier loads = longer recovery
/// - Muscle size: Legs (72h) > Back/Chest (48h) > Arms/Shoulders (36h)
/// - Training experience: Advanced lifters recover faster (not yet implemented)
///
/// **Recovery status:**
/// - Fresh: >48h since last trained (large muscles), >36h (small muscles)
/// - Recovering: 24-48h since training, partial recovery
/// - Fatigued: <24h or very high recent volume (>1.5x baseline)
public final class MuscleRecoveryService: Sendable {
    private static let logger = Logger(subsystem: "com.gymbro", category: "MuscleRecovery")
    
    // MARK: - Recovery Time Constants (evidence-based)
    
    /// Base recovery times per muscle group (hours).
    /// Based on research: larger muscles need more recovery time.
    private static let baseRecoveryTimes: [String: TimeInterval] = [
        // Large muscle groups (72h)
        "Quadriceps": 72 * 3600,
        "Hamstrings": 72 * 3600,
        "Glutes": 72 * 3600,
        
        // Medium-large muscle groups (48h)
        "Chest": 48 * 3600,
        "Back": 48 * 3600,
        "Lats": 48 * 3600,
        
        // Medium muscle groups (42h)
        "Shoulders": 42 * 3600,
        "Traps": 42 * 3600,
        
        // Smaller muscle groups (36h)
        "Biceps": 36 * 3600,
        "Triceps": 36 * 3600,
        "Forearms": 36 * 3600,
        "Calves": 36 * 3600,
        "Abs": 36 * 3600,
    ]
    
    /// Default recovery time if muscle group not found (48h)
    private static let defaultRecoveryTime: TimeInterval = 48 * 3600
    
    // Volume thresholds for fatigue classification
    private static let highVolumeMultiplier = 1.5 // >1.5x baseline = fatigued
    
    public init() {}
    
    // MARK: - Public API
    
    /// Calculate recovery status for all muscle groups based on recent training history.
    /// - Parameter workouts: Recent workouts (recommend 7-14 days)
    /// - Parameter currentDate: Current date/time (default: now)
    /// - Returns: Dictionary of muscle group name → recovery status
    public func calculateRecoveryMap(
        workouts: [Workout],
        currentDate: Date = Date()
    ) -> [String: MuscleRecoveryStatus] {
        guard !workouts.isEmpty else { return [:] }
        
        // Group sets by muscle group
        let muscleGroupData = aggregateByMuscleGroup(workouts: workouts)
        
        // Calculate baseline volume per muscle (7-day rolling average)
        let baselineVolumes = calculateBaselineVolumes(muscleGroupData: muscleGroupData)
        
        // Calculate recovery status per muscle
        var recoveryMap: [String: MuscleRecoveryStatus] = [:]
        
        for (muscleName, data) in muscleGroupData {
            let status = calculateRecoveryStatus(
                muscleName: muscleName,
                trainingData: data,
                baselineVolume: baselineVolumes[muscleName] ?? 0,
                currentDate: currentDate
            )
            recoveryMap[muscleName] = status
        }
        
        Self.logger.info("Calculated recovery for \(recoveryMap.count) muscle groups")
        return recoveryMap
    }
    
    /// Get recovery status for a specific muscle group.
    public func getRecoveryStatus(
        for muscleName: String,
        workouts: [Workout],
        currentDate: Date = Date()
    ) -> MuscleRecoveryStatus? {
        let map = calculateRecoveryMap(workouts: workouts, currentDate: currentDate)
        return map[muscleName]
    }
    
    // MARK: - Internal Logic
    
    /// Aggregate all sets by muscle group with volume and time data.
    private func aggregateByMuscleGroup(workouts: [Workout]) -> [String: [MuscleTrainingData]] {
        var muscleData: [String: [MuscleTrainingData]] = [:]
        
        for workout in workouts {
            guard !workout.isCancelled else { continue }
            
            for set in workout.sets where !set.isWarmup {
                guard let exercise = set.exercise else { continue }
                
                for muscleGroup in exercise.muscleGroups {
                    let data = MuscleTrainingData(
                        date: workout.date,
                        volume: set.volume,
                        intensity: estimateIntensity(set: set),
                        isPrimary: muscleGroup.isPrimary
                    )
                    
                    muscleData[muscleGroup.name, default: []].append(data)
                }
            }
        }
        
        return muscleData
    }
    
    /// Estimate intensity as percentage (0-1) from RPE or reps.
    /// RPE 10 ≈ 100%, RPE 5 ≈ 50%. If no RPE, use reps (1 rep ≈ 100%, 12 reps ≈ 70%).
    private func estimateIntensity(set: ExerciseSet) -> Double {
        if let rpe = set.rpe {
            return min(max(rpe / 10.0, 0), 1)
        }
        
        // Estimate from reps using inverted relationship
        // 1-3 reps ≈ 90-100%, 8-12 reps ≈ 70-75%, 15+ reps ≈ 60%
        switch set.reps {
        case 1...3: return 0.95
        case 4...6: return 0.85
        case 7...9: return 0.75
        case 10...12: return 0.70
        default: return 0.60
        }
    }
    
    /// Calculate 7-day rolling baseline volume per muscle group.
    private func calculateBaselineVolumes(muscleGroupData: [String: [MuscleTrainingData]]) -> [String: Double] {
        var baselines: [String: Double] = [:]
        
        for (muscleName, data) in muscleGroupData {
            let totalVolume = data.reduce(0) { $0 + $1.volume }
            let uniqueDays = Set(data.map { Calendar.current.startOfDay(for: $0.date) }).count
            
            // Average daily volume over the period
            baselines[muscleName] = uniqueDays > 0 ? totalVolume / Double(uniqueDays) : totalVolume
        }
        
        return baselines
    }
    
    /// Calculate recovery status for a single muscle group.
    private func calculateRecoveryStatus(
        muscleName: String,
        trainingData: [MuscleTrainingData],
        baselineVolume: Double,
        currentDate: Date
    ) -> MuscleRecoveryStatus {
        guard !trainingData.isEmpty else {
            return MuscleRecoveryStatus(
                muscleName: muscleName,
                status: .fresh,
                hoursSinceLastTrained: nil,
                lastTrainedDate: nil,
                recentVolume: 0,
                recoveryPercentage: 100
            )
        }
        
        // Find most recent training session
        let sortedData = trainingData.sorted { $0.date > $1.date }
        guard let lastSession = sortedData.first else {
            return MuscleRecoveryStatus(
                muscleName: muscleName,
                status: .fresh,
                hoursSinceLastTrained: nil,
                lastTrainedDate: nil,
                recentVolume: 0,
                recoveryPercentage: 100
            )
        }
        
        let hoursSinceLastTrained = currentDate.timeIntervalSince(lastSession.date) / 3600
        
        // Recent volume (last 48h)
        let cutoff = currentDate.addingTimeInterval(-48 * 3600)
        let recentVolume = trainingData
            .filter { $0.date > cutoff }
            .reduce(0) { $0 + $1.volume }
        
        // Get base recovery time for this muscle
        let baseRecoveryHours = (Self.baseRecoveryTimes[muscleName] ?? Self.defaultRecoveryTime) / 3600
        
        // Adjust recovery time based on volume (high volume extends recovery)
        var adjustedRecoveryHours = baseRecoveryHours
        if baselineVolume > 0 && recentVolume > baselineVolume * Self.highVolumeMultiplier {
            adjustedRecoveryHours *= 1.3 // +30% recovery time for high volume
        }
        
        // Adjust for intensity (high intensity extends recovery)
        let avgIntensity = sortedData.prefix(3).map(\.intensity).reduce(0, +) / Double(min(3, sortedData.count))
        if avgIntensity > 0.85 {
            adjustedRecoveryHours *= 1.2 // +20% recovery time for high intensity
        }
        
        // Calculate recovery percentage (0-100)
        let recoveryPercentage = min(100, (hoursSinceLastTrained / adjustedRecoveryHours) * 100)
        
        // Determine status
        let status: RecoveryStatus
        if hoursSinceLastTrained >= adjustedRecoveryHours {
            status = .fresh
        } else if hoursSinceLastTrained >= adjustedRecoveryHours * 0.5 {
            status = .recovering
        } else {
            status = .fatigued
        }
        
        Self.logger.debug(
            "Muscle \(muscleName): \(status.rawValue), \(String(format: "%.1f", hoursSinceLastTrained))h since training, \(String(format: "%.0f", recoveryPercentage))% recovered"
        )
        
        return MuscleRecoveryStatus(
            muscleName: muscleName,
            status: status,
            hoursSinceLastTrained: hoursSinceLastTrained,
            lastTrainedDate: lastSession.date,
            recentVolume: recentVolume,
            recoveryPercentage: recoveryPercentage
        )
    }
}

// MARK: - Supporting Types

/// Recovery status classification.
public enum RecoveryStatus: String, Codable, Sendable {
    case fresh       // Fully recovered, ready for hard training
    case recovering  // Partial recovery, can train but monitor volume
    case fatigued    // Still recovering, avoid training this muscle
}

/// Recovery status for a single muscle group.
public struct MuscleRecoveryStatus: Codable, Sendable, Equatable {
    public let muscleName: String
    public let status: RecoveryStatus
    public let hoursSinceLastTrained: Double?
    public let lastTrainedDate: Date?
    public let recentVolume: Double
    public let recoveryPercentage: Double
    
    public init(
        muscleName: String,
        status: RecoveryStatus,
        hoursSinceLastTrained: Double?,
        lastTrainedDate: Date?,
        recentVolume: Double,
        recoveryPercentage: Double
    ) {
        self.muscleName = muscleName
        self.status = status
        self.hoursSinceLastTrained = hoursSinceLastTrained
        self.lastTrainedDate = lastTrainedDate
        self.recentVolume = recentVolume
        self.recoveryPercentage = recoveryPercentage
    }
    
    /// Color for UI heat map visualization.
    public var heatMapColor: String {
        switch status {
        case .fresh: return "green"
        case .recovering: return "yellow"
        case .fatigued: return "red"
        }
    }
    
    /// Short recommendation for UI.
    public var recommendation: String {
        switch status {
        case .fresh:
            return "Ready for training"
        case .recovering:
            return "Train with moderate volume"
        case .fatigued:
            return "Avoid heavy training"
        }
    }
}

/// Internal training data per muscle group.
private struct MuscleTrainingData: Sendable {
    let date: Date
    let volume: Double
    let intensity: Double
    let isPrimary: Bool
}
