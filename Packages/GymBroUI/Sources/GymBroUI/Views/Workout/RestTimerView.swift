import SwiftUI
import GymBroCore

public struct RestTimerView: View {
    @State private var timerService = RestTimerService.shared
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @ScaledMetric(relativeTo: .largeTitle) private var timerFontSize: CGFloat = 56

    public init() {}
    
    public var body: some View {
        VStack(spacing: 24) {
            // Timer countdown
            ZStack {
                // Background circle
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 12)
                    .frame(width: 200, height: 200)
                
                // Progress circle with gradient stroke
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        AngularGradient(
                            colors: [timerColor.opacity(0.6), timerColor],
                            center: .center,
                            startAngle: .degrees(0),
                            endAngle: .degrees(360 * progress)
                        ),
                        style: StrokeStyle(lineWidth: 12, lineCap: .round)
                    )
                    .frame(width: 200, height: 200)
                    .rotationEffect(.degrees(-90))
                    .animation(reduceMotion ? nil : .easeInOut(duration: 1), value: progress)
                
                // Time remaining
                VStack(spacing: 4) {
                    Text(timeString)
                        .font(.system(size: timerFontSize, weight: .bold, design: .rounded))
                        .monospacedDigit()
                    
                    Text("remaining")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            
            // Next set preview
            if let nextSet = timerService.nextSetInfo {
                VStack(spacing: 8) {
                    Text("UP NEXT")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .fontWeight(.semibold)
                    
                    Text(nextSet.exerciseName)
                        .font(.headline)
                    
                    HStack(spacing: 16) {
                        HStack(spacing: 4) {
                            Text("Set")
                            Text("\(nextSet.setNumber)")
                                .fontWeight(.bold)
                        }
                        
                        HStack(spacing: 4) {
                            Text("\(Int(nextSet.targetWeight))")
                                .fontWeight(.bold)
                            Text(nextSet.weightUnit)
                        }
                        
                        HStack(spacing: 4) {
                            Text("\(nextSet.targetReps)")
                                .fontWeight(.bold)
                            Text("reps")
                        }
                    }
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                }
                .padding()
                .background(.quaternary)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            
            // Quick adjust buttons
            HStack(spacing: 16) {
                Button {
                    timerService.addTime(-30)
                } label: {
                    Label("-30s", systemImage: "minus.circle.fill")
                        .font(.headline)
                }
                .buttonStyle(.bordered)
                .disabled(timerService.remainingSeconds <= 30)
                
                Button {
                    timerService.addTime(30)
                } label: {
                    Label("+30s", systemImage: "plus.circle.fill")
                        .font(.headline)
                }
                .buttonStyle(.bordered)
            }
            
            // Skip button
            Button {
                timerService.skip()
            } label: {
                Text("Skip Rest")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.orange)
        }
        .padding()
    }
    
    private var progress: Double {
        guard timerService.totalSeconds > 0 else { return 0 }
        return Double(timerService.remainingSeconds) / Double(timerService.totalSeconds)
    }
    
    private var timeString: String {
        let minutes = timerService.remainingSeconds / 60
        let seconds = timerService.remainingSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
    
    private var timerColor: Color {
        if timerService.remainingSeconds <= 10 {
            return .red
        } else if timerService.remainingSeconds <= 30 {
            return .orange
        } else {
            return .blue
        }
    }
}

#Preview {
    RestTimerView()
        .onAppear {
            RestTimerService.shared.start(
                duration: 120,
                nextSetInfo: NextSetInfo(
                    exerciseName: "Barbell Squat",
                    setNumber: 3,
                    targetReps: 5,
                    targetWeight: 140.0,
                    weightUnit: "kg"
                )
            )
        }
}
