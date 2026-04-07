import SwiftUI
import SwiftData
import GymBroCore

/// Main onboarding flow coordinator. Progressive disclosure, fast completion.
public struct OnboardingFlowView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    
    @State private var currentStep: OnboardingStep = .welcome
    @State private var trainingGoals: Set<TrainingGoal> = []
    @State private var experienceLevel: ExperienceLevel = .intermediate
    @State private var trainingFrequency: Int = 3
    @State private var equipmentType: EquipmentType = .fullGym
    @State private var injuriesText: String = ""
    
    private let onComplete: () -> Void
    
    public init(onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
    }
    
    public var body: some View {
        ZStack {
            GymBroColors.background
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Progress bar
                progressBar
                    .padding(.horizontal, GymBroSpacing.md)
                    .padding(.top, GymBroSpacing.md)
                
                // Current step view
                Group {
                    switch currentStep {
                    case .welcome:
                        WelcomeStepView(onNext: { currentStep = .goals })
                    case .goals:
                        GoalsStepView(
                            selectedGoals: $trainingGoals,
                            onNext: { currentStep = .experience },
                            onBack: { currentStep = .welcome }
                        )
                    case .experience:
                        ExperienceStepView(
                            selectedLevel: $experienceLevel,
                            onNext: { currentStep = .frequency },
                            onBack: { currentStep = .goals }
                        )
                    case .frequency:
                        FrequencyStepView(
                            selectedFrequency: $trainingFrequency,
                            onNext: { currentStep = .equipment },
                            onBack: { currentStep = .experience }
                        )
                    case .equipment:
                        EquipmentStepView(
                            selectedEquipment: $equipmentType,
                            onNext: { currentStep = .limitations },
                            onBack: { currentStep = .frequency }
                        )
                    case .limitations:
                        LimitationsStepView(
                            injuriesText: $injuriesText,
                            onNext: { currentStep = .summary },
                            onBack: { currentStep = .equipment }
                        )
                    case .summary:
                        SummaryStepView(
                            goals: trainingGoals,
                            experience: experienceLevel,
                            frequency: trainingFrequency,
                            equipment: equipmentType,
                            limitations: injuriesText,
                            onComplete: completeOnboarding,
                            onBack: { currentStep = .limitations }
                        )
                    }
                }
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing).combined(with: .opacity),
                    removal: .move(edge: .leading).combined(with: .opacity)
                ))
                .animation(reduceMotion ? nil : .easeInOut(duration: 0.3), value: currentStep)
            }
        }
        .preferredColorScheme(.dark)
    }
    
    private var progressBar: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                // Background track
                RoundedRectangle(cornerRadius: 2)
                    .fill(GymBroColors.surfaceSecondary)
                    .frame(height: 4)
                
                // Progress fill
                RoundedRectangle(cornerRadius: 2)
                    .fill(GymBroColors.accentGreen)
                    .frame(width: geometry.size.width * currentStep.progress, height: 4)
                    .animation(reduceMotion ? nil : .easeInOut(duration: 0.3), value: currentStep)
            }
        }
        .frame(height: 4)
    }
    
    private func completeOnboarding() {
        let profile = UserProfile(
            experienceLevel: experienceLevel,
            hasCompletedOnboarding: true,
            trainingGoals: Array(trainingGoals.map { $0.rawValue }),
            trainingFrequency: trainingFrequency,
            equipmentAvailability: equipmentType,
            injuriesOrLimitations: injuriesText.isEmpty ? nil : injuriesText
        )
        
        modelContext.insert(profile)
        try? modelContext.save()
        
        onComplete()
    }
}

// MARK: - Onboarding Steps

enum OnboardingStep: Int, CaseIterable {
    case welcome = 0
    case goals
    case experience
    case frequency
    case equipment
    case limitations
    case summary
    
    var progress: CGFloat {
        CGFloat(rawValue) / CGFloat(Self.allCases.count - 1)
    }
}

public enum TrainingGoal: String, CaseIterable, Identifiable {
    case strength = "Strength"
    case hypertrophy = "Muscle Growth"
    case endurance = "Endurance"
    case generalFitness = "General Fitness"
    case athleticPerformance = "Athletic Performance"
    
    public var id: String { rawValue }
    
    var icon: String {
        switch self {
        case .strength:
            return "bolt.fill"
        case .hypertrophy:
            return "flame.fill"
        case .endurance:
            return "figure.run"
        case .generalFitness:
            return "heart.fill"
        case .athleticPerformance:
            return "trophy.fill"
        }
    }
}
