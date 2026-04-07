import Foundation

/// Represents the current authentication state of the user.
public enum AuthState: Equatable, Sendable {
    case unknown
    case signedOut
    case signingIn
    case signedIn(userID: String)
    case error(String)

    public var isAuthenticated: Bool {
        if case .signedIn = self { return true }
        return false
    }
}

/// Authentication-related errors.
public enum AuthError: LocalizedError, Equatable {
    case invalidCredential
    case canceled
    case invalidResponse
    case notHandled
    case failed(String)
    case unknown(String)

    public var errorDescription: String? {
        switch self {
        case .invalidCredential:
            return "Invalid credential received from Apple."
        case .canceled:
            return "Sign in was canceled."
        case .invalidResponse:
            return "Invalid response from Apple."
        case .notHandled:
            return "Authorization request not handled."
        case .failed(let message):
            return "Sign in failed: \(message)"
        case .unknown(let message):
            return "Unknown error: \(message)"
        }
    }
}
