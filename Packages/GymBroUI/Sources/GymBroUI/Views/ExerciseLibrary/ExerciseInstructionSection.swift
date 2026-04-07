import SwiftUI

public struct ExerciseInstructionSection: View {
    let instructions: String
    @ScaledMetric(relativeTo: .headline) private var sectionTitleSize: CGFloat = 17
    @ScaledMetric(relativeTo: .body) private var bodySize: CGFloat = 16
    
    public init(instructions: String) {
        self.instructions = instructions
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
            let sections = parseInstructions(instructions)
            
            ForEach(Array(sections.enumerated()), id: \.offset) { _, section in
                instructionCard(section: section)
            }
        }
    }
    
    @ViewBuilder
    private func instructionCard(section: InstructionSection) -> some View {
        GymBroCard(accent: accentColor(for: section.type)) {
            VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                HStack(spacing: GymBroSpacing.sm) {
                    if let icon = iconName(for: section.type) {
                        Image(systemName: icon)
                            .foregroundStyle(accentColor(for: section.type))
                            .font(.system(size: sectionTitleSize, weight: .semibold))
                    }
                    
                    Text(section.title)
                        .font(.system(size: sectionTitleSize, weight: .semibold))
                        .foregroundStyle(GymBroColors.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                }
                
                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    ForEach(section.items, id: \.self) { item in
                        HStack(alignment: .top, spacing: GymBroSpacing.sm) {
                            Text("•")
                                .font(.system(size: bodySize))
                                .foregroundStyle(textColor(for: section.type))
                            Text(item)
                                .font(.system(size: bodySize))
                                .foregroundStyle(textColor(for: section.type))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            }
        }
    }
    
    private func accentColor(for type: SectionType) -> Color {
        switch type {
        case .setup:
            return GymBroColors.accentCyan
        case .execution:
            return GymBroColors.accentGreen
        case .mistakes:
            return GymBroColors.accentAmber
        case .safety:
            return GymBroColors.accentRed
        case .generic:
            return GymBroColors.textSecondary
        }
    }
    
    private func textColor(for type: SectionType) -> Color {
        switch type {
        case .safety:
            return GymBroColors.textPrimary
        default:
            return GymBroColors.textSecondary
        }
    }
    
    private func iconName(for type: SectionType) -> String? {
        switch type {
        case .setup:
            return "gearshape.fill"
        case .execution:
            return "figure.strengthtraining.traditional"
        case .mistakes:
            return "exclamationmark.triangle.fill"
        case .safety:
            return "shield.fill"
        case .generic:
            return nil
        }
    }
    
    private func parseInstructions(_ text: String) -> [InstructionSection] {
        var sections: [InstructionSection] = []
        let lines = text.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        
        var currentSection: InstructionSection?
        
        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            
            if trimmed.isEmpty {
                continue
            }
            
            // Check if this is a section header (all caps, no bullet)
            if trimmed.uppercased() == trimmed && !trimmed.hasPrefix("•") && !trimmed.hasPrefix("⚠️") {
                // Save previous section
                if let section = currentSection {
                    sections.append(section)
                }
                
                // Start new section
                let type = sectionType(for: trimmed)
                currentSection = InstructionSection(title: trimmed, type: type, items: [])
                continue
            }
            
            // This is a content line
            let cleanLine = trimmed
                .replacingOccurrences(of: "^[•⚠️]\\s*", with: "", options: .regularExpression)
                .trimmingCharacters(in: .whitespaces)
            
            if !cleanLine.isEmpty {
                currentSection?.items.append(cleanLine)
            }
        }
        
        // Add final section
        if let section = currentSection {
            sections.append(section)
        }
        
        return sections
    }
    
    private func sectionType(for title: String) -> SectionType {
        let upper = title.uppercased()
        if upper.contains("SETUP") || upper.contains("STARTING") || upper.contains("POSITION") {
            return .setup
        } else if upper.contains("EXECUTION") || upper.contains("MOVEMENT") || upper.contains("PERFORM") {
            return .execution
        } else if upper.contains("MISTAKE") || upper.contains("AVOID") || upper.contains("DON'T") {
            return .mistakes
        } else if upper.contains("SAFETY") || upper.contains("WARNING") || upper.contains("CAUTION") {
            return .safety
        } else {
            return .generic
        }
    }
}

// MARK: - Models

struct InstructionSection {
    let title: String
    let type: SectionType
    var items: [String]
}

enum SectionType {
    case setup
    case execution
    case mistakes
    case safety
    case generic
}

// MARK: - Preview

#Preview("Instruction Section") {
    ScrollView {
        ExerciseInstructionSection(
            instructions: """
            SETUP
            • Position barbell on upper traps (high bar) or rear delts (low bar)
            • Feet shoulder-width, toes slightly out (15-30°)
            • Engage core, chest up, eyes neutral
            
            EXECUTION
            • Inhale at top, brace core
            • Break at hips and knees simultaneously
            • Descend until thighs parallel or below (full ROM)
            • Drive through midfoot, exhale on ascent
            • Maintain neutral spine throughout
            
            COMMON MISTAKES
            • Knees caving inward (valgus collapse)
            • Excessive forward lean
            • Heels lifting off ground
            • Losing core tension at bottom
            
            SAFETY
            ⚠️ Use safety bars or squat in a power rack
            ⚠️ Bail properly if failing — drop bar behind you, step forward
            ⚠️ Advanced exercise — ensure proper ankle/hip mobility before loading heavy
            """
        )
        .padding(GymBroSpacing.md)
    }
    .gymBroDarkBackground()
}
