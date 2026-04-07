import SwiftUI

/// Floating action button overlay for quick access to AI Coach during workouts.
public struct CoachFloatingButton: View {
    @Binding var isPresented: Bool

    public init(isPresented: Binding<Bool>) {
        self._isPresented = isPresented
    }

    public var body: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                Button {
                    isPresented = true
                } label: {
                    Image(systemName: "brain.head.profile")
                        .font(.title2)
                        .foregroundStyle(.white)
                        .frame(width: 56, height: 56)
                        .background(Color.accentColor)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.2), radius: 8, y: 4)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
    }
}
