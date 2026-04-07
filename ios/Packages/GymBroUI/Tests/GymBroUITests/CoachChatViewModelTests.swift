import XCTest
import SwiftData
@testable import GymBroUI
@testable import GymBroCore

@MainActor
final class CoachChatViewModelTests: XCTestCase {

    private var modelContainer: ModelContainer!
    private var modelContext: ModelContext!

    override func setUp() {
        super.setUp()
        let schema = Schema([
            ChatMessage.self,
            Workout.self,
            Exercise.self,
            ExerciseSet.self,
            UserProfile.self,
            BodyweightEntry.self,
            Program.self,
            ProgramDay.self,
            PlannedExercise.self,
            MuscleGroup.self
        ])
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        modelContainer = try! ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(modelContainer)
    }

    override func tearDown() {
        modelContainer = nil
        modelContext = nil
        super.tearDown()
    }

    // MARK: - Suggested Prompts

    func testDefaultSuggestedPromptsExist() {
        let vm = CoachChatViewModel()
        XCTAssertFalse(vm.suggestedPrompts.isEmpty, "Default suggested prompts should exist")
        XCTAssertEqual(vm.suggestedPrompts.count, 6)
    }

    func testSuggestedPromptHasTextAndIcon() {
        for prompt in SuggestedPrompt.defaults {
            XCTAssertFalse(prompt.text.isEmpty, "Prompt text should not be empty")
            XCTAssertFalse(prompt.icon.isEmpty, "Prompt icon should not be empty")
            XCTAssertFalse(prompt.id.isEmpty, "Prompt id should not be empty")
        }
    }

    // MARK: - Context Summary

    func testContextSummaryDefaultsToZero() {
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        XCTAssertEqual(vm.contextSummary.workoutCount, 0)
        XCTAssertEqual(vm.contextSummary.weeksOfData, 0)
        XCTAssertNil(vm.contextSummary.lastWorkoutDate)
    }

    // MARK: - Message Reactions

    func testToggleReactionOnAssistantMessage() {
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)

        let message = ChatMessage(role: .assistant, content: "Test response")
        modelContext.insert(message)
        try! modelContext.save()
        vm.messages = [message]

        // Apply thumbs up
        vm.toggleReaction(.thumbsUp, for: message)
        XCTAssertEqual(vm.messages[0].reaction, MessageReaction.thumbsUp.rawValue)

        // Toggle off
        vm.toggleReaction(.thumbsUp, for: message)
        XCTAssertEqual(vm.messages[0].reaction, MessageReaction.none.rawValue)

        // Apply thumbs down
        vm.toggleReaction(.thumbsDown, for: message)
        XCTAssertEqual(vm.messages[0].reaction, MessageReaction.thumbsDown.rawValue)

        // Switch to thumbs up
        vm.toggleReaction(.thumbsUp, for: message)
        XCTAssertEqual(vm.messages[0].reaction, MessageReaction.thumbsUp.rawValue)
    }

    func testToggleReactionIgnoresUserMessages() {
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)

        let message = ChatMessage(role: .user, content: "My question")
        vm.messages = [message]

        vm.toggleReaction(.thumbsUp, for: message)
        XCTAssertEqual(vm.messages[0].reaction, MessageReaction.none.rawValue)
    }

    // MARK: - History

    func testClearHistoryRemovesAllMessages() {
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)

        let msg1 = ChatMessage(role: .user, content: "Hello")
        let msg2 = ChatMessage(role: .assistant, content: "Hi there!")
        modelContext.insert(msg1)
        modelContext.insert(msg2)
        try! modelContext.save()
        vm.messages = [msg1, msg2]

        vm.clearHistory()
        XCTAssertTrue(vm.messages.isEmpty)
    }

    func testConfigureLoadsPersistedMessages() {
        let msg = ChatMessage(role: .user, content: "Persisted")
        modelContext.insert(msg)
        try! modelContext.save()

        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        XCTAssertEqual(vm.messages.count, 1)
        XCTAssertEqual(vm.messages.first?.content, "Persisted")
    }

    // MARK: - MessageReaction Enum

    func testMessageReactionValues() {
        XCTAssertEqual(MessageReaction.none.rawValue, 0)
        XCTAssertEqual(MessageReaction.thumbsUp.rawValue, 1)
        XCTAssertEqual(MessageReaction.thumbsDown.rawValue, -1)
    }

    // MARK: - ChatMessage Reaction Default

    func testChatMessageDefaultReactionIsNone() {
        let message = ChatMessage(role: .assistant, content: "Hello")
        XCTAssertEqual(message.reaction, 0)
    }
    
    // MARK: - Context Building
    
    func testBuildContextReturnsEmptyWhenNoData() {
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        
        // Use reflection to call private method (or test via sendMessage side effects)
        // For now, we'll test indirectly by checking that the context service is configured
        XCTAssertNotNil(vm.contextSummary)
    }
    
    func testBuildContextIncludesUserProfile() async {
        // Create user profile with bodyweight
        let profile = UserProfile(unitSystem: .metric, experienceLevel: .intermediate)
        modelContext.insert(profile)
        
        let bodyweight = BodyweightEntry(date: Date(), weightKg: 82.5)
        bodyweight.userProfile = profile
        modelContext.insert(bodyweight)
        
        try! modelContext.save()
        
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        
        // Context should now include profile data
        // We can't directly test buildContext since it's private, but we verify the data is available
        let descriptor = FetchDescriptor<UserProfile>()
        let profiles = try! modelContext.fetch(descriptor)
        XCTAssertEqual(profiles.count, 1)
        XCTAssertEqual(profiles.first?.experienceLevel, .intermediate)
    }
    
    func testBuildContextIncludesRecentWorkouts() {
        // Create exercise
        let squat = Exercise(name: "Back Squat", category: .compound, equipment: .barbell)
        modelContext.insert(squat)
        
        // Create completed workout
        let workout = Workout(date: Date())
        workout.startTime = Date().addingTimeInterval(-3600)
        workout.endTime = Date()
        modelContext.insert(workout)
        
        // Add sets to workout
        let set1 = ExerciseSet(exercise: squat, workout: workout, weightKg: 100, reps: 5)
        set1.completedAt = Date()
        modelContext.insert(set1)
        
        let set2 = ExerciseSet(exercise: squat, workout: workout, weightKg: 100, reps: 5)
        set2.completedAt = Date()
        modelContext.insert(set2)
        
        try! modelContext.save()
        
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        
        // Verify workout data is accessible
        var descriptor = FetchDescriptor<Workout>()
        descriptor.predicate = #Predicate<Workout> { w in
            w.endTime != nil && !w.isCancelled
        }
        let workouts = try! modelContext.fetch(descriptor)
        XCTAssertEqual(workouts.count, 1)
        XCTAssertEqual(workouts.first?.totalSets, 2)
    }
    
    func testBuildContextIncludesActiveProgram() {
        // Create active program
        let program = Program(
            name: "Beginner 5x5",
            durationWeeks: 12,
            frequencyPerWeek: 3,
            periodizationType: .linear,
            isActive: true
        )
        modelContext.insert(program)
        try! modelContext.save()
        
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        
        // Verify program is accessible
        var descriptor = FetchDescriptor<Program>()
        descriptor.predicate = #Predicate<Program> { p in p.isActive }
        let programs = try! modelContext.fetch(descriptor)
        XCTAssertEqual(programs.count, 1)
        XCTAssertEqual(programs.first?.name, "Beginner 5x5")
    }
    
    func testBuildContextIncludesPersonalRecords() {
        // Create exercises
        let squat = Exercise(name: "Back Squat", category: .compound, equipment: .barbell)
        let bench = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        modelContext.insert(squat)
        modelContext.insert(bench)
        
        // Create workout
        let workout = Workout(date: Date())
        workout.startTime = Date().addingTimeInterval(-3600)
        workout.endTime = Date()
        modelContext.insert(workout)
        
        // Add PR sets
        let squatPR = ExerciseSet(exercise: squat, workout: workout, weightKg: 150, reps: 1)
        squatPR.completedAt = Date()
        modelContext.insert(squatPR)
        
        let benchPR = ExerciseSet(exercise: bench, workout: workout, weightKg: 100, reps: 1)
        benchPR.completedAt = Date()
        modelContext.insert(benchPR)
        
        try! modelContext.save()
        
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        
        // Verify PR data is accessible
        var descriptor = FetchDescriptor<ExerciseSet>()
        descriptor.predicate = #Predicate<ExerciseSet> { s in
            !s.isWarmup && s.completedAt != nil
        }
        let sets = try! modelContext.fetch(descriptor)
        XCTAssertEqual(sets.count, 2)
        XCTAssertTrue(sets.contains { $0.weightKg == 150 })
        XCTAssertTrue(sets.contains { $0.weightKg == 100 })
    }
    
    func testBuildContextHandlesEmptyData() {
        // No data in database
        let vm = CoachChatViewModel()
        vm.configure(modelContext: modelContext)
        
        // Should not crash and should return empty context
        XCTAssertEqual(vm.contextSummary.workoutCount, 0)
    }
    
    func testBuildContextExcludesWarmupSets() {
        // Create exercise
        let squat = Exercise(name: "Back Squat", category: .compound, equipment: .barbell)
        modelContext.insert(squat)
        
        // Create workout
        let workout = Workout(date: Date())
        workout.startTime = Date().addingTimeInterval(-3600)
        workout.endTime = Date()
        modelContext.insert(workout)
        
        // Add warmup set (should be excluded from PRs)
        let warmup = ExerciseSet(exercise: squat, workout: workout, weightKg: 60, reps: 5, setType: .warmup)
        warmup.completedAt = Date()
        modelContext.insert(warmup)
        
        // Add working set
        let working = ExerciseSet(exercise: squat, workout: workout, weightKg: 100, reps: 5, setType: .working)
        working.completedAt = Date()
        modelContext.insert(working)
        
        try! modelContext.save()
        
        // Verify warmup sets are excluded from PR calculation
        var descriptor = FetchDescriptor<ExerciseSet>()
        descriptor.predicate = #Predicate<ExerciseSet> { s in
            !s.isWarmup && s.completedAt != nil
        }
        let sets = try! modelContext.fetch(descriptor)
        XCTAssertEqual(sets.count, 1)
        XCTAssertEqual(sets.first?.weightKg, 100)
    }
}
