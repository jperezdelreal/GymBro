import SwiftUI

// MARK: - Primary Button Style

/// Neon green gradient fill with heavy haptic feedback and press animation.
public struct GymBroPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(GymBroTypography.headline)
            .foregroundStyle(GymBroColors.background)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                isEnabled
                    ? AnyShapeStyle(GymBroColors.greenGradient)
                    : AnyShapeStyle(GymBroColors.surfaceElevated)
            )
            .clipShape(RoundedRectangle(cornerRadius: GymBroRadius.lg))
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.15), value: configuration.isPressed)
            .gymBroElevatedShadow()
            .onChange(of: configuration.isPressed) { _, pressed in
                if pressed {
                    let generator = UIImpactFeedbackGenerator(style: .heavy)
                    generator.impactOccurred()
                }
            }
    }
}

// MARK: - Secondary Button Style

/// Outlined with accent border and subtle fill on press.
public struct GymBroSecondaryButtonStyle: ButtonStyle {
    let accentColor: Color

    public init(accent: Color = GymBroColors.accentGreen) {
        self.accentColor = accent
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(GymBroTypography.headline)
            .foregroundStyle(accentColor)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                configuration.isPressed
                    ? accentColor.opacity(0.15)
                    : Color.clear
            )
            .clipShape(RoundedRectangle(cornerRadius: GymBroRadius.lg))
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.lg)
                    .strokeBorder(accentColor.opacity(0.6), lineWidth: 1.5)
            )
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.15), value: configuration.isPressed)
    }
}

// MARK: - Destructive Button Style

/// Red tint for dangerous actions.
public struct GymBroDestructiveButtonStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(GymBroTypography.headline)
            .foregroundStyle(GymBroColors.accentRed)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                configuration.isPressed
                    ? GymBroColors.accentRed.opacity(0.15)
                    : GymBroColors.accentRed.opacity(0.08)
            )
            .clipShape(RoundedRectangle(cornerRadius: GymBroRadius.lg))
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.lg)
                    .strokeBorder(GymBroColors.accentRed.opacity(0.3), lineWidth: 1)
            )
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.15), value: configuration.isPressed)
    }
}

// MARK: - Convenience Extensions

public extension ButtonStyle where Self == GymBroPrimaryButtonStyle {
    static var gymBroPrimary: GymBroPrimaryButtonStyle { .init() }
}

public extension ButtonStyle where Self == GymBroDestructiveButtonStyle {
    static var gymBroDestructive: GymBroDestructiveButtonStyle { .init() }
}

// MARK: - Previews

#Preview("GymBro Buttons") {
    VStack(spacing: GymBroSpacing.lg) {
        Button("Complete Set") {}
            .buttonStyle(.gymBroPrimary)

        Button("Complete Set") {}
            .buttonStyle(.gymBroPrimary)
            .disabled(true)

        Button("Add Exercise") {}
            .buttonStyle(GymBroSecondaryButtonStyle())

        Button("Add Exercise") {}
            .buttonStyle(GymBroSecondaryButtonStyle(accent: GymBroColors.accentCyan))

        Button("Discard Workout") {}
            .buttonStyle(.gymBroDestructive)
    }
    .padding(GymBroSpacing.md)
    .gymBroDarkBackground()
}
