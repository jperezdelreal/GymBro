import SwiftUI
import GymBroCore

/// User profile view showing auth state, account info, and sign-in/sign-out controls.
public struct ProfileView: View {
    private let authService: AuthenticationService
    private let syncService: CloudKitSyncService

    @State private var showSignOutConfirmation = false
    @State private var deleteDataOnSignOut = false

    public init(authService: AuthenticationService, syncService: CloudKitSyncService) {
        self.authService = authService
        self.syncService = syncService
    }

    public var body: some View {
        List {
            // Account section
            Section("Account") {
                if authService.isSignedIn {
                    signedInSection
                } else {
                    signedOutSection
                }
            }

            // Sync section (only when signed in)
            if authService.isSignedIn {
                Section("iCloud Sync") {
                    SyncStatusView(syncService: syncService)
                }
            }

            // App info
            Section("About") {
                LabeledContent("Version", value: "1.0.0")
                LabeledContent("Data Storage", value: authService.isSignedIn ? "iCloud + Local" : "Local Only")
            }
        }
        .navigationTitle("Profile")
        .confirmationDialog(
            "Sign Out",
            isPresented: $showSignOutConfirmation,
            titleVisibility: .visible
        ) {
            Button("Keep local data", role: .none) {
                authService.signOut(deleteLocalData: false)
            }
            Button("Delete local data", role: .destructive) {
                authService.signOut(deleteLocalData: true)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Your workouts in iCloud will not be affected. Choose whether to keep a local copy on this device.")
        }
    }

    @ViewBuilder
    private var signedInSection: some View {
        HStack {
            Image(systemName: "person.circle.fill")
                .font(.title)
                .foregroundStyle(.tint)

            VStack(alignment: .leading, spacing: 2) {
                Text(authService.userDisplayName ?? "Apple ID User")
                    .font(.headline)

                if let email = authService.userEmail {
                    Text(email)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }

        Button("Sign Out", role: .destructive) {
            showSignOutConfirmation = true
        }
    }

    @ViewBuilder
    private var signedOutSection: some View {
        HStack {
            Image(systemName: "person.circle")
                .font(.title)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 2) {
                Text("Not signed in")
                    .font(.headline)

                Text("Sign in to sync across devices")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }

        Button {
            Task {
                try? await authService.signInWithApple()
            }
        } label: {
            Label("Sign in with Apple", systemImage: "apple.logo")
        }
    }
}
