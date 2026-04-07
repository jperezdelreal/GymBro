import SwiftUI

/// Trend direction for hero number displays.
public enum HeroTrend: Sendable {
    case up
    case down
    case flat

    var symbol: String {
        switch self {
        case .up: return "↑"
        case .down: return "↓"
        case .flat: return "→"
        }
    }

    var color: Color {
        switch self {
        case .up: return GymBroColors.accentGreen
        case .down: return GymBroColors.accentRed
        case .flat: return GymBroColors.textTertiary
        }
    }

    var accessibilityLabel: String {
        switch self {
        case .up: return "trending up"
        case .down: return "trending down"
        case .flat: return "no change"
        }
    }
}

/// Large stat display — weight/reps in 72pt+ SF Pro Heavy, monospaced.
public struct HeroNumber: View {
    let value: String
    let unit: String
    let trend: HeroTrend?

    public init(value: String, unit: String, trend: HeroTrend? = nil) {
        self.value = value
        self.unit = unit
        self.trend = trend
    }

    /// Convenience initializer for numeric values
    public init(value: Double, format: String = "%.1f", unit: String, trend: HeroTrend? = nil) {
        self.value = String(format: format, value)
        self.unit = unit
        self.trend = trend
    }

    /// Convenience initializer for integer values
    public init(value: Int, unit: String, trend: HeroTrend? = nil) {
        self.value = "\(value)"
        self.unit = unit
        self.trend = trend
    }

    public var body: some View {
        VStack(spacing: GymBroSpacing.xs) {
            HStack(alignment: .firstTextBaseline, spacing: GymBroSpacing.sm) {
                Text(value)
                    .font(GymBroTypography.heroNumber)
                    .foregroundStyle(GymBroColors.textPrimary)
                    .contentTransition(.numericText())

                if let trend {
                    Text(trend.symbol)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(trend.color)
                        .accessibilityLabel(trend.accessibilityLabel)
                }
            }

            Text(unit.uppercased())
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(value) \(unit)")
    }
}

// MARK: - Previews

#Preview("Hero Numbers") {
    VStack(spacing: GymBroSpacing.xxl) {
        HeroNumber(value: 140.0, format: "%.1f", unit: "kg", trend: .up)

        HeroNumber(value: 8, unit: "reps", trend: .flat)

        HeroNumber(value: "225", unit: "lb", trend: .down)

        HeroNumber(value: "1:45", unit: "rest")

        HStack(spacing: GymBroSpacing.xl) {
            VStack {
                HeroNumber(value: 12, unit: "sets")
                Text("Today")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textSecondary)
            }
            VStack {
                HeroNumber(value: 4520.0, format: "%.0f", unit: "kg vol", trend: .up)
                Text("Volume")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textSecondary)
            }
        }
    }
    .padding(GymBroSpacing.lg)
    .gymBroDarkBackground()
}
