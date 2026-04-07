import Foundation
import SwiftData
import os

/// Result of a PR check — which record types were broken and the values achieved.
public struct PRDetectionResult: Sendable {
    public let exerciseName: String
    public let recordTypes: [PersonalRecord.RecordType]
    public let weight: Double
    public let reps: Int
    public let e1rm: Double

    public var isPR: Bool { !recordTypes.isEmpty }

    /// Human-readable badge text for the highest-priority record type.
    public var primaryBadgeText: String {
        guard let primary = recordTypes.first else { return "" }
        switch primary {
        case .maxE1RM:   return "New 1RM!"
        case .maxWeight: return "Weight PR!"
        case .maxVolume: return "Volume Record!"
        case .maxReps:   return "Rep PR!"
        }
    }

    /// Short detail string shown below the badge.
    public var detailText: String {
        guard let primary = recordTypes.first else { return "" }
        switch primary {
        case .maxE1RM:
            return String(format: "%.1f kg estimated 1RM", e1rm)
        case .maxWeight:
            return String(format: "%.1f kg × %d", weight, reps)
        case .maxVolume:
            return String(format: "%.0f kg volume", weight * Double(reps))
        case .maxReps:
            return "\(reps) reps @ \(String(format: "%.1f", weight)) kg"
        }
    }
}

/// Checks whether a just-completed set is a new personal record.
/// Thin wrapper around PersonalRecordService with caching-friendly API.
public final class PRDetectionService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "PRDetection")

    private let personalRecordService: PersonalRecordService

    public init(modelContext: ModelContext) {
        self.personalRecordService = PersonalRecordService(modelContext: modelContext)
    }

    /// Check all PR categories for a just-completed set.
    public func checkForPR(set: ExerciseSet) -> PRDetectionResult {
        let exerciseName = set.exercise?.name ?? "Unknown"

        do {
            let recordTypes = try personalRecordService.getRecordTypes(for: set)
            // Sort by priority: e1RM > weight > volume > reps
            let sorted = recordTypes.sorted { priority($0) < priority($1) }
            return PRDetectionResult(
                exerciseName: exerciseName,
                recordTypes: sorted,
                weight: set.weightKg,
                reps: set.reps,
                e1rm: set.estimatedOneRepMax
            )
        } catch {
            Self.logger.error("PR detection failed: \(error.localizedDescription)")
            return PRDetectionResult(
                exerciseName: exerciseName,
                recordTypes: [],
                weight: set.weightKg,
                reps: set.reps,
                e1rm: set.estimatedOneRepMax
            )
        }
    }

    /// Fetch all-time PRs for an exercise, one per record type.
    public func getAllTimePRs(for exercise: Exercise) -> [PersonalRecord] {
        do {
            return try personalRecordService.getPersonalRecords(for: exercise)
        } catch {
            Self.logger.error("Failed to fetch all-time PRs: \(error.localizedDescription)")
            return []
        }
    }

    private func priority(_ type: PersonalRecord.RecordType) -> Int {
        switch type {
        case .maxE1RM:   return 0
        case .maxWeight: return 1
        case .maxVolume: return 2
        case .maxReps:   return 3
        }
    }
}
