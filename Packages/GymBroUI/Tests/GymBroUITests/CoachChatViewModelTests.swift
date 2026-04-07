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
        let schema = Schema([ChatMessage.self, Workout.self, Exercise.self, ExerciseSet.self])
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
}
