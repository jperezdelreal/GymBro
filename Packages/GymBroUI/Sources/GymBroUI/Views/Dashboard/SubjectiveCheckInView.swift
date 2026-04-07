import SwiftUI
import GymBroCore

/// Quick daily check-in for subjective recovery factors (energy, soreness, motivation).
/// Each rated 1-5. Optional — the readiness score works without it.
public struct SubjectiveCheckInView: View {
    @State private var energy: Int = 3
    @State private var soreness: Int = 3
    @State private var motivation: Int = 3
    @State private var submitted = false

    public var onSubmit: ((SubjectiveCheckIn) -> Void)?

    public init(onSubmit: ((SubjectiveCheckIn) -> Void)? = nil) {
        self.onSubmit = onSubmit
    }

    public var body: some View {
        VStack(spacing: 20) {
            header

            if submitted {
                submittedView
            } else {
                ratingSection("Energy Level", value: $energy, icon: "bolt.fill", lowLabel: "Exhausted", highLabel: "Energized")
                ratingSection("Soreness", value: $soreness, icon: "figure.walk", lowLabel: "None", highLabel: "Very sore")
                ratingSection("Motivation", value: $motivation, icon: "flame.fill", lowLabel: "Low", highLabel: "Fired up")

                submitButton
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Subviews

    private var header: some View {
        HStack {
            Image(systemName: "person.fill.questionmark")
                .font(.title2)
                .foregroundStyle(.blue)
            VStack(alignment: .leading) {
                Text("Daily Check-In")
                    .font(.headline)
                Text("How are you feeling today?")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
    }

    private func ratingSection(
        _ title: String,
        value: Binding<Int>,
        icon: String,
        lowLabel: String,
        highLabel: String
    ) -> some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(.secondary)
                Text(title)
                    .font(.subheadline.weight(.medium))
                Spacer()
                Text("\(value.wrappedValue)/5")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 8) {
                Text(lowLabel)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .frame(width: 60, alignment: .trailing)

                ForEach(1...5, id: \.self) { rating in
                    Button {
                        value.wrappedValue = rating
                    } label: {
                        Circle()
                            .fill(rating <= value.wrappedValue ? Color.blue : Color(.systemGray4))
                            .frame(width: 32, height: 32)
                            .overlay {
                                Text("\(rating)")
                                    .font(.caption.bold())
                                    .foregroundStyle(rating <= value.wrappedValue ? .white : .secondary)
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(title) rating \(rating) of 5")
                }

                Text(highLabel)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .frame(width: 60, alignment: .leading)
            }
        }
    }

    private var submitButton: some View {
        Button {
            let checkIn = SubjectiveCheckIn(
                date: Date(),
                energy: energy,
                soreness: soreness,
                motivation: motivation
            )
            onSubmit?(checkIn)
            withAnimation { submitted = true }
        } label: {
            Text("Submit Check-In")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .buttonStyle(.borderedProminent)
    }

    private var submittedView: some View {
        VStack(spacing: 8) {
            Image(systemName: "checkmark.circle.fill")
                .font(.largeTitle)
                .foregroundStyle(.green)
            Text("Check-in recorded!")
                .font(.subheadline.weight(.medium))
            Text("Your subjective recovery has been factored into today's readiness score.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical)
    }
}
