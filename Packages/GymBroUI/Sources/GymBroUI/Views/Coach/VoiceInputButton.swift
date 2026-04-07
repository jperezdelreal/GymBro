#if canImport(Speech)
import SwiftUI
import GymBroCore

/// Voice input button for hands-free gym use. Tap to record, tap to stop.
struct VoiceInputButton: View {
    @State private var voiceService = VoiceInputService()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let onTranscription: (String) -> Void

    @State private var pulseAnimation = false

    var body: some View {
        Button {
            toggleRecording()
        } label: {
            ZStack {
                if voiceService.isRecording {
                    Circle()
                        .fill(GymBroColors.accentRed.opacity(0.2))
                        .frame(width: 44, height: 44)
                        .scaleEffect(pulseAnimation ? 1.3 : 1.0)
                        .opacity(pulseAnimation ? 0.0 : 0.6)
                        .animation(
                            reduceMotion ? nil : .easeInOut(duration: 1.0).repeatForever(autoreverses: false),
                            value: pulseAnimation
                        )
                }

                Image(systemName: voiceService.isRecording ? "stop.fill" : "mic.fill")
                    .font(.body)
                    .foregroundStyle(voiceService.isRecording ? GymBroColors.accentRed : GymBroColors.accentCyan)
                    .frame(width: 36, height: 36)
                    .background(
                        voiceService.isRecording
                            ? GymBroColors.accentRed.opacity(0.15)
                            : GymBroColors.surfaceSecondary
                    )
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .strokeBorder(
                                voiceService.isRecording ? GymBroColors.accentRed.opacity(0.5) : GymBroColors.border,
                                lineWidth: 1
                            )
                    )
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(voiceService.isRecording ? "Stop recording" : "Start voice input")
        .task {
            voiceService.requestAuthorization()
        }
    }

    private func toggleRecording() {
        if voiceService.isRecording {
            let text = voiceService.stopRecording()
            pulseAnimation = false
            if !text.isEmpty {
                onTranscription(text)
            }
        } else {
            do {
                try voiceService.startRecording()
                pulseAnimation = true
            } catch {
                // Voice service sets its own errorMessage
            }
        }
    }
}
#endif
