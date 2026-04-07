import SwiftUI
import SwiftData
import GymBroCore

/// Main AI Coach chat view — conversational interface with streaming responses.
public struct CoachChatView: View {
    @State private var viewModel = CoachChatViewModel()
    @Environment(\.modelContext) private var modelContext

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header
            headerBar

            // Messages
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        if viewModel.messages.isEmpty {
                            welcomeMessage
                        }

                        ForEach(viewModel.messages, id: \.id) { message in
                            ChatMessageBubble(message: message)
                                .id(message.id)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .onChange(of: viewModel.messages.count) {
                    if let last = viewModel.messages.last {
                        withAnimation(.easeOut(duration: 0.3)) {
                            proxy.scrollTo(last.id, anchor: .bottom)
                        }
                    }
                }
            }

            // Error banner
            if let error = viewModel.errorMessage {
                errorBanner(error)
            }

            // Input area
            inputArea
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
        }
    }

    // MARK: - Subviews

    private var headerBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("AI Coach")
                    .font(.headline)
                HStack(spacing: 4) {
                    Circle()
                        .fill(viewModel.isOfflineMode ? Color.orange : Color.green)
                        .frame(width: 8, height: 8)
                    Text(viewModel.isOfflineMode ? "Offline Mode" : "Online")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            if !viewModel.messages.isEmpty {
                Button {
                    viewModel.clearHistory()
                } label: {
                    Image(systemName: "trash")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
    }

    private var welcomeMessage: some View {
        VStack(spacing: 16) {
            Image(systemName: "brain.head.profile")
                .font(.system(size: 48))
                .foregroundStyle(.tint)

            Text("GymBro Coach")
                .font(.title2.bold())

            Text("Your AI-powered strength training assistant. Ask me about programming, technique, recovery, or anything training-related.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            // Quick action chips
            VStack(spacing: 8) {
                quickActionChip("How should I warm up for squats?")
                quickActionChip("What's a good deload strategy?")
                quickActionChip("Explain RPE vs RIR")
            }
            .padding(.top, 8)
        }
        .padding(.vertical, 40)
    }

    private func quickActionChip(_ text: String) -> some View {
        Button {
            viewModel.inputText = text
            Task { await viewModel.sendMessage() }
        } label: {
            Text(text)
                .font(.subheadline)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func errorBanner(_ message: String) -> some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.orange)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
            Button("Dismiss") { viewModel.errorMessage = nil }
                .font(.caption.bold())
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.orange.opacity(0.1))
    }

    private var inputArea: some View {
        VStack(spacing: 8) {
            // Usage counter for free tier
            usageCounter

            HStack(spacing: 12) {
                TextField("Ask your coach...", text: $viewModel.inputText, axis: .vertical)
                    .textFieldStyle(.plain)
                    .lineLimit(1...4)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color(.systemGray6))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .onSubmit { Task { await viewModel.sendMessage() } }

                Button {
                    Task { await viewModel.sendMessage() }
                } label: {
                    Image(systemName: viewModel.isLoading ? "stop.circle.fill" : "arrow.up.circle.fill")
                        .font(.title2)
                        .foregroundStyle(viewModel.inputText.isEmpty ? .secondary : .tint)
                }
                .disabled(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !viewModel.isLoading)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
        }
        .background(.ultraThinMaterial)
    }

    @ViewBuilder
    private var usageCounter: some View {
        if viewModel.remainingFreeMessages < 5 {
            HStack {
                Text("\(viewModel.remainingFreeMessages) free messages remaining this week")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
    }
}
