import SwiftUI
import GymBroCore

// MARK: - ProfileView Previews

#Preview("Profile — Signed Out") {
    NavigationStack {
        ProfileView(
            authService: AuthenticationService(),
            syncService: CloudKitSyncService()
        )
    }
    .preferredColorScheme(.dark)
}
