import SwiftUI

/// Confetti-style particle effect using Canvas + TimelineView for PR celebrations.
/// Gated on `accessibilityReduceMotion`.
struct ConfettiCelebrationView: View {
    @State private var particles: [ConfettiParticle] = []
    @State private var isAnimating = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let particleCount: Int

    init(particleCount: Int = 60) {
        self.particleCount = particleCount
    }

    var body: some View {
        if reduceMotion {
            // Static celebration badge instead of confetti
            Image(systemName: "star.fill")
                .font(.largeTitle)
                .foregroundStyle(.yellow)
                .accessibilityLabel("Personal record celebration")
        } else {
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    let elapsed = isAnimating
                        ? timeline.date.timeIntervalSinceReferenceDate - (particles.first?.startTime ?? timeline.date.timeIntervalSinceReferenceDate)
                        : 0

                    for particle in particles {
                        let age = elapsed - (particle.delay)
                        guard age > 0 && age < particle.lifetime else { continue }

                        let progress = age / particle.lifetime
                        let x = size.width / 2 + particle.velocity.dx * CGFloat(age) + particle.drift * CGFloat(sin(age * particle.wobbleFrequency))
                        let y = size.height * 0.3 + particle.velocity.dy * CGFloat(age) + 120 * CGFloat(age * age)
                        let opacity = 1 - progress
                        let rotation = Angle.degrees(particle.rotationSpeed * age)

                        context.opacity = opacity
                        context.translateBy(x: x, y: y)
                        context.rotate(by: rotation)

                        let rect = CGRect(x: -particle.size / 2, y: -particle.size / 2, width: particle.size, height: particle.size)
                        context.fill(
                            Path(roundedRect: rect, cornerRadius: particle.isCircle ? particle.size / 2 : 2),
                            with: .color(particle.color)
                        )

                        context.rotate(by: -rotation)
                        context.translateBy(x: -x, y: -y)
                    }
                }
            }
            .allowsHitTesting(false)
            .accessibilityHidden(true)
            .onAppear {
                spawnParticles()
                isAnimating = true
            }
        }
    }

    private func spawnParticles() {
        let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple, .pink, .mint]
        particles = (0..<particleCount).map { _ in
            ConfettiParticle(
                velocity: CGVector(
                    dx: CGFloat.random(in: -120...120),
                    dy: CGFloat.random(in: -280...-80)
                ),
                lifetime: Double.random(in: 1.5...3.0),
                delay: Double.random(in: 0...0.3),
                color: colors.randomElement()!,
                size: CGFloat.random(in: 4...10),
                rotationSpeed: Double.random(in: -400...400),
                drift: CGFloat.random(in: -20...20),
                wobbleFrequency: Double.random(in: 2...6),
                isCircle: Bool.random(),
                startTime: Date.timeIntervalSinceReferenceDate
            )
        }
    }
}

private struct ConfettiParticle {
    let velocity: CGVector
    let lifetime: Double
    let delay: Double
    let color: Color
    let size: CGFloat
    let rotationSpeed: Double
    let drift: CGFloat
    let wobbleFrequency: Double
    let isCircle: Bool
    let startTime: TimeInterval
}
