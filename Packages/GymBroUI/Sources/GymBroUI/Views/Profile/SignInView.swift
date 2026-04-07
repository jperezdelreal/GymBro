import SwiftUI
import AuthenticationServices
import GymBroCore

/// Sign in with Apple onboarding view.
/// Users can sign in for CloudKit sync or skip to use the app in local-only mode.
public struct SignInView: View {
    @Environment(\.colorScheme) private var colorScheme

    private let authService: AuthenticationService
    private let onContinueWithoutSignIn: () -> Void

    @State private var isLoading = false
    @State private var errorMessage: String?

    public init(
        authService: AuthenticationService,
        onContinueWithoutSignIn: @escaping () -> Void
    ) {
        self.authService = authService
        self.onContinueWithoutSignIn = onContinueWithoutSignIn
    }

    public var body: some View {
        VStack(spacing: 0) {
            Spacer()

            // App branding
            VStack(spacing: 16) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.system(size: 72))
                    .foregroundStyle(.tint)

                Text("GymBro")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Your AI-powered training partner")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            // Benefits of signing in
            VStack(alignment: .leading, spacing: 12) {
                benefitRow(
                    icon: "icloud",
                    title: "Sync across devices",
                    subtitle: "Your workouts, backed up to iCloud"
                )
                benefitRow(
                    icon: "lock.shield",
                    title: "Private & secure",
                    subtitle: "End-to-end encrypted with Apple"
                )
                benefitRow(
                    icon: "arrow.clockwise",
                    title: "Never lose data",
                    subtitle: "Automatic cloud backup"
                )
            }
            .padding(.horizontal, 32)

            Spacer()

            // Sign in button + skip option
            VStack(spacing: 16) {
                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                SignInWithAppleButton(
                    .signIn,
                    onRequest: { request in
                        request.requestedScopes = [.fullName, .email]
                    },
                    onCompletion: { _ in
                        // Handled by AuthenticationService delegate
                    }
                )
                .signInWithAppleButtonStyle(
                    colorScheme == .dark ? .white : .black
                )
                .frame(height: 50)
                .padding(.horizontal, 32)
                .overlay {
                    if isLoading {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(.ultraThinMaterial)
                            .padding(.horizontal, 32)
                        ProgressView()
                    }
                }
                .disabled(isLoading)
                .onTapGesture {
                    performSignIn()
                }

                Button("Continue without signing in") {
                    onContinueWithoutSignIn()
                }
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.bottom, 8)
            }
            .padding(.bottom, 32)
        }
    }

    @ViewBuilder
    private func benefitRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(.tint)
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)

                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func performSignIn() {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                try await authService.signInWithApple()
            } catch let error as AuthError {
                if error != .canceled {
                    errorMessage = error.localizedDescription
                }
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }
}
