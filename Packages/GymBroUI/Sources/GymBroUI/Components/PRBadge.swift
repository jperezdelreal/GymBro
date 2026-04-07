import SwiftUI
import GymBroCore

/// Small inline badge indicating a set is a personal record.
/// Shown next to the checkmark on ExerciseSetRow.
public struct PRBadge: View {
    let recordTypes: [PersonalRecord.RecordType]

    @ScaledMetric(relativeTo: .caption2) private var iconSize: CGFloat = 10

    public init(recordTypes: [PersonalRecord.RecordType]) {
        self.recordTypes = recordTypes
    }

    public var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "trophy.fill")
                .font(.system(size: iconSize))
            Text(badgeLabel)
                .font(GymBroTypography.caption2)
        }
        .foregroundStyle(GymBroColors.accentAmber)
        .padding(.horizontal, GymBroSpacing.sm)
        .padding(.vertical, 3)
        .background(
            Capsule().fill(GymBroColors.accentAmber.opacity(0.15))
        )
        .accessibilityLabel("Personal record: \(badgeLabel)")
    }

    private var badgeLabel: String {
        guard let primary = recordTypes.first else { return "PR" }
        switch primary {
        case .maxE1RM:   return "1RM"
        case .maxWeight: return "Weight PR"
        case .maxVolume: return "Volume PR"
        case .maxReps:   return "Rep PR"
        }
    }
}

// MARK: - Preview

#Preview("PR Badges") {
    VStack(spacing: GymBroSpacing.md) {
        PRBadge(recordTypes: [.maxE1RM])
        PRBadge(recordTypes: [.maxWeight])
        PRBadge(recordTypes: [.maxVolume])
        PRBadge(recordTypes: [.maxReps])
        PRBadge(recordTypes: [.maxE1RM, .maxWeight, .maxReps])
    }
    .padding()
    .gymBroDarkBackground()
}
