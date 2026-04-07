import SwiftUI

// MARK: - Color Palette

public enum GymBroColors {
    // Backgrounds — deep blacks for that premium gym aesthetic
    public static let background = Color(hex: 0x0A0A0A)
    public static let surfacePrimary = Color(hex: 0x141414)
    public static let surfaceSecondary = Color(hex: 0x1C1C1E)
    public static let surfaceElevated = Color(hex: 0x2C2C2E)

    // Borders
    public static let border = Color(hex: 0x2C2C2E)
    public static let borderSubtle = Color(hex: 0x3A3A3C)

    // Accents — electric highlights
    public static let accentGreen = Color(hex: 0x00FF87)
    public static let accentAmber = Color(hex: 0xFFB800)
    public static let accentRed = Color(hex: 0xFF3B30)
    public static let accentCyan = Color(hex: 0x00D4FF)

    // Text
    public static let textPrimary = Color.white
    public static let textSecondary = Color.white.opacity(0.6)
    public static let textTertiary = Color.white.opacity(0.35)

    // Gradients
    public static let greenGradient = LinearGradient(
        colors: [Color(hex: 0x00FF87), Color(hex: 0x00CC6A)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    public static let surfaceGradient = LinearGradient(
        colors: [Color(hex: 0x141414), Color(hex: 0x0A0A0A)],
        startPoint: .top,
        endPoint: .bottom
    )

    public static let amberGradient = LinearGradient(
        colors: [Color(hex: 0xFFB800), Color(hex: 0xFF8C00)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

// MARK: - Color Extension

extension Color {
    init(hex: UInt, opacity: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: opacity
        )
    }
}

// MARK: - Typography

public enum GymBroTypography {
    /// 72pt+ SF Pro Heavy, monospaced digits — for hero weight/rep displays
    public static let heroNumber = Font.system(size: 72, weight: .heavy, design: .rounded)
        .monospacedDigit()

    /// Large title — section headers, big callouts
    public static let largeTitle = Font.largeTitle.weight(.bold)

    /// Title — exercise names, card headers
    public static let title = Font.title.weight(.bold)

    /// Title2 — secondary headers
    public static let title2 = Font.title2.weight(.semibold)

    /// Title3 — stat values
    public static let title3 = Font.title3.weight(.semibold)

    /// Headline — buttons, emphasis
    public static let headline = Font.headline.weight(.semibold)

    /// Body — default content
    public static let body = Font.body

    /// Subheadline
    public static let subheadline = Font.subheadline

    /// Caption — labels, secondary info
    public static let caption = Font.caption

    /// Caption2 — smallest labels, units
    public static let caption2 = Font.caption2

    /// Monospaced digits for numeric displays
    public static func monoNumber(size: CGFloat, weight: Font.Weight = .bold) -> Font {
        .system(size: size, weight: weight, design: .rounded).monospacedDigit()
    }
}

// MARK: - Spacing Tokens

public enum GymBroSpacing {
    public static let xs: CGFloat = 4
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 16
    public static let lg: CGFloat = 24
    public static let xl: CGFloat = 32
    public static let xxl: CGFloat = 48
}

// MARK: - Corner Radii

public enum GymBroRadius {
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 12
    public static let lg: CGFloat = 16
    public static let xl: CGFloat = 24
}

// MARK: - Shadows

public enum GymBroShadow {
    public static func card(_ color: Color = .black) -> some View {
        EmptyView()
            .shadow(color: color.opacity(0.3), radius: 8, x: 0, y: 4)
    }

    public static let cardRadius: CGFloat = 8
    public static let cardOpacity: Double = 0.3
    public static let cardY: CGFloat = 4

    public static let elevatedRadius: CGFloat = 16
    public static let elevatedOpacity: Double = 0.4
    public static let elevatedY: CGFloat = 8
}

// MARK: - View Modifiers

public struct GymBroDarkBackground: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .background(GymBroColors.background.ignoresSafeArea())
            .preferredColorScheme(.dark)
    }
}

public extension View {
    func gymBroDarkBackground() -> some View {
        modifier(GymBroDarkBackground())
    }

    func gymBroCardShadow() -> some View {
        self.shadow(
            color: .black.opacity(GymBroShadow.cardOpacity),
            radius: GymBroShadow.cardRadius,
            x: 0,
            y: GymBroShadow.cardY
        )
    }

    func gymBroElevatedShadow() -> some View {
        self.shadow(
            color: .black.opacity(GymBroShadow.elevatedOpacity),
            radius: GymBroShadow.elevatedRadius,
            x: 0,
            y: GymBroShadow.elevatedY
        )
    }
}

// MARK: - Preview

#Preview("GymBro Colors") {
    ScrollView {
        VStack(spacing: GymBroSpacing.md) {
            Group {
                colorSwatch("Background", GymBroColors.background)
                colorSwatch("Surface Primary", GymBroColors.surfacePrimary)
                colorSwatch("Surface Secondary", GymBroColors.surfaceSecondary)
                colorSwatch("Surface Elevated", GymBroColors.surfaceElevated)
            }

            Divider().background(GymBroColors.border)

            Group {
                colorSwatch("Accent Green", GymBroColors.accentGreen)
                colorSwatch("Accent Amber", GymBroColors.accentAmber)
                colorSwatch("Accent Red", GymBroColors.accentRed)
                colorSwatch("Accent Cyan", GymBroColors.accentCyan)
            }

            Divider().background(GymBroColors.border)

            VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                Text("Hero Number")
                    .font(GymBroTypography.heroNumber)
                    .foregroundStyle(GymBroColors.textPrimary)
                Text("Large Title")
                    .font(GymBroTypography.largeTitle)
                    .foregroundStyle(GymBroColors.textPrimary)
                Text("Title")
                    .font(GymBroTypography.title)
                    .foregroundStyle(GymBroColors.textPrimary)
                Text("Body text content")
                    .font(GymBroTypography.body)
                    .foregroundStyle(GymBroColors.textSecondary)
                Text("Caption label")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(GymBroSpacing.md)
        }
        .padding(GymBroSpacing.md)
    }
    .gymBroDarkBackground()
}

@ViewBuilder
private func colorSwatch(_ name: String, _ color: Color) -> some View {
    HStack {
        RoundedRectangle(cornerRadius: GymBroRadius.sm)
            .fill(color)
            .frame(width: 48, height: 48)
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .strokeBorder(GymBroColors.border, lineWidth: 1)
            )
        Text(name)
            .font(GymBroTypography.body)
            .foregroundStyle(GymBroColors.textPrimary)
        Spacer()
    }
}
