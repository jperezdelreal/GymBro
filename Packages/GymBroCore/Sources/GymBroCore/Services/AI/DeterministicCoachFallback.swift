import Foundation

/// Offline fallback: deterministic rule-based responses when no network is available.
/// Covers basic training questions without requiring an API call.
public final class DeterministicCoachFallback: AICoachService {

    public init() {}

    public func sendMessage(_ message: String, context: CoachContext) async throws -> String {
        let response = generateResponse(for: message, context: context)
        return response
    }

    public func streamMessage(_ message: String, context: CoachContext) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                let response = self.generateResponse(for: message, context: context)
                // Simulate streaming by yielding words
                let words = response.split(separator: " ")
                for word in words {
                    continuation.yield(String(word) + " ")
                    try? await Task.sleep(nanoseconds: 30_000_000) // 30ms per word
                }
                continuation.finish()
            }
        }
    }

    // MARK: - Rule Engine

    private func generateResponse(for message: String, context: CoachContext) -> String {
        let lowered = message.lowercased()

        // Safety first
        let safety = SafetyFilter()
        if case .flagged(_, let advisory) = safety.checkUserMessage(message) {
            return advisory
        }

        // Pattern matching for common questions
        if matchesAny(lowered, patterns: ["rest time", "how long", "rest between", "rest period"]) {
            return restTimeAdvice(context: context)
        }

        if matchesAny(lowered, patterns: ["deload", "recovery week", "take a break"]) {
            return deloadAdvice(context: context)
        }

        if matchesAny(lowered, patterns: ["plateau", "stuck", "not progressing", "stalled"]) {
            return plateauAdvice(context: context)
        }

        if matchesAny(lowered, patterns: ["warm up", "warmup", "warm-up"]) {
            return warmupAdvice()
        }

        if matchesAny(lowered, patterns: ["rpe", "rir", "rate of perceived", "reps in reserve"]) {
            return rpeExplanation()
        }

        if matchesAny(lowered, patterns: ["progressive overload", "add weight", "increase weight"]) {
            return progressiveOverloadAdvice(context: context)
        }

        if matchesAny(lowered, patterns: ["what should i do", "suggest", "recommend", "next exercise"]) {
            return workoutSuggestion(context: context)
        }

        return offlineFallbackMessage()
    }

    private func matchesAny(_ text: String, patterns: [String]) -> Bool {
        patterns.contains { text.contains($0) }
    }

    // MARK: - Canned Responses

    private func restTimeAdvice(context: CoachContext) -> String {
        """
        **Rest Time Guidelines** (offline mode)

        - **Compound lifts** (squat, bench, deadlift): 3-5 min for strength, 2-3 min for hypertrophy
        - **Isolation exercises**: 60-90 seconds
        - **Accessory work**: 30-60 seconds

        RPE matters: if your last set was RPE 9+, take the full rest. Recovery drives adaptation.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func deloadAdvice(context: CoachContext) -> String {
        let level = context.userProfile?.experienceLevel ?? "intermediate"
        let frequency = level == "advanced" || level == "elite" ? "every 4-6 weeks" : "every 6-8 weeks"

        return """
        **Deload Recommendations** (offline mode)

        Based on your experience level (\(level)), consider deloading \(frequency).

        **Options:**
        1. **Volume deload**: Keep intensity, reduce sets by 40-50%
        2. **Intensity deload**: Keep volume, reduce weight by 10-15%
        3. **Full deload**: Reduce both by 30-40%

        Signs you need a deload: persistent fatigue, joint aches, declining performance for 2+ sessions.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func plateauAdvice(context: CoachContext) -> String {
        """
        **Breaking Through Plateaus** (offline mode)

        **Quick fixes to try:**
        1. Add a set (volume increase)
        2. Add a rep before adding weight (micro-progression)
        3. Change rep range temporarily (e.g., 5×5 → 3×8)
        4. Improve sleep and nutrition first
        5. Take a deload week, then push hard

        **If stuck for 3+ weeks:** Consider changing your program's periodization or exercise variations.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func warmupAdvice() -> String {
        """
        **Warm-Up Protocol** (offline mode)

        1. **General**: 5 min light cardio (bike, rowing)
        2. **Dynamic stretches**: Leg swings, arm circles, hip circles
        3. **Specific warm-up sets**:
           - Empty bar × 10
           - 50% working weight × 8
           - 70% working weight × 5
           - 85% working weight × 3
           - 90% working weight × 1 (optional for heavy days)

        Never skip warm-ups. Cold muscles under heavy load = injury risk.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func rpeExplanation() -> String {
        """
        **RPE / RIR Scale** (offline mode)

        | RPE | RIR | Description |
        |-----|-----|-------------|
        | 10  | 0   | Maximum effort, no reps left |
        | 9   | 1   | Could do 1 more rep |
        | 8   | 2   | Could do 2 more reps |
        | 7   | 3   | Speed starts to slow |
        | 6   | 4+  | Moderate effort |

        **For most training:** Target RPE 7-8 for working sets. RPE 9-10 only for testing or competition.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func progressiveOverloadAdvice(context: CoachContext) -> String {
        let level = context.userProfile?.experienceLevel ?? "intermediate"

        let increment: String
        switch level {
        case "beginner": increment = "2.5kg per session"
        case "intermediate": increment = "2.5kg per week"
        case "advanced", "elite": increment = "1-2.5kg per month"
        default: increment = "2.5kg per week"
        }

        return """
        **Progressive Overload** (offline mode)

        For \(level) lifters, target approximately **\(increment)** on compound lifts.

        **Methods:**
        1. Add weight (most direct)
        2. Add reps at same weight
        3. Add sets (volume)
        4. Improve bar speed at same weight
        5. Reduce rest times (density)

        Track everything. If you can't add weight, add a rep. If you can't add a rep, add a set.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func workoutSuggestion(context: CoachContext) -> String {
        """
        **Workout Suggestions** (offline mode)

        I need an internet connection to generate personalized workout suggestions based on your training history.

        **General guidelines while offline:**
        - Follow your current program if active
        - If no program: Upper/Lower or Push/Pull/Legs split
        - Start with compound movements, end with isolation
        - 3-5 working sets per exercise, RPE 7-8

        Connect to the internet for AI-powered personalized recommendations.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }

    private func offlineFallbackMessage() -> String {
        """
        **Offline Mode** 📡

        I'm running in offline mode with limited capabilities. I can help with:
        - Rest time guidelines
        - Deload recommendations
        - Plateau advice
        - Warm-up protocols
        - RPE/RIR explanation
        - Progressive overload tips

        For personalized coaching, training analysis, and complex questions, please connect to the internet.

        *⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*
        """
    }
}
