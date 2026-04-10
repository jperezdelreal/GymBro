import Foundation

/// Builds system and user prompts with training context for the AI coach.
public struct PromptBuilder {

    public init() {}

    // MARK: - System Prompt

    public func buildSystemPrompt(context: CoachContext) -> String {
        var parts: [String] = [coreIdentity]

        if let profile = context.userProfile {
            parts.append(profileSection(profile))
        }

        if !context.recentWorkouts.isEmpty {
            parts.append(recentWorkoutsSection(context.recentWorkouts))
            let rpeSection = rpeTrendsSection(context.recentWorkouts)
            if !rpeSection.isEmpty {
                parts.append(rpeSection)
            }
        }

        if let program = context.activeProgram {
            parts.append(programSection(program))
        }

        if !context.personalRecords.isEmpty {
            parts.append(personalRecordsSection(context.personalRecords))
        }

        parts.append(safetyRules)
        return parts.joined(separator: "\n\n")
    }

    // MARK: - Private sections

    private var coreIdentity: String {
        """
        You are GymBro Coach, an expert strength training AI assistant. \
        You specialize in powerlifting, Olympic weightlifting, bodybuilding, and general strength programming.

        Guidelines:
        - Be concise and actionable. Serious lifters don't need fluff.
        - Always explain your reasoning (transparency required).
        - Use evidence-based recommendations (periodization science, RPE/RIR, progressive overload).
        - Reference the user's actual training data when available.
        - If uncertain, say so—don't fabricate data or studies.
        - Format responses with markdown for readability.
        """
    }

    private func profileSection(_ profile: UserProfileSnapshot) -> String {
        var lines = ["## Athlete Profile"]
        lines.append("- Experience: \(profile.experienceLevel)")
        lines.append("- Units: \(profile.unitSystem)")
        if let bw = profile.bodyweightKg {
            lines.append("- Bodyweight: \(String(format: "%.1f", bw)) kg")
        }
        return lines.joined(separator: "\n")
    }

    private func recentWorkoutsSection(_ workouts: [WorkoutSnapshot]) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short

        var lines = ["## Recent Workouts (last \(workouts.count))"]
        for workout in workouts.prefix(5) {
            let dateStr = formatter.string(from: workout.date)
            let exercises = workout.exercises.map { ex in
                var desc = "\(ex.name) \(ex.sets)×\(ex.bestReps)@\(String(format: "%.1f", ex.bestWeight))kg"
                if let rpe = ex.avgRpe {
                    desc += " RPE \(String(format: "%.0f", rpe))"
                }
                return desc
            }
            let duration = workout.durationMinutes.map { " (\(Int($0))min)" } ?? ""
            lines.append("- \(dateStr)\(duration): \(exercises.joined(separator: ", "))")
        }
        return lines.joined(separator: "\n")
    }

    private func programSection(_ program: ProgramSnapshot) -> String {
        """
        ## Active Program
        - Name: \(program.name)
        - Periodization: \(program.periodization)
        - Week: \(program.weekNumber) | Frequency: \(program.frequencyPerWeek)×/week
        """
    }

    private func personalRecordsSection(_ prs: [PRSnapshot]) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short

        var lines = ["## Personal Records"]
        for pr in prs.prefix(10) {
            let dateStr = formatter.string(from: pr.date)
            lines.append("- \(pr.exerciseName): \(String(format: "%.1f", pr.weightKg))kg × \(pr.reps) (\(dateStr))")
        }
        return lines.joined(separator: "\n")
    }

    private func rpeTrendsSection(_ workouts: [WorkoutSnapshot]) -> String {
        let exercisesWithRpe = workouts.flatMap(\.exercises).filter { $0.avgRpe != nil }
        guard !exercisesWithRpe.isEmpty else { return "" }

        let allRpes = exercisesWithRpe.compactMap(\.avgRpe)
        let overallAvg = allRpes.reduce(0, +) / Double(allRpes.count)

        var lines = ["## RPE Trends"]
        lines.append("- Overall average RPE (recent): \(String(format: "%.1f", overallAvg))")

        // Flag high-RPE exercises (avg ≥ 8.5 = potential fatigue)
        let fatigued = Dictionary(grouping: exercisesWithRpe, by: \.name)
            .compactMap { name, snapshots -> (String, Double)? in
                let avg = snapshots.compactMap(\.avgRpe).reduce(0, +) / Double(snapshots.count)
                return avg >= 8.5 ? (name, avg) : nil
            }

        if !fatigued.isEmpty {
            lines.append("- ⚠ High-effort exercises (avg RPE ≥ 8.5):")
            for (name, avg) in fatigued {
                lines.append("  * \(name): avg RPE \(String(format: "%.1f", avg))")
            }
        }

        return lines.joined(separator: "\n")
    }

    private var safetyRules: String {
        """
        ## Safety Rules (MANDATORY)
        - NEVER provide medical advice, injury diagnosis, or treatment recommendations.
        - NEVER suggest exercises that could be dangerous without proper coaching.
        - If someone describes pain or injury, advise them to consult a qualified medical professional.
        - Always include this disclaimer when giving training advice: \
        "This is AI-generated guidance, not medical advice. Consult a qualified professional for health concerns."
        - If asked about supplements or PEDs, only provide general educational information and recommend consulting a doctor.
        """
    }
}
