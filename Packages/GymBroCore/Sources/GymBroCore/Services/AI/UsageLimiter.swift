import Foundation

/// Manages free-tier usage limits for AI coach.
/// Free users get 5 questions per week; premium users are unlimited.
public final class UsageLimiter {

    private let freeWeeklyLimit = 5
    private let storageKey = "ai_coach_usage"

    public init() {}

    public struct UsageRecord: Codable {
        public var weekStart: Date
        public var count: Int
    }

    // MARK: - Public API

    public func canSendMessage(isPremium: Bool) -> Bool {
        if isPremium { return true }
        let record = loadRecord()
        return record.count < freeWeeklyLimit
    }

    public func remainingMessages(isPremium: Bool) -> Int {
        if isPremium { return .max }
        let record = loadRecord()
        return max(0, freeWeeklyLimit - record.count)
    }

    public func recordUsage() {
        var record = loadRecord()
        record.count += 1
        saveRecord(record)
    }

    // MARK: - Persistence (UserDefaults)

    private func loadRecord() -> UsageRecord {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let record = try? JSONDecoder().decode(UsageRecord.self, from: data) else {
            return resetRecord()
        }

        // Reset if we're in a new week
        if !Calendar.current.isDate(record.weekStart, equalTo: Date(), toGranularity: .weekOfYear) {
            return resetRecord()
        }

        return record
    }

    private func resetRecord() -> UsageRecord {
        let record = UsageRecord(
            weekStart: Calendar.current.startOfDay(for: Date()),
            count: 0
        )
        saveRecord(record)
        return record
    }

    private func saveRecord(_ record: UsageRecord) {
        if let data = try? JSONEncoder().encode(record) {
            UserDefaults.standard.set(data, forKey: storageKey)
        }
    }
}
