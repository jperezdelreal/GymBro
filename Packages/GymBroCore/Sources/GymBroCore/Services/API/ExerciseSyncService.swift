import Foundation
import SwiftData
import os

/// Orchestrates syncing exercises from wger.de API into the local SwiftData cache.
/// Offline-first: sync failures never block core functionality.
public actor ExerciseSyncService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ExerciseSync")
    private let apiService: WgerAPIService
    private let syncThrottleInterval: TimeInterval = 86_400 // 24 hours

    public init(apiService: WgerAPIService = WgerAPIService()) {
        self.apiService = apiService
    }

    // MARK: - Sync Orchestration

    /// Performs a full sync if the throttle window has elapsed.
    /// Returns the number of new exercises imported, or nil if sync was skipped/failed.
    @discardableResult
    public func syncIfNeeded(modelContext: ModelContext) async -> Int? {
        guard shouldSync() else {
            Self.logger.info("Sync throttled — last sync within 24 hours")
            return nil
        }

        do {
            let count = try await performSync(modelContext: modelContext)
            recordSyncTimestamp()
            Self.logger.info("Sync completed: \(count) new exercises imported")
            return count
        } catch {
            Self.logger.error("Sync failed (non-blocking): \(error.localizedDescription)")
            return nil
        }
    }

    /// Fetches all pages from wger.de and imports exercises that don't already exist locally.
    private func performSync(modelContext: ModelContext) async throws -> Int {
        let muscles = try await apiService.fetchMuscles()
        let equipment = try await apiService.fetchEquipment()

        var allExercises: [WgerExerciseData] = []
        var currentPage: Int? = 1

        while let page = currentPage {
            let (exercises, nextPage) = try await apiService.fetchExercises(page: page)
            allExercises.append(contentsOf: exercises)
            currentPage = nextPage

            // Rate-limit politeness: small delay between pages
            if nextPage != nil {
                try await Task.sleep(for: .milliseconds(300))
            }
        }

        Self.logger.info("Fetched \(allExercises.count) exercises across all pages")

        let existing = try fetchExistingExerciseNames(modelContext: modelContext)
        var importCount = 0

        for wgerExercise in allExercises {
            let normalizedName = wgerExercise.name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !normalizedName.isEmpty else { continue }

            // Deduplicate by name similarity (case-insensitive)
            if isDuplicate(name: normalizedName, existingNames: existing) {
                continue
            }

            let category = mapCategory(wgerCategoryId: wgerExercise.categoryId)
            let equip = mapEquipment(wgerEquipmentIds: wgerExercise.equipmentIds, equipmentLookup: equipment)
            let muscleGroups = mapMuscleGroups(
                primaryIds: wgerExercise.primaryMuscleIds,
                secondaryIds: wgerExercise.secondaryMuscleIds,
                muscleLookup: muscles
            )

            let exercise = Exercise(
                name: normalizedName,
                category: category,
                equipment: equip,
                instructions: stripHTML(wgerExercise.description),
                muscleGroups: muscleGroups,
                isCustom: false,
                source: .wger,
                wgerId: wgerExercise.wgerId
            )
            exercise.lastSyncedAt = Date()

            modelContext.insert(exercise)
            importCount += 1
        }

        if importCount > 0 {
            try modelContext.save()
        }

        return importCount
    }

    // MARK: - Throttle

    private static let lastSyncKey = "com.gymbro.wger.lastSync"

    private func shouldSync() -> Bool {
        guard let lastSync = UserDefaults.standard.object(forKey: Self.lastSyncKey) as? Date else {
            return true
        }
        return Date().timeIntervalSince(lastSync) >= syncThrottleInterval
    }

    private func recordSyncTimestamp() {
        UserDefaults.standard.set(Date(), forKey: Self.lastSyncKey)
    }

    // MARK: - Deduplication

    private func fetchExistingExerciseNames(modelContext: ModelContext) throws -> Set<String> {
        let descriptor = FetchDescriptor<Exercise>()
        let exercises = try modelContext.fetch(descriptor)
        return Set(exercises.map { $0.name.lowercased() })
    }

    private func isDuplicate(name: String, existingNames: Set<String>) -> Bool {
        let lower = name.lowercased()
        if existingNames.contains(lower) { return true }

        // Fuzzy match: check if a name is a substring of an existing one or vice versa
        // (e.g., "Bench Press" vs "Barbell Bench Press")
        for existing in existingNames {
            if existing.contains(lower) || lower.contains(existing) {
                if min(existing.count, lower.count) >= 5 {
                    return true
                }
            }
        }
        return false
    }

    // MARK: - wger → GymBro Mapping

    /// Maps wger category IDs to GymBro's ExerciseCategory.
    /// wger categories: 8=Arms, 9=Legs, 10=Abs, 11=Chest, 12=Back, 13=Shoulders, 14=Calves, 15=Cardio
    private func mapCategory(wgerCategoryId: Int) -> ExerciseCategory {
        switch wgerCategoryId {
        case 9, 11, 12:  // Legs, Chest, Back — typically multi-joint
            return .compound
        case 8, 10, 14:  // Arms, Abs, Calves — typically single-joint
            return .isolation
        default:
            return .accessory
        }
    }

    /// Maps wger equipment IDs to GymBro's Equipment enum.
    private func mapEquipment(wgerEquipmentIds: [Int], equipmentLookup: [WgerEquipmentData]) -> Equipment {
        guard let firstId = wgerEquipmentIds.first else { return .bodyweight }

        let name = equipmentLookup.first(where: { $0.id == firstId })?.name.lowercased() ?? ""

        if name.contains("barbell") { return .barbell }
        if name.contains("dumbbell") { return .dumbbell }
        if name.contains("kettlebell") { return .kettlebell }
        if name.contains("machine") || name.contains("bench") { return .machine }
        if name.contains("cable") || name.contains("pull-up") { return .cable }
        if name.contains("band") { return .band }
        if name.contains("body") || name.contains("none") { return .bodyweight }
        return .other
    }

    /// Maps wger muscle IDs to GymBro MuscleGroup models.
    /// wger muscles → GymBro's 19 muscle groups.
    private func mapMuscleGroups(
        primaryIds: [Int],
        secondaryIds: [Int],
        muscleLookup: [WgerMuscleData]
    ) -> [MuscleGroup] {
        var groups: [MuscleGroup] = []

        for id in primaryIds {
            if let name = resolveMuscleName(wgerId: id, lookup: muscleLookup) {
                groups.append(MuscleGroup(name: name, isPrimary: true))
            }
        }

        for id in secondaryIds {
            if let name = resolveMuscleName(wgerId: id, lookup: muscleLookup) {
                // Avoid duplicate entries
                if !groups.contains(where: { $0.name.lowercased() == name.lowercased() }) {
                    groups.append(MuscleGroup(name: name, isPrimary: false))
                }
            }
        }

        return groups
    }

    /// Maps wger muscle IDs to standardized GymBro muscle names.
    /// wger IDs: 1=Biceps, 2=Deltoids, 3=Chest, 4=Triceps, 5=Abs, 6=Calves,
    ///           7=Glutes, 8=Lats, 9=Hamstrings, 10=Quads, 11=Obliques,
    ///           12=Soleus, 13=Forearms, 14=Traps, 15=Serratus
    private static let wgerMuscleMap: [Int: String] = [
        1: "Biceps",
        2: "Shoulders",
        3: "Chest",
        4: "Triceps",
        5: "Abs",
        6: "Calves",
        7: "Glutes",
        8: "Lats",
        9: "Hamstrings",
        10: "Quads",
        11: "Obliques",
        12: "Calves",
        13: "Forearms",
        14: "Traps",
        15: "Chest"
    ]

    private func resolveMuscleName(wgerId: Int, lookup: [WgerMuscleData]) -> String? {
        if let mapped = Self.wgerMuscleMap[wgerId] {
            return mapped
        }
        // Fallback: use name from API response
        return lookup.first(where: { $0.id == wgerId })?.name
    }

    // MARK: - Utility

    /// Strips HTML tags from wger exercise descriptions.
    private func stripHTML(_ html: String) -> String {
        guard !html.isEmpty else { return "" }
        var result = html
        // Remove HTML tags
        while let range = result.range(of: "<[^>]+>", options: .regularExpression) {
            result.replaceSubrange(range, with: "")
        }
        // Decode common HTML entities
        result = result
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&nbsp;", with: " ")
        return result.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
