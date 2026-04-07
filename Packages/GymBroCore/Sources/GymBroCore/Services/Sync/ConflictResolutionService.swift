import Foundation
import SwiftData
import os

/// Handles SwiftData/CloudKit merge conflicts with property-level merging
/// for UserProfile and set-level merging for Workout data.
///
/// Architecture: SwiftData's built-in CloudKit integration uses last-writer-wins.
/// This service intercepts persistent-store remote-change notifications, detects
/// conflicting records, and applies smarter merge strategies before autosave commits.
@MainActor
@Observable
public final class ConflictResolutionService {

    private static let logger = Logger(subsystem: "com.gymbro", category: "ConflictResolution")

    // MARK: - Published State

    /// Recent conflict resolutions shown in the UI.
    public private(set) var recentResolutions: [ConflictResolution] = []

    /// Total conflicts resolved this session.
    public private(set) var conflictsResolvedCount: Int = 0

    // MARK: - Private

    private var remoteChangeObserver: Any?
    private weak var modelContext: ModelContext?

    // MARK: - Init

    public init() {}

    // MARK: - Public API

    /// Begin listening for remote-change notifications and resolving conflicts.
    public func startMonitoring(modelContext: ModelContext) {
        self.modelContext = modelContext

        remoteChangeObserver = NotificationCenter.default.addObserver(
            forName: .NSPersistentStoreRemoteChange,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor in
                self?.handleRemoteChange(notification)
            }
        }

        Self.logger.info("Started conflict resolution monitoring")
    }

    /// Stop listening for remote changes.
    public func stopMonitoring() {
        if let observer = remoteChangeObserver {
            NotificationCenter.default.removeObserver(observer)
            remoteChangeObserver = nil
        }
        Self.logger.info("Stopped conflict resolution monitoring")
    }

    /// Clears UI-facing resolution history.
    public func clearRecentResolutions() {
        recentResolutions.removeAll()
    }

    // MARK: - Merge Logic (internal for testing)

    /// Property-level merge for UserProfile: keeps the most recently updated
    /// non-default value for each field from either the local or remote version.
    ///
    /// Returns a list of field names that were changed.
    static func mergeUserProfiles(
        local: UserProfile,
        remote: UserProfile
    ) -> [String] {
        var changed: [String] = []

        // Prefer whichever side was updated more recently per-field.
        // Since SwiftData doesn't track per-field timestamps, we use
        // the overall updatedAt as a proxy and merge non-default values
        // from the remote if the remote is newer.
        let remoteIsNewer = remote.updatedAt > local.updatedAt

        if remoteIsNewer {
            if remote.unitSystem != local.unitSystem {
                local.unitSystem = remote.unitSystem
                changed.append("unitSystem")
            }
            if remote.experienceLevel != local.experienceLevel {
                local.experienceLevel = remote.experienceLevel
                changed.append("experienceLevel")
            }
            if remote.defaultRestSeconds != local.defaultRestSeconds {
                local.defaultRestSeconds = remote.defaultRestSeconds
                changed.append("defaultRestSeconds")
            }
        }

        // Always take the latest updatedAt
        if remote.updatedAt > local.updatedAt {
            local.updatedAt = remote.updatedAt
        }

        return changed
    }

    /// Set-level merge for Workout data: takes the union of ExerciseSets from
    /// both devices, deduplicating by exercise-name + completedAt timestamp.
    ///
    /// Returns the number of new sets merged in.
    static func mergeWorkoutSets(
        local: Workout,
        remoteSets: [ExerciseSetSnapshot]
    ) -> Int {
        let localKeys = Set(local.sets.map { setDeduplicationKey(for: $0) })
        var merged = 0

        for remoteSet in remoteSets {
            let key = snapshotDeduplicationKey(for: remoteSet)
            if !localKeys.contains(key) {
                let newSet = ExerciseSet(
                    id: remoteSet.id,
                    weightKg: remoteSet.weightKg,
                    reps: remoteSet.reps,
                    rpe: remoteSet.rpe,
                    restSeconds: remoteSet.restSeconds,
                    setType: remoteSet.setType,
                    setNumber: remoteSet.setNumber
                )
                newSet.completedAt = remoteSet.completedAt
                newSet.workout = local
                local.sets.append(newSet)
                merged += 1
            }
        }

        if merged > 0 {
            local.updatedAt = Date()
        }

        return merged
    }

    // MARK: - Deduplication Keys

    /// Creates a deduplication key from an ExerciseSet: exercise name + completedAt + weight + reps.
    static func setDeduplicationKey(for set: ExerciseSet) -> String {
        let exerciseName = set.exercise?.name ?? "unknown"
        let completedAt = set.completedAt?.timeIntervalSince1970 ?? 0
        return "\(exerciseName)|\(completedAt)|\(set.weightKg)|\(set.reps)"
    }

    /// Creates a deduplication key from a snapshot.
    static func snapshotDeduplicationKey(for snapshot: ExerciseSetSnapshot) -> String {
        let completedAt = snapshot.completedAt?.timeIntervalSince1970 ?? 0
        return "\(snapshot.exerciseName)|\(completedAt)|\(snapshot.weightKg)|\(snapshot.reps)"
    }

    // MARK: - Private

    private func handleRemoteChange(_ notification: Notification) {
        guard let context = modelContext else { return }

        Self.logger.info("Remote change detected - checking for conflicts")

        do {
            try mergeUserProfilesIfNeeded(context: context)
            try mergeActiveWorkoutsIfNeeded(context: context)
        } catch {
            Self.logger.error("Conflict resolution failed: \(error.localizedDescription)")
        }
    }

    private func mergeUserProfilesIfNeeded(context: ModelContext) throws {
        let descriptor = FetchDescriptor<UserProfile>()
        let profiles = try context.fetch(descriptor)

        // If multiple profiles exist from different devices, merge into one
        guard profiles.count > 1 else { return }

        let primary = profiles.sorted(by: { $0.createdAt < $1.createdAt }).first!
        let duplicates = profiles.filter { $0.id != primary.id }

        for duplicate in duplicates {
            let changed = Self.mergeUserProfiles(local: primary, remote: duplicate)

            if !changed.isEmpty {
                let resolution = ConflictResolution(
                    entityType: "UserProfile",
                    summary: "Merged profile settings from another device",
                    fieldsChanged: changed
                )
                recentResolutions.insert(resolution, at: 0)
                conflictsResolvedCount += 1

                logResolution(
                    context: context,
                    entityType: "UserProfile",
                    entityID: primary.id.uuidString,
                    strategy: "property-level-merge",
                    fields: changed,
                    summary: "Merged \(changed.count) field(s): \(changed.joined(separator: ", "))"
                )

                Self.logger.info("Merged UserProfile fields: \(changed.joined(separator: ", "))")
            }

            // Migrate bodyweight history before deleting
            for entry in duplicate.bodyweightHistory {
                entry.userProfile = primary
            }
            context.delete(duplicate)
        }
    }

    private func mergeActiveWorkoutsIfNeeded(context: ModelContext) throws {
        let descriptor = FetchDescriptor<Workout>(
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        let workouts = try context.fetch(descriptor)

        // Group workouts by UUID to find duplicates from CloudKit merge
        var workoutsByID: [UUID: [Workout]] = [:]
        for workout in workouts {
            workoutsByID[workout.id, default: []].append(workout)
        }

        for (workoutID, duplicates) in workoutsByID where duplicates.count > 1 {
            let primary = duplicates.sorted(by: { $0.createdAt < $1.createdAt }).first!
            let others = duplicates.filter { $0 !== primary }

            for other in others {
                let snapshots = other.sets.map { ExerciseSetSnapshot(from: $0) }
                let mergedCount = Self.mergeWorkoutSets(local: primary, remoteSets: snapshots)

                if mergedCount > 0 {
                    let resolution = ConflictResolution(
                        entityType: "Workout",
                        summary: "Merged \(mergedCount) set(s) from another device",
                        fieldsChanged: ["sets"]
                    )
                    recentResolutions.insert(resolution, at: 0)
                    conflictsResolvedCount += 1

                    logResolution(
                        context: context,
                        entityType: "Workout",
                        entityID: workoutID.uuidString,
                        strategy: "set-level-merge",
                        fields: ["sets(\(mergedCount))"],
                        summary: "Merged \(mergedCount) set(s) into workout"
                    )

                    Self.logger.info("Merged \(mergedCount) sets into workout \(workoutID)")
                }

                // Transfer notes if the other copy has more content
                if other.notes.count > primary.notes.count {
                    primary.notes = other.notes
                }

                // Keep the earliest start time
                if let otherStart = other.startTime {
                    if primary.startTime == nil || otherStart < primary.startTime! {
                        primary.startTime = otherStart
                    }
                }

                // Keep the latest end time
                if let otherEnd = other.endTime {
                    if primary.endTime == nil || otherEnd > primary.endTime! {
                        primary.endTime = otherEnd
                    }
                }

                context.delete(other)
            }
        }

        // Cap the recent resolutions list at 50
        if recentResolutions.count > 50 {
            recentResolutions = Array(recentResolutions.prefix(50))
        }
    }

    private func logResolution(
        context: ModelContext,
        entityType: String,
        entityID: String,
        strategy: String,
        fields: [String],
        summary: String
    ) {
        let log = ConflictResolutionLog(
            entityType: entityType,
            entityID: entityID,
            strategy: strategy,
            fieldsResolved: fields.joined(separator: ","),
            summary: summary
        )
        context.insert(log)
    }
}

// MARK: - ExerciseSetSnapshot

/// Lightweight snapshot of an ExerciseSet for merge operations.
/// Avoids passing @Model objects across contexts.
public struct ExerciseSetSnapshot: Sendable {
    public let id: UUID
    public let exerciseName: String
    public let weightKg: Double
    public let reps: Int
    public let rpe: Double?
    public let restSeconds: Int
    public let setType: SetType
    public let setNumber: Int
    public let completedAt: Date?

    public init(from set: ExerciseSet) {
        self.id = set.id
        self.exerciseName = set.exercise?.name ?? "unknown"
        self.weightKg = set.weightKg
        self.reps = set.reps
        self.rpe = set.rpe
        self.restSeconds = set.restSeconds
        self.setType = set.setType
        self.setNumber = set.setNumber
        self.completedAt = set.completedAt
    }

    public init(
        id: UUID = UUID(),
        exerciseName: String,
        weightKg: Double,
        reps: Int,
        rpe: Double? = nil,
        restSeconds: Int = 120,
        setType: SetType = .working,
        setNumber: Int = 1,
        completedAt: Date? = nil
    ) {
        self.id = id
        self.exerciseName = exerciseName
        self.weightKg = weightKg
        self.reps = reps
        self.rpe = rpe
        self.restSeconds = restSeconds
        self.setType = setType
        self.setNumber = setNumber
        self.completedAt = completedAt
    }
}
