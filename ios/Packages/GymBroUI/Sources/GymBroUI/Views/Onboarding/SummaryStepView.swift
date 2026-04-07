import SwiftUI
import GymBroCore

/// Summary and first workout suggestion step.
struct SummaryStepView: View {
    let goals: Set<TrainingGoal>
    let experience: ExperienceLevel
    let frequency: Int
    let equipment: EquipmentType
    let limitations: String
    let onComplete: () -> Void
    let onBack: () -> Void
    
    @State private var isCompleting = false
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.xl) {
                    // Header
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text("You're all set! 🎉")
                            .font(GymBroTypography.largeTitle)
                            .foregroundStyle(GymBroColors.textPrimary)
                        
                        Text("Here's your training profile")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    // Profile summary cards
                    VStack(spacing: GymBroSpacing.md) {
                        summaryCard(
                            title: "Goals",
                            icon: "target",
                            value: goals.map { $0.rawValue }.joined(separator: ", ")
                        )
                        
                        summaryCard(
                            title: "Experience",
                            icon: "chart.line.uptrend.xyaxis",
                            value: experience.rawValue.capitalized
                        )
                        
                        summaryCard(
                            title: "Frequency",
                            icon: "calendar",
                            value: "\(frequency) days/week"
                        )
                        
                        summaryCard(
                            title: "Equipment",
                            icon: "dumbbell.fill",
                            value: equipment.displayName
                        )
                        
                        if !limitations.isEmpty {
                            summaryCard(
                                title: "Limitations",
                                icon: "heart.text.square",
                                value: limitations
                            )
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    // First workout suggestion
                    firstWorkoutCard
                        .padding(.horizontal, GymBroSpacing.md)
                }
                .padding(.top, GymBroSpacing.xl)
                .padding(.bottom, GymBroSpacing.xxl * 2)
            }
            
            // Bottom button tray
            buttonTray
        }
    }
    
    @ViewBuilder
    private func summaryCard(title: String, icon: String, value: String) -> some View {
        GymBroCard {
            HStack(spacing: GymBroSpacing.md) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(GymBroColors.accentGreen)
                    .frame(width: 28)
                
                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    Text(title)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                    
                    Text(value)
                        .font(GymBroTypography.headline)
                        .foregroundStyle(GymBroColors.textPrimary)
                }
                
                Spacer()
            }
        }
    }
    
    @ViewBuilder
    private var firstWorkoutCard: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Text("SUGGESTED FIRST WORKOUT")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.5)
            
            GymBroCard(accent: GymBroColors.accentGreen) {
                VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                    HStack {
                        Image(systemName: "bolt.fill")
                            .foregroundStyle(GymBroColors.accentGreen)
                        
                        Text(suggestedWorkoutName)
                            .font(GymBroTypography.title3)
                            .foregroundStyle(GymBroColors.textPrimary)
                    }
                    
                    Text(suggestedExercises)
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textSecondary)
                        .lineSpacing(4)
                }
            }
            
            Text("You can start this workout or create your own from the Workout tab")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
        }
    }
    
    // Rule-based first workout suggestion
    private var suggestedWorkoutName: String {
        if goals.contains(.strength) {
            return "Strength Foundations"
        } else if goals.contains(.hypertrophy) {
            return "Muscle Builder"
        } else if goals.contains(.endurance) {
            return "Endurance Circuit"
        } else {
            return "Full Body Essentials"
        }
    }
    
    private var suggestedExercises: String {
        if equipment == .bodyweightOnly {
            return "• Push-ups\n• Pull-ups\n• Squats\n• Plank"
        } else if goals.contains(.strength) {
            return "• Squat\n• Bench Press\n• Deadlift\n• Overhead Press"
        } else if goals.contains(.hypertrophy) {
            return "• Barbell Squat\n• Romanian Deadlift\n• Dumbbell Bench Press\n• Lat Pulldown"
        } else {
            return "• Goblet Squat\n• Push-ups\n• Dumbbell Row\n• Plank"
        }
    }
    
    @ViewBuilder
    private var buttonTray: some View {
        VStack(spacing: GymBroSpacing.md) {
            Divider()
                .background(GymBroColors.border)
            
            HStack(spacing: GymBroSpacing.md) {
                Button("Back") {
                    onBack()
                }
                .buttonStyle(GymBroSecondaryButtonStyle(accent: GymBroColors.textSecondary))
                .frame(maxWidth: 100)
                .disabled(isCompleting)
                
                Button(isCompleting ? "Setting up..." : "Let's Go!") {
                    isCompleting = true
                    
                    // Heavy haptic for completion
                    let generator = UIImpactFeedbackGenerator(style: .heavy)
                    generator.impactOccurred()
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        onComplete()
                    }
                }
                .buttonStyle(.gymBroPrimary)
                .disabled(isCompleting)
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
        .padding(.bottom, GymBroSpacing.md)
        .background(
            GymBroColors.background
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

#Preview {
    SummaryStepView(
        goals: [.strength, .hypertrophy],
        experience: .intermediate,
        frequency: 4,
        equipment: .fullGym,
        limitations: "Lower back issues",
        onComplete: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
