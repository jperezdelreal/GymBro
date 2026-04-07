import SwiftUI
import GymBroCore

/// One-tap daily vibe check — emoji select for instant check-in.
/// Default: single row of 5 emojis, one tap auto-submits.
/// Optional: "Fine-tune" expands individual factor sliders.
/// Skip uses smart defaults (neutral 3/3/3). Target: < 5 seconds.
public struct SubjectiveCheckInView: View {
    @State private var selectedVibe: Int? = nil
    @State private var energy: Int = 3
    @State private var soreness: Int = 3
    @State private var motivation: Int = 3
    @State private var showFineTune = false
    @State private var submitted = false
    @State private var bounceEmoji: Int? = nil

    public var onSubmit: ((SubjectiveCheckIn) -> Void)?

    public init(onSubmit: ((SubjectiveCheckIn) -> Void)? = nil) {
        self.onSubmit = onSubmit
    }

    // Emoji options: worst → best
    private static let vibes: [(emoji: String, label: String)] = [
        ("😫", "Rough"),
        ("😕", "Meh"),
        ("😐", "OK"),
        ("😊", "Good"),
        ("🔥", "Great")
    ]

    public var body: some View {
        GymBroCard {
            VStack(spacing: GymBroSpacing.md) {
                if submitted {
                    submittedView
                } else {
                    header
                    emojiRow
                    fineTuneSection
                    bottomActions
                }
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: GymBroSpacing.sm) {
            Text("How do you feel?")
                .font(GymBroTypography.headline)
                .foregroundStyle(GymBroColors.textPrimary)
            Spacer()
            Button {
                submitCheckIn(energy: 3, soreness: 3, motivation: 3)
            } label: {
                Text("Skip")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Skip check-in, use defaults")
        }
    }

    // MARK: - Emoji Quick Select

    private var emojiRow: some View {
        HStack(spacing: GymBroSpacing.sm) {
            ForEach(1...5, id: \.self) { level in
                let vibe = Self.vibes[level - 1]
                let isSelected = selectedVibe == level

                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                        selectedVibe = level
                        bounceEmoji = level
                        applyVibeMapping(level)
                    }
                    // Auto-submit after brief visual feedback if not fine-tuning
                    if !showFineTune {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            submitFromVibe()
                        }
                    }
                } label: {
                    VStack(spacing: GymBroSpacing.xs) {
                        Text(vibe.emoji)
                            .font(.system(size: isSelected ? 36 : 28))
                            .scaleEffect(bounceEmoji == level ? 1.2 : 1.0)

                        Text(vibe.label)
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(
                                isSelected
                                    ? GymBroColors.accentGreen
                                    : GymBroColors.textTertiary
                            )
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, GymBroSpacing.sm)
                    .background(
                        RoundedRectangle(cornerRadius: GymBroRadius.md)
                            .fill(
                                isSelected
                                    ? GymBroColors.accentGreen.opacity(0.12)
                                    : Color.clear
                            )
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: GymBroRadius.md)
                            .strokeBorder(
                                isSelected
                                    ? GymBroColors.accentGreen.opacity(0.4)
                                    : Color.clear,
                                lineWidth: 1
                            )
                    )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Feeling \(vibe.label), \(level) of 5")
            }
        }
    }

    // MARK: - Fine-Tune (Optional Detail)

    @ViewBuilder
    private var fineTuneSection: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.25)) {
                showFineTune.toggle()
            }
        } label: {
            HStack(spacing: GymBroSpacing.xs) {
                Text("Fine-tune")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)
                Image(systemName: showFineTune ? "chevron.up" : "chevron.down")
                    .font(.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(showFineTune ? "Collapse individual ratings" : "Expand individual ratings")

        if showFineTune {
            VStack(spacing: GymBroSpacing.sm) {
                compactRating("⚡", label: "Energy", value: $energy)
                compactRating("💪", label: "Soreness", value: $soreness)
                compactRating("🔥", label: "Motivation", value: $motivation)
            }
            .transition(.opacity.combined(with: .move(edge: .top)))
        }
    }

    private func compactRating(
        _ emoji: String,
        label: String,
        value: Binding<Int>
    ) -> some View {
        HStack(spacing: GymBroSpacing.sm) {
            Text(emoji)
                .font(.system(size: 16))
                .frame(width: 24)

            Text(label)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
                .frame(width: 72, alignment: .leading)

            ForEach(1...5, id: \.self) { rating in
                Button {
                    value.wrappedValue = rating
                } label: {
                    Circle()
                        .fill(
                            rating <= value.wrappedValue
                                ? GymBroColors.accentGreen
                                : GymBroColors.surfaceElevated
                        )
                        .frame(width: 28, height: 28)
                        .overlay {
                            Text("\(rating)")
                                .font(.caption2.bold())
                                .foregroundStyle(
                                    rating <= value.wrappedValue
                                        ? GymBroColors.background
                                        : GymBroColors.textTertiary
                                )
                        }
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(label) \(rating) of 5")
            }

            Spacer()
        }
    }

    // MARK: - Bottom Actions

    @ViewBuilder
    private var bottomActions: some View {
        if showFineTune {
            Button {
                submitFromFineTune()
            } label: {
                Text("Save")
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.background)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, GymBroSpacing.sm + 2)
                    .background(GymBroColors.greenGradient)
                    .clipShape(RoundedRectangle(cornerRadius: GymBroRadius.md))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Save check-in with custom values")
        }
    }

    // MARK: - Submitted Confirmation

    private var submittedView: some View {
        HStack(spacing: GymBroSpacing.md) {
            Image(systemName: "checkmark.circle.fill")
                .font(.title2)
                .foregroundStyle(GymBroColors.accentGreen)
            VStack(alignment: .leading, spacing: 2) {
                Text("Checked in")
                    .font(GymBroTypography.subheadline.weight(.medium))
                    .foregroundStyle(GymBroColors.textPrimary)
                Text("Factored into your readiness score")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)
            }
            Spacer()
        }
        .padding(.vertical, GymBroSpacing.xs)
    }

    // MARK: - Logic

    /// Maps a single vibe level (1-5) to all three factors.
    private func applyVibeMapping(_ level: Int) {
        energy = level
        soreness = 6 - level // invert: feeling great = low soreness
        motivation = level
    }

    private func submitFromVibe() {
        guard let vibe = selectedVibe else { return }
        submitCheckIn(energy: vibe, soreness: 6 - vibe, motivation: vibe)
    }

    private func submitFromFineTune() {
        submitCheckIn(energy: energy, soreness: soreness, motivation: motivation)
    }

    private func submitCheckIn(energy: Int, soreness: Int, motivation: Int) {
        let checkIn = SubjectiveCheckIn(
            date: Date(),
            energy: energy,
            soreness: soreness,
            motivation: motivation
        )
        onSubmit?(checkIn)
        withAnimation(.easeInOut(duration: 0.3)) { submitted = true }
    }
}
