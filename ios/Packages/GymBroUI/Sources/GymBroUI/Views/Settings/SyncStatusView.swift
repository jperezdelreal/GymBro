import SwiftUI
import GymBroCore

/// Displays CloudKit sync status in the settings/profile screen.
public struct SyncStatusView: View {
    private let syncService: CloudKitSyncService

    public init(syncService: CloudKitSyncService) {
        self.syncService = syncService
    }

    public var body: some View {
        HStack {
            Image(systemName: syncService.syncStatus.systemImage)
                .foregroundStyle(statusColor)
                .symbolEffect(.pulse, isActive: syncService.syncStatus == .syncing)

            VStack(alignment: .leading, spacing: 2) {
                Text(syncService.syncStatus.displayText)
                    .font(.subheadline)

                if let lastSync = syncService.lastSyncDate {
                    Text("Last synced: \(lastSync, style: .relative) ago")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .task {
            await syncService.checkAccountStatus()
        }
    }

    private var statusColor: Color {
        switch syncService.syncStatus {
        case .synced:
            return .green
        case .syncing:
            return .blue
        case .error:
            return .red
        case .noAccount, .offline:
            return .orange
        case .idle:
            return .secondary
        }
    }
}
