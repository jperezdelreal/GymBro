import Foundation
import AuthenticationServices
import os

/// Manages Sign in with Apple authentication flow and user session state.
/// Supports local-only mode: the app is fully usable without signing in.
@MainActor
@Observable
public final class AuthenticationService: NSObject {

    private static let logger = Logger(subsystem: "com.gymbro", category: "Auth")

    // MARK: - Keychain Keys

    private enum KeychainKey {
        static let userIdentifier = "apple_user_identifier"
        static let identityToken = "apple_identity_token"
        static let fullName = "apple_user_full_name"
        static let email = "apple_user_email"
    }

    // MARK: - Published State

    public private(set) var authState: AuthState = .unknown
    public private(set) var userDisplayName: String?
    public private(set) var userEmail: String?

    // MARK: - Continuations

    private var signInContinuation: CheckedContinuation<Void, Error>?

    // MARK: - Init

    public override init() {
        super.init()
    }

    // MARK: - Public API

    /// Checks existing credential state on launch.
    /// If a valid Apple ID credential exists in the Keychain, restores the session.
    public func checkExistingCredential() async {
        guard let userID = KeychainService.get(key: KeychainKey.userIdentifier) else {
            authState = .signedOut
            Self.logger.info("No stored credential — local-only mode")
            return
        }

        do {
            let provider = ASAuthorizationAppleIDProvider()
            let state = try await provider.credentialState(forUserID: userID)

            switch state {
            case .authorized:
                userDisplayName = KeychainService.get(key: KeychainKey.fullName)
                userEmail = KeychainService.get(key: KeychainKey.email)
                authState = .signedIn(userID: userID)
                Self.logger.info("Restored session for user")
            case .revoked, .notFound:
                clearCredentials()
                authState = .signedOut
                Self.logger.info("Credential revoked or not found")
            case .transferred:
                authState = .signedOut
            @unknown default:
                authState = .signedOut
            }
        } catch {
            Self.logger.error("Credential check failed: \(error.localizedDescription)")
            authState = .signedOut
        }
    }

    /// Initiates the Sign in with Apple flow.
    public func signInWithApple() async throws {
        authState = .signingIn

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            self.signInContinuation = continuation
            controller.performRequests()
        }
    }

    /// Signs the user out and optionally clears local data.
    public func signOut(deleteLocalData: Bool = false) {
        clearCredentials()
        authState = .signedOut
        userDisplayName = nil
        userEmail = nil
        Self.logger.info("User signed out (deleteLocalData: \(deleteLocalData))")
    }

    /// Whether the user is currently signed in.
    public var isSignedIn: Bool {
        if case .signedIn = authState { return true }
        return false
    }

    /// The Apple user identifier if signed in.
    public var currentUserID: String? {
        if case .signedIn(let userID) = authState { return userID }
        return nil
    }

    // MARK: - Private

    private func clearCredentials() {
        KeychainService.delete(key: KeychainKey.userIdentifier)
        KeychainService.delete(key: KeychainKey.identityToken)
        KeychainService.delete(key: KeychainKey.fullName)
        KeychainService.delete(key: KeychainKey.email)
    }

    private func storeCredentials(
        userID: String,
        identityToken: String?,
        fullName: PersonNameComponents?,
        email: String?
    ) {
        KeychainService.set(userID, forKey: KeychainKey.userIdentifier)

        if let token = identityToken {
            KeychainService.set(token, forKey: KeychainKey.identityToken)
        }

        if let name = fullName {
            let displayName = [name.givenName, name.familyName]
                .compactMap { $0 }
                .joined(separator: " ")
            if !displayName.isEmpty {
                KeychainService.set(displayName, forKey: KeychainKey.fullName)
                userDisplayName = displayName
            }
        }

        if let email = email {
            KeychainService.set(email, forKey: KeychainKey.email)
            userEmail = email
        }
    }
}

// MARK: - ASAuthorizationControllerDelegate

extension AuthenticationService: ASAuthorizationControllerDelegate {

    nonisolated public func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        Task { @MainActor in
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
                let error = AuthError.invalidCredential
                authState = .error(error.localizedDescription)
                signInContinuation?.resume(throwing: error)
                signInContinuation = nil
                return
            }

            let tokenString: String? = credential.identityToken
                .flatMap { String(data: $0, encoding: .utf8) }

            storeCredentials(
                userID: credential.user,
                identityToken: tokenString,
                fullName: credential.fullName,
                email: credential.email
            )

            // Restore display name from Keychain if Apple didn't provide it (returning user)
            if userDisplayName == nil {
                userDisplayName = KeychainService.get(key: KeychainKey.fullName)
            }

            authState = .signedIn(userID: credential.user)
            Self.logger.info("Sign in successful")
            signInContinuation?.resume()
            signInContinuation = nil
        }
    }

    nonisolated public func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        Task { @MainActor in
            let authError: AuthError
            if let asError = error as? ASAuthorizationError {
                switch asError.code {
                case .canceled:
                    authError = .canceled
                case .invalidResponse:
                    authError = .invalidResponse
                case .notHandled:
                    authError = .notHandled
                case .failed:
                    authError = .failed(asError.localizedDescription)
                case .notInteractive:
                    authError = .failed(asError.localizedDescription)
                @unknown default:
                    authError = .unknown(asError.localizedDescription)
                }
            } else {
                authError = .unknown(error.localizedDescription)
            }

            authState = .error(authError.localizedDescription)
            Self.logger.error("Sign in failed: \(authError.localizedDescription)")
            signInContinuation?.resume(throwing: authError)
            signInContinuation = nil
        }
    }
}
