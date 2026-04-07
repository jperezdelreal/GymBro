import SwiftUI
import GymBroCore

/// Displays recent conflict resolutions so users know what changed during sync.
public struct ConflictResolutionView: View {
    private let conflictService: ConflictResolutionService

    public init(conflictService: ConflictResolutionService) {
        self.conflictService = conflictService
    }

    public var body: some View {
        Group {
            if conflictService.recentResolutions.isEmpty {
                emptyState
            } else {
                resolutionList
            }
        }
        .navigationTitle("Sync Conflicts")
    }

    // MARK: - Subviews

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: GymBroSpacing.md) {
            Image(systemName: "checkmark.icloud")
                .font(.system(size: 48))
                .foregroundStyle(GymBroColors.accentGreen)

            Text("No Conflicts")
                .font(GymBroTypography.title2)
                .foregroundStyle(GymBroColors.textPrimary)

            Text("All your data is in sync across devices.")
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(GymBroSpacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private var resolutionList: some View {
        List {
            Section {
                HStack {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .foregroundStyle(GymBroColors.accentCyan)
                    Text("\(conflictService.conflictsResolvedCount) conflict(s) resolved")
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textSecondary)
                    Spacer()
                    Button("Clear") {
                        conflictService.clearRecentResolutions()
                    }
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.accentGreen)
                }
            }

            Section("Recent") {
                ForEach(conflictService.recentResolutions) { resolution in
                    resolutionRow(resolution)
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    @ViewBuilder
    private func resolutionRow(_ resolution: ConflictResolution) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
            HStack {
                Image(systemName: iconForEntityType(resolution.entityType))
                    .foregroundStyle(GymBroColors.accentAmber)
                Text(resolution.entityType)
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.textPrimary)
                Spacer()
                Text(resolution.timestamp, style: .relative)
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
            }

            Text(resolution.summary)
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)

            if !resolution.fieldsChanged.isEmpty {
                HStack(spacing: GymBroSpacing.xs) {
                    ForEach(resolution.fieldsChanged, id: \.self) { field in
                        Text(field)
                            .font(GymBroTypography.caption2)
                            .padding(.horizontal, GymBroSpacing.sm)
                            .padding(.vertical, 2)
                            .background(GymBroColors.surfaceElevated)
                            .clipShape(Capsule())
                            .foregroundStyle(GymBroColors.accentCyan)
                    }
                }
            }
        }
        .padding(.vertical, GymBroSpacing.xs)
    }

    // MARK: - Helpers

    private func iconForEntityType(_ type: String) -> String {
        switch type {
        case "UserProfile":
            return "person.circle"
        case "Workout":
            return "figure.strengthtraining.traditional"
        default:
            return "doc.circle"
        }
    }
}
