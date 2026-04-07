import SwiftUI
import GymBroCore

// MARK: - SubjectiveCheckInView Previews

#Preview("Check-In — Default") {
    SubjectiveCheckInView()
        .padding()
        .gymBroDarkBackground()
}

#Preview("Check-In — Submitted") {
    SubjectiveCheckInView { checkIn in
        // Submitted callback
    }
    .padding()
    .gymBroDarkBackground()
}
