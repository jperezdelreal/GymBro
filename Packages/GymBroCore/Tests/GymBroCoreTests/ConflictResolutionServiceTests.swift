import XCTest
@testable import GymBroCore

final class ConflictResolutionServiceTests: XCTestCase {

    // MARK: - UserProfile Property-Level Merge

    func testMergeUserProfiles_remoteNewerOverwritesFields() {
        let local = UserProfile(
            unitSystem: .metric,
            experienceLevel: .intermediate,
            defaultRestSeconds: 120
        )
        local.updatedAt = Date(timeIntervalSince1970: 1000)

        let remote = UserProfile(
            unitSystem: .imperial,
            experienceLevel: .advanced,
            defaultRestSeconds: 180
        )
        remote.updatedAt = Date(timeIntervalSince1970: 2000)

        let changed = ConflictResolutionService.mergeUserProfiles(local: local, remote: remote)

        XCTAssertEqual(local.unitSystem, .imperial)
        XCTAssertEqual(local.experienceLevel, .advanced)
        XCTAssertEqual(local.defaultRestSeconds, 180)
        XCTAssertEqual(changed.count, 3)
        XCTAssertTrue(changed.contains("unitSystem"))
        XCTAssertTrue(changed.contains("experienceLevel"))
        XCTAssertTrue(changed.contains("defaultRestSeconds"))
    }

    func testMergeUserProfiles_localNewerKeepsLocalValues() {
        let local = UserProfile(
            unitSystem: .imperial,
            experienceLevel: .advanced,
            defaultRestSeconds: 180
        )
        local.updatedAt = Date(timeIntervalSince1970: 3000)

        let remote = UserProfile(
            unitSystem: .metric,
            experienceLevel: .beginner,
            defaultRestSeconds: 60
        )
        remote.updatedAt = Date(timeIntervalSince1970: 1000)

        let changed = ConflictResolutionService.mergeUserProfiles(local: local, remote: remote)

        XCTAssertEqual(local.unitSystem, .imperial)
        XCTAssertEqual(local.experienceLevel, .advanced)
        XCTAssertEqual(local.defaultRestSeconds, 180)
        XCTAssertTrue(changed.isEmpty)
    }

    func testMergeUserProfiles_sameValuesNoChange() {
        let local = UserProfile(
            unitSystem: .metric,
            experienceLevel: .intermediate,
            defaultRestSeconds: 120
        )
        local.updatedAt = Date(timeIntervalSince1970: 1000)

        let remote = UserProfile(
            unitSystem: .metric,
            experienceLevel: .intermediate,
            defaultRestSeconds: 120
        )
        remote.updatedAt = Date(timeIntervalSince1970: 2000)

        let changed = ConflictResolutionService.mergeUserProfiles(local: local, remote: remote)

        XCTAssertTrue(changed.isEmpty)
    }

    func testMergeUserProfiles_partialDifference() {
        let local = UserProfile(
            unitSystem: .metric,
            experienceLevel: .intermediate,
            defaultRestSeconds: 120
        )
        local.updatedAt = Date(timeIntervalSince1970: 1000)

        let remote = UserProfile(
            unitSystem: .metric,
            experienceLevel: .elite,
            defaultRestSeconds: 120
        )
        remote.updatedAt = Date(timeIntervalSince1970: 2000)

        let changed = ConflictResolutionService.mergeUserProfiles(local: local, remote: remote)

        XCTAssertEqual(local.experienceLevel, .elite)
        XCTAssertEqual(changed, ["experienceLevel"])
    }

    func testMergeUserProfiles_updatesTimestamp() {
        let local = UserProfile()
        local.updatedAt = Date(timeIntervalSince1970: 1000)

        let remote = UserProfile()
        remote.updatedAt = Date(timeIntervalSince1970: 5000)

        _ = ConflictResolutionService.mergeUserProfiles(local: local, remote: remote)

        XCTAssertEqual(local.updatedAt, Date(timeIntervalSince1970: 5000))
    }

    // MARK: - Workout Set-Level Merge

    func testMergeWorkoutSets_addsUniqueSets() {
        let workout = Workout()

        let existingSet = ExerciseSet(weightKg: 100, reps: 5)
        existingSet.completedAt = Date(timeIntervalSince1970: 1000)
        workout.sets.append(existingSet)

        let remoteSnapshots = [
            ExerciseSetSnapshot(
                exerciseName: "Bench Press",
                weightKg: 110,
                reps: 3,
                completedAt: Date(timeIntervalSince1970: 2000)
            )
        ]

        let merged = ConflictResolutionService.mergeWorkoutSets(
            local: workout,
            remoteSets: remoteSnapshots
        )

        XCTAssertEqual(merged, 1)
        XCTAssertEqual(workout.sets.count, 2)
    }

    func testMergeWorkoutSets_deduplicatesByKey() {
        let workout = Workout()
        let exercise = Exercise(
            name: "Squat",
            category: .compound,
            equipment: .barbell
        )

        let existingSet = ExerciseSet(weightKg: 140, reps: 5)
        existingSet.exercise = exercise
        existingSet.completedAt = Date(timeIntervalSince1970: 1000)
        workout.sets.append(existingSet)

        let remoteSnapshots = [
            ExerciseSetSnapshot(
                exerciseName: "Squat",
                weightKg: 140,
                reps: 5,
                completedAt: Date(timeIntervalSince1970: 1000)
            )
        ]

        let merged = ConflictResolutionService.mergeWorkoutSets(
            local: workout,
            remoteSets: remoteSnapshots
        )

        XCTAssertEqual(merged, 0)
        XCTAssertEqual(workout.sets.count, 1)
    }

    func testMergeWorkoutSets_multipleNewSets() {
        let workout = Workout()

        let remoteSnapshots = [
            ExerciseSetSnapshot(
                exerciseName: "Deadlift",
                weightKg: 180,
                reps: 3,
                completedAt: Date(timeIntervalSince1970: 1000)
            ),
            ExerciseSetSnapshot(
                exerciseName: "Deadlift",
                weightKg: 180,
                reps: 3,
                completedAt: Date(timeIntervalSince1970: 1200)
            ),
            ExerciseSetSnapshot(
                exerciseName: "Deadlift",
                weightKg: 200,
                reps: 1,
                completedAt: Date(timeIntervalSince1970: 1400)
            )
        ]

        let merged = ConflictResolutionService.mergeWorkoutSets(
            local: workout,
            remoteSets: remoteSnapshots
        )

        XCTAssertEqual(merged, 3)
        XCTAssertEqual(workout.sets.count, 3)
    }

    func testMergeWorkoutSets_emptyRemote() {
        let workout = Workout()
        let existingSet = ExerciseSet(weightKg: 100, reps: 5)
        workout.sets.append(existingSet)

        let merged = ConflictResolutionService.mergeWorkoutSets(
            local: workout,
            remoteSets: []
        )

        XCTAssertEqual(merged, 0)
        XCTAssertEqual(workout.sets.count, 1)
    }

    func testMergeWorkoutSets_preservesSetProperties() {
        let workout = Workout()

        let snapshot = ExerciseSetSnapshot(
            exerciseName: "OHP",
            weightKg: 60,
            reps: 8,
            rpe: 7.5,
            restSeconds: 90,
            setType: .working,
            setNumber: 2,
            completedAt: Date(timeIntervalSince1970: 3000)
        )

        let merged = ConflictResolutionService.mergeWorkoutSets(
            local: workout,
            remoteSets: [snapshot]
        )

        XCTAssertEqual(merged, 1)
        let newSet = workout.sets.first!
        XCTAssertEqual(newSet.weightKg, 60)
        XCTAssertEqual(newSet.reps, 8)
        XCTAssertEqual(newSet.rpe, 7.5)
        XCTAssertEqual(newSet.restSeconds, 90)
        XCTAssertEqual(newSet.setType, .working)
        XCTAssertEqual(newSet.setNumber, 2)
        XCTAssertEqual(newSet.completedAt, Date(timeIntervalSince1970: 3000))
    }

    // MARK: - Deduplication Key

    func testDeduplicationKey_differentTimestampsAreDifferent() {
        let snapshot1 = ExerciseSetSnapshot(
            exerciseName: "Squat",
            weightKg: 140,
            reps: 5,
            completedAt: Date(timeIntervalSince1970: 1000)
        )

        let snapshot2 = ExerciseSetSnapshot(
            exerciseName: "Squat",
            weightKg: 140,
            reps: 5,
            completedAt: Date(timeIntervalSince1970: 1001)
        )

        let key1 = ConflictResolutionService.snapshotDeduplicationKey(for: snapshot1)
        let key2 = ConflictResolutionService.snapshotDeduplicationKey(for: snapshot2)

        XCTAssertNotEqual(key1, key2)
    }

    func testDeduplicationKey_sameDataSameKey() {
        let snapshot1 = ExerciseSetSnapshot(
            exerciseName: "Bench",
            weightKg: 100,
            reps: 5,
            completedAt: Date(timeIntervalSince1970: 5000)
        )

        let snapshot2 = ExerciseSetSnapshot(
            exerciseName: "Bench",
            weightKg: 100,
            reps: 5,
            completedAt: Date(timeIntervalSince1970: 5000)
        )

        let key1 = ConflictResolutionService.snapshotDeduplicationKey(for: snapshot1)
        let key2 = ConflictResolutionService.snapshotDeduplicationKey(for: snapshot2)

        XCTAssertEqual(key1, key2)
    }

    func testDeduplicationKey_differentWeightDifferentKey() {
        let snapshot1 = ExerciseSetSnapshot(
            exerciseName: "Bench",
            weightKg: 100,
            reps: 5,
            completedAt: Date(timeIntervalSince1970: 5000)
        )

        let snapshot2 = ExerciseSetSnapshot(
            exerciseName: "Bench",
            weightKg: 105,
            reps: 5,
            completedAt: Date(timeIntervalSince1970: 5000)
        )

        let key1 = ConflictResolutionService.snapshotDeduplicationKey(for: snapshot1)
        let key2 = ConflictResolutionService.snapshotDeduplicationKey(for: snapshot2)

        XCTAssertNotEqual(key1, key2)
    }

    // MARK: - ExerciseSetSnapshot

    func testExerciseSetSnapshot_initFromValues() {
        let snapshot = ExerciseSetSnapshot(
            exerciseName: "Squat",
            weightKg: 200,
            reps: 1,
            rpe: 10,
            restSeconds: 300,
            setType: .working,
            setNumber: 1,
            completedAt: Date()
        )

        XCTAssertEqual(snapshot.exerciseName, "Squat")
        XCTAssertEqual(snapshot.weightKg, 200)
        XCTAssertEqual(snapshot.reps, 1)
        XCTAssertEqual(snapshot.rpe, 10)
        XCTAssertEqual(snapshot.restSeconds, 300)
    }

    func testExerciseSetSnapshot_initFromExerciseSet() {
        let exercise = Exercise(name: "Deadlift", category: .compound, equipment: .barbell)
        let set = ExerciseSet(
            exercise: exercise,
            weightKg: 220,
            reps: 3,
            rpe: 8.5,
            restSeconds: 240,
            setType: .working,
            setNumber: 2
        )
        set.completedAt = Date(timeIntervalSince1970: 9000)

        let snapshot = ExerciseSetSnapshot(from: set)

        XCTAssertEqual(snapshot.exerciseName, "Deadlift")
        XCTAssertEqual(snapshot.weightKg, 220)
        XCTAssertEqual(snapshot.reps, 3)
        XCTAssertEqual(snapshot.rpe, 8.5)
        XCTAssertEqual(snapshot.restSeconds, 240)
        XCTAssertEqual(snapshot.setNumber, 2)
        XCTAssertEqual(snapshot.completedAt, Date(timeIntervalSince1970: 9000))
    }

    // MARK: - ConflictResolution Model

    func testConflictResolution_identifiable() {
        let resolution = ConflictResolution(
            entityType: "Workout",
            summary: "Merged 3 sets",
            fieldsChanged: ["sets"]
        )

        XCTAssertFalse(resolution.id.uuidString.isEmpty)
        XCTAssertEqual(resolution.entityType, "Workout")
        XCTAssertEqual(resolution.summary, "Merged 3 sets")
        XCTAssertEqual(resolution.fieldsChanged, ["sets"])
    }
}
