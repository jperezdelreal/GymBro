import SwiftUI
import SwiftData
import GymBroCore

/// Main AI Coach chat view -- premium dark-themed conversational interface with
/// conversation history, context indicators, suggested prompts, streaming,
/// reactions, and voice input for hands-free gym use.
public struct CoachChatView: View {
    @State private var viewModel = CoachChatViewModel()
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @ScaledMetric(relativeTo: .largeTitle) private var welcomeIconSize: CGFloat = 48

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            headerBar

            ContextIndicatorBar(summary: viewModel.contextSummary)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: GymBroSpacing.md) {
                        if viewModel.messages.isEmpty {
                            welcomeMessage
                        }

                        ForEach(viewModel.messages, id: \.id) { message in
                            ChatMessageBubble(message: message) { reaction in
                                viewModel.toggleReaction(reaction, for: message)
                            }
                            .id(message.id)
                            .transition(
                                reduceMotion ? .identity : .asymmetric(
                                    insertion: .move(edge: .bottom).combined(with: .opacity),
                                    removal: .opacity
                                )
                            )
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    .padding(.vertical, GymBroSpacing.md)
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: viewModel.messages.count) {
                    if let last = viewModel.messages.last {
                        withAnimation(reduceMotion ? nil : .easeOut(duration: 0.3)) {
                            proxy.scrollTo(last.id, anchor: .bottom)
                        }
                    }
                }
            }

            if viewModel.isRetrying {
                retryingBanner
            }

            if let error = viewModel.errorMessage {
                errorBanner(error)
            }

            // Suggested prompts above input when conversation has started
            if !viewModel.messages.isEmpty && !viewModel.isLoading {
                SuggestedPromptsBar(prompts: viewModel.suggestedPrompts) { prompt in
                    Task { await viewModel.sendSuggestedPrompt(prompt) }
                }
            }

            inputArea
        }
        .gymBroDarkBackground()
        .task {
            viewModel.configure(modelContext: modelContext)
        }
    }

    // MARK: - Subviews

    private var headerBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("AI Coach")
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.textPrimary)
                HStack(spacing: GymBroSpacing.xs) {
                    Circle()
                        .fill(viewModel.isOfflineMode ? GymBroColors.accentAmber : GymBroColors.accentGreen)
                        .frame(width: 8, height: 8)
                    Text(viewModel.isOfflineMode ? "Offline Mode" : "Online")
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textSecondary)
                }
            }

            Spacer()

            if !viewModel.messages.isEmpty {
                Button("Clear History", systemImage: "trash") {
                    viewModel.clearHistory()
                }
                .labelStyle(.iconOnly)
                .foregroundStyle(GymBroColors.textSecondary)
            }
        }
        .padding(.horizontal, GymBroSpacing.md)
        .padding(.vertical, GymBroSpacing.md)
        .background(GymBroColors.surfacePrimary)
    }

    private var welcomeMessage: some View {
        VStack(spacing: GymBroSpacing.md) {
            Image(systemName: "brain.head.profile")
                .font(.system(size: welcomeIconSize))
                .foregroundStyle(GymBroColors.accentCyan)

            Text("GymBro Coach")
                .font(GymBroTypography.title2)
                .foregroundStyle(GymBroColors.textPrimary)

            Text("Your AI-powered strength training assistant. Ask me about programming, technique, recovery, or anything training-related.")
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, GymBroSpacing.xl)

            // Initial suggested prompts as vertical list
            VStack(spacing: GymBroSpacing.sm) {
                ForEach(SuggestedPrompt.defaults.prefix(4)) { prompt in
                    Button {
                        Task { await viewModel.sendSuggestedPrompt(prompt) }
                    } label: {
                        HStack(spacing: GymBroSpacing.sm) {
                            Image(systemName: prompt.icon)
                                .font(.caption)
                                .foregroundStyle(GymBroColors.accentCyan)
                                .frame(width: 20)
                            Text(prompt.text)
                                .font(GymBroTypography.subheadline)
                                .foregroundStyle(GymBroColors.textPrimary)
                            Spacer()
                            Image(systemName: "arrow.right")
                                .font(.caption2)
                                .foregroundStyle(GymBroColors.textTertiary)
                        }
                        .padding(.horizontal, GymBroSpacing.md)
                        .padding(.vertical, GymBroSpacing.md)
                        .background(GymBroColors.surfaceSecondary)
                        .clipShape(RoundedRectangle(cornerRadius: GymBroRadius.md))
                        .overlay(
                            RoundedRectangle(cornerRadius: GymBroRadius.md)
                                .strokeBorder(GymBroColors.border, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, GymBroSpacing.sm)
            .padding(.horizontal, GymBroSpacing.md)
        }
        .padding(.vertical, GymBroSpacing.xxl)
    }

    private var retryingBanner: some View {
        HStack(spacing: GymBroSpacing.sm) {
            ProgressView()
                .tint(GymBroColors.accentCyan)
                .controlSize(.small)
            Text("Retrying...")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
            Spacer()
        }
        .padding(.horizontal, GymBroSpacing.md)
        .padding(.vertical, GymBroSpacing.sm)
        .background(GymBroColors.accentCyan.opacity(0.08))
    }

    private func errorBanner(_ message: String) -> some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(GymBroColors.accentAmber)
            Text(message)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
            Spacer()
            Button("Dismiss") { viewModel.errorMessage = nil }
                .font(GymBroTypography.caption.bold())
                .foregroundStyle(GymBroColors.accentAmber)
        }
        .padding(.horizontal, GymBroSpacing.md)
        .padding(.vertical, GymBroSpacing.sm)
        .background(GymBroColors.accentAmber.opacity(0.1))
    }

    private var inputArea: some View {
        VStack(spacing: GymBroSpacing.sm) {
            usageCounter

            HStack(spacing: GymBroSpacing.sm) {
                #if canImport(Speech)
                VoiceInputButton { transcription in
                    viewModel.inputText = transcription
                }
                #endif

                TextField("Ask your coach...", text: $viewModel.inputText, axis: .vertical)
                    .textFieldStyle(.plain)
                    .font(GymBroTypography.body)
                    .foregroundStyle(GymBroColors.textPrimary)
                    .lineLimit(1...4)
                    .padding(.horizontal, GymBroSpacing.md)
                    .padding(.vertical, 10)
                    .background(GymBroColors.surfaceSecondary)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .strokeBorder(GymBroColors.border, lineWidth: 1)
                    )
                    .onSubmit { Task { await viewModel.sendMessage() } }

                Button("Send", systemImage: viewModel.isLoading ? "stop.circle.fill" : "arrow.up.circle.fill") {
                    Task { await viewModel.sendMessage() }
                }
                .labelStyle(.iconOnly)
                .font(.title2)
                .foregroundStyle(
                    viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        ? GymBroColors.textTertiary
                        : GymBroColors.accentGreen
                )
                .disabled(
                    viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !viewModel.isLoading
                )
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.bottom, GymBroSpacing.sm)
        }
        .background(GymBroColors.surfacePrimary)
    }

    @ViewBuilder
    private var usageCounter: some View {
        if viewModel.remainingFreeMessages < 5 {
            HStack {
                Text("\(viewModel.remainingFreeMessages) free messages remaining this week")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                Spacer()
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.top, GymBroSpacing.sm)
        }
    }
}
