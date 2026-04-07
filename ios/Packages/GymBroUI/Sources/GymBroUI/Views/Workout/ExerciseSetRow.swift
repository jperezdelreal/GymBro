import SwiftUI
import GymBroCore

public struct ExerciseSetRow: View {
    let set: ExerciseSet
    let setNumber: Int
    let unitSystem: UnitSystem
    let onTap: () -> Void

    @ScaledMetric(relativeTo: .title3) private var setNumberSize: CGFloat = 20
    @ScaledMetric(relativeTo: .body) private var valueSize: CGFloat = 18
    @ScaledMetric(relativeTo: .title3) private var checkSize: CGFloat = 24

    public init(
        set: ExerciseSet,
        setNumber: Int,
        unitSystem: UnitSystem = .metric,
        onTap: @escaping () -> Void = {}
    ) {
        self.set = set
        self.setNumber = setNumber
        self.unitSystem = unitSystem
        self.onTap = onTap
    }

    private var weightString: String {
        let weight = set.weightInUnit(unitSystem)
        let unit = unitSystem == .metric ? "kg" : "lb"
        return String(format: "%.1f %@", weight, unit)
    }

    public var body: some View {
        Button(action: onTap) {
            HStack(spacing: GymBroSpacing.md) {
                Text("\(setNumber)")
                    .font(GymBroTypography.monoNumber(size: setNumberSize))
                    .foregroundStyle(set.isWarmup ? GymBroColors.textTertiary : GymBroColors.textPrimary)
                    .frame(width: 40)

                VStack(alignment: .leading, spacing: 2) {
                    Text(weightString)
                        .font(.system(size: valueSize, weight: .semibold))
                        .foregroundStyle(set.completedAt != nil ? GymBroColors.textPrimary : GymBroColors.textSecondary)
                    Text("WEIGHT")
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .tracking(0.5)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .leading, spacing: 2) {
                    Text("\(set.reps)")
                        .font(.system(size: valueSize, weight: .semibold))
                        .foregroundStyle(set.completedAt != nil ? GymBroColors.textPrimary : GymBroColors.textSecondary)
                    Text("REPS")
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .tracking(0.5)
                }
                .frame(width: 60, alignment: .leading)

                if let rpe = set.rpe {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(format: "%.0f", rpe))
                            .font(.system(size: valueSize, weight: .semibold))
                            .foregroundStyle(GymBroColors.accentAmber)
                        Text("RPE")
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(GymBroColors.textTertiary)
                            .tracking(0.5)
                    }
                    .frame(width: 50, alignment: .leading)
                }

                if set.completedAt != nil {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: checkSize))
                        .foregroundStyle(GymBroColors.accentGreen)
                }
            }
            .padding(.vertical, GymBroSpacing.md)
            .padding(.horizontal, GymBroSpacing.md)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .fill(set.completedAt != nil
                        ? GymBroColors.accentGreen.opacity(0.08)
                        : GymBroColors.surfaceSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .strokeBorder(
                        set.isWarmup ? GymBroColors.accentAmber.opacity(0.3) : GymBroColors.border,
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}
