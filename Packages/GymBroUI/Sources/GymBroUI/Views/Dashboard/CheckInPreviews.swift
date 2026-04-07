import SwiftUI
import GymBroCore

// MARK: - SubjectiveCheckInView Previews

#Preview("Check-In — Quick Vibe") {
    ScrollView {
        SubjectiveCheckInView()
            .padding()
    }
    .gymBroDarkBackground()
}

#Preview("Check-In — With Callback") {
    ScrollView {
        SubjectiveCheckInView { checkIn in
            print("Energy: \(checkIn.energy), Soreness: \(checkIn.soreness), Motivation: \(checkIn.motivation)")
        }
        .padding()
    }
    .gymBroDarkBackground()
}
