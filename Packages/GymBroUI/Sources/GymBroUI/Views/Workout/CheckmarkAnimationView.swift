import SwiftUI

/// Animated checkmark drawn with a path trim animation and a subtle scale bounce.
/// Respects `accessibilityReduceMotion`.
struct CheckmarkAnimationView: View {
    @State private var trimEnd: CGFloat = 0
    @State private var scale: CGFloat = 0.8
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let size: CGFloat
    let color: Color

    init(size: CGFloat = 24, color: Color = .green) {
        self.size = size
        self.color = color
    }

    var body: some View {
        ZStack {
            Circle()
                .fill(color.opacity(0.15))
                .frame(width: size * 1.4, height: size * 1.4)

            CheckmarkShape()
                .trim(from: 0, to: trimEnd)
                .stroke(color, style: StrokeStyle(lineWidth: size * 0.15, lineCap: .round, lineJoin: .round))
                .frame(width: size, height: size)
        }
        .scaleEffect(scale)
        .onAppear {
            if reduceMotion {
                trimEnd = 1
                scale = 1
            } else {
                withAnimation(.easeOut(duration: 0.4)) {
                    trimEnd = 1
                }
                withAnimation(.spring(response: 0.35, dampingFraction: 0.5).delay(0.3)) {
                    scale = 1
                }
            }
        }
    }
}

/// Custom checkmark path for trim animation.
struct CheckmarkShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let w = rect.width
        let h = rect.height
        path.move(to: CGPoint(x: w * 0.2, y: h * 0.5))
        path.addLine(to: CGPoint(x: w * 0.42, y: h * 0.72))
        path.addLine(to: CGPoint(x: w * 0.82, y: h * 0.28))
        return path
    }
}
