import Foundation
import CloudKit
import SwiftData
import os

/// Monitors CloudKit sync status and handles conflict resolution.
///
/// Architecture: The actual sync is handled by SwiftData's built-in CloudKit
/// integration via `ModelConfiguration(cloudKitDatabase:)`. This service monitors
/// that sync and provides status information to the UI layer.
///
/// Conflict resolution: last-writer-wins for simple fields. SwiftData+CloudKit
/// handles this automatically via NSPersistentCloudKitContainer. For workout sets,
/// property-level merging is deferred to v1.1 per retro decision.
@MainActor
@Observable
public final class CloudKitSyncService {

    private static let logger = Logger(subsystem: "com.gymbro", category: "CloudKitSync")

    // MARK: - Configuration

    public static let containerIdentifier = "iCloud.com.gymbro.app"

    // MARK: - Published State

    public private(set) var syncStatus: SyncStatus = .idle
    public private(set) var lastSyncDate: Date?
    public private(set) var iCloudAccountStatus: CKAccountStatus = .couldNotDetermine

    // MARK: - Private

    private var eventObserver: Any?
    private var accountObserver: Any?

    // MARK: - Init

    public init() {}

    // MARK: - Public API

    /// Checks the user's iCloud account status.
    public func checkAccountStatus() async {
        do {
            let container = CKContainer(identifier: Self.containerIdentifier)
            let status = try await container.accountStatus()
            iCloudAccountStatus = status

            switch status {
            case .available:
                Self.logger.info("iCloud account available")
                if syncStatus == .noAccount || syncStatus == .idle {
                    syncStatus = .idle
                }
            case .noAccount:
                Self.logger.info("No iCloud account")
                syncStatus = .noAccount
            case .restricted:
                Self.logger.info("iCloud account restricted")
                syncStatus = .error("iCloud access is restricted on this device.")
            case .couldNotDetermine:
                Self.logger.warning("Could not determine iCloud status")
                syncStatus = .error("Unable to determine iCloud account status.")
            case .temporarilyUnavailable:
                Self.logger.info("iCloud temporarily unavailable")
                syncStatus = .offline
            @unknown default:
                syncStatus = .error("Unknown iCloud status.")
            }
        } catch {
            Self.logger.error("Failed to check account status: \(error.localizedDescription)")
            syncStatus = .error(error.localizedDescription)
        }
    }

    /// Starts observing CloudKit sync events and iCloud account changes.
    public func startMonitoring() {
        // Observe NSPersistentCloudKitContainer event changes
        eventObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name("NSPersistentCloudKitContainer.eventChangedNotification"),
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor in
                self?.handleSyncEvent(notification)
            }
        }

        // Observe iCloud account changes
        accountObserver = NotificationCenter.default.addObserver(
            forName: .CKAccountChanged,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                await self?.checkAccountStatus()
            }
        }

        Self.logger.info("Started monitoring CloudKit sync events")
    }

    /// Stops observing CloudKit sync events.
    public func stopMonitoring() {
        if let observer = eventObserver {
            NotificationCenter.default.removeObserver(observer)
            eventObserver = nil
        }
        if let observer = accountObserver {
            NotificationCenter.default.removeObserver(observer)
            accountObserver = nil
        }
        Self.logger.info("Stopped monitoring CloudKit sync events")
    }

    /// Creates a CloudKit-enabled ModelConfiguration for use with SwiftData.
    /// When the user is signed in, this configures sync to CloudKit private database.
    /// When signed out, returns a local-only configuration.
    public static func makeModelConfiguration(isSignedIn: Bool) -> ModelConfiguration {
        if isSignedIn {
            return ModelConfiguration(
                "GymBro",
                cloudKitDatabase: .private(containerIdentifier)
            )
        } else {
            return ModelConfiguration(
                "GymBro",
                cloudKitDatabase: .none
            )
        }
    }

    // MARK: - Private

    private func handleSyncEvent(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let eventType = userInfo["type"] as? Int else {
            return
        }

        // NSPersistentCloudKitContainer event types: 0 = setup, 1 = import, 2 = export
        switch eventType {
        case 0:
            syncStatus = .syncing
            Self.logger.info("CloudKit sync: setup in progress")
        case 1:
            if let succeeded = userInfo["succeeded"] as? Bool, succeeded {
                syncStatus = .synced
                lastSyncDate = Date()
                Self.logger.info("CloudKit sync: import complete")
            } else {
                syncStatus = .syncing
            }
        case 2:
            if let succeeded = userInfo["succeeded"] as? Bool, succeeded {
                syncStatus = .synced
                lastSyncDate = Date()
                Self.logger.info("CloudKit sync: export complete")
            } else {
                syncStatus = .syncing
            }
        default:
            break
        }
    }
}

// MARK: - SyncStatus

/// Represents the current state of CloudKit synchronization.
public enum SyncStatus: Equatable, Sendable {
    case idle
    case syncing
    case synced
    case error(String)
    case noAccount
    case offline

    public var displayText: String {
        switch self {
        case .idle:
            return "Ready"
        case .syncing:
            return "Syncing…"
        case .synced:
            return "Up to date"
        case .error(let message):
            return "Error: \(message)"
        case .noAccount:
            return "No iCloud account"
        case .offline:
            return "Offline"
        }
    }

    public var systemImage: String {
        switch self {
        case .idle:
            return "icloud"
        case .syncing:
            return "arrow.triangle.2.circlepath.icloud"
        case .synced:
            return "checkmark.icloud"
        case .error:
            return "exclamationmark.icloud"
        case .noAccount:
            return "icloud.slash"
        case .offline:
            return "icloud.slash"
        }
    }
}
