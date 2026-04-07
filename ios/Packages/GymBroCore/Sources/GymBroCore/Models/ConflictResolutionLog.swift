import Foundation
import SwiftData

/// Records each conflict resolution for debugging and user transparency.
@Model
public final class ConflictResolutionLog {
    public var id: UUID
    public var timestamp: Date
    public var entityType: String
    public var entityID: String
    public var strategy: String
    public var fieldsResolved: String
    public var summary: String

    public init(
        id: UUID = UUID(),
        entityType: String,
        entityID: String,
        strategy: String,
        fieldsResolved: String = "",
        summary: String
    ) {
        self.id = id
        self.timestamp = Date()
        self.entityType = entityType
        self.entityID = entityID
        self.strategy = strategy
        self.fieldsResolved = fieldsResolved
        self.summary = summary
    }
}

/// Lightweight struct surfaced to the UI when a conflict is resolved.
public struct ConflictResolution: Identifiable, Sendable {
    public let id: UUID
    public let timestamp: Date
    public let entityType: String
    public let summary: String
    public let fieldsChanged: [String]

    public init(
        id: UUID = UUID(),
        timestamp: Date = Date(),
        entityType: String,
        summary: String,
        fieldsChanged: [String]
    ) {
        self.id = id
        self.timestamp = timestamp
        self.entityType = entityType
        self.summary = summary
        self.fieldsChanged = fieldsChanged
    }
}
