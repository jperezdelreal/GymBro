import Foundation

/// Generates natural language coaching messages for deload recommendations.
///
/// Integrates with AI coach to provide context-aware deload messaging based on trigger types,
/// severity, and user training history.
public struct DeloadCoachingMessageGenerator: Sendable {
    
    /// Generate a coaching message for a deload recommendation.
    ///
    /// - Parameters:
    ///   - recommendation: The deload recommendation from DeloadAutomationService
    ///   - userName: Optional user's preferred name for personalization
    /// - Returns: Natural language coaching message suitable for AI coach display
    public static func generateMessage(
        for recommendation: DeloadRecommendation,
        userName: String? = nil
    ) -> String {
        let greeting = userName.map { "Hey \($0), " } ?? ""
        
        switch recommendation.urgency {
        case .none:
            return generateNoDeloadMessage(recommendation: recommendation, greeting: greeting)
        case .recommended:
            return generateRecommendedMessage(recommendation: recommendation, greeting: greeting)
        case .immediate:
            return generateImmediateMessage(recommendation: recommendation, greeting: greeting)
        }
    }
    
    /// Generate a short summary for notification or widget display.
    public static func generateShortSummary(
        for recommendation: DeloadRecommendation
    ) -> String {
        switch recommendation.urgency {
        case .none:
            return "Training load optimal — no deload needed"
        case .recommended:
            let triggers = recommendation.triggers.map { triggerName(for: $0.type) }
            return "Consider deload: \(triggers.joined(separator: ", "))"
        case .immediate:
            return "⚠️ Deload needed — multiple recovery signals detected"
        }
    }
    
    // MARK: - Message Builders
    
    private static func generateNoDeloadMessage(
        recommendation: DeloadRecommendation,
        greeting: String
    ) -> String {
        if recommendation.state == .detraining {
            return """
            \(greeting)your training volume has dropped recently (ACWR: \(String(format: "%.2f", recommendation.acwr))).
            
            This is fine if it was planned (vacation, rest week), but if unintentional, consider increasing training frequency to rebuild work capacity.
            
            No deload needed — focus on consistent training.
            """
        }
        
        return """
        \(greeting)your training load is well-managed. ACWR: \(String(format: "%.2f", recommendation.acwr))
        
        Keep training as planned. I'll monitor your recovery and let you know if a deload becomes necessary.
        """
    }
    
    private static func generateRecommendedMessage(
        recommendation: DeloadRecommendation,
        greeting: String
    ) -> String {
        let primaryTrigger = recommendation.triggers.first
        let triggerContext = primaryTrigger.map { trigger in
            triggerExplanation(for: trigger)
        } ?? ""
        
        return """
        \(greeting)I'm seeing early signs that you could benefit from a deload week.
        
        \(triggerContext)
        
        Current metrics:
        • ACWR: \(String(format: "%.2f", recommendation.acwr)) (ideal range: 0.8-1.3)
        • Training Stress Balance: \(Int(recommendation.trainingStressBalance))
        
        My recommendation: Plan a deload week within the next 7 days.
        
        Options:
        1. **Full deload week** — Reduce volume to 60-70%, maintain weights
        2. **Active recovery** — Take 2-3 days completely off, then resume lighter
        3. **Technique focus** — 50% volume, emphasize perfect form and control
        
        This isn't a step back — it's strategic recovery that prevents plateaus and keeps you progressing long-term.
        """
    }
    
    private static func generateImmediateMessage(
        recommendation: DeloadRecommendation,
        greeting: String
    ) -> String {
        let triggers = recommendation.triggers
            .filter { $0.severity == .high }
            .map { "• \(triggerExplanation(for: $0))" }
            .joined(separator: "\n")
        
        return """
        \(greeting)your body needs recovery. Multiple high-severity signals detected:
        
        \(triggers)
        
        Critical metrics:
        • ACWR: \(String(format: "%.2f", recommendation.acwr)) (\(acwrInterpretation(recommendation.acwr)))
        • Training Stress Balance: \(Int(recommendation.trainingStressBalance))
        
        **Action plan (start this week):**
        
        1. **Reduce volume to 60-70%**
           - Cut 3-4 sets per exercise
           - Keep the same exercises and movement patterns
        
        2. **Maintain intensity**
           - Use 90-95% of your working weights
           - Focus: quality movement, NOT fatigue
        
        3. **Stop sets early**
           - All sets to RPE 6-7 maximum
           - Leave 3-4 reps in the tank
        
        4. **Prioritize recovery**
           - Sleep: 8+ hours nightly
           - Nutrition: adequate calories and protein
           - Stress management: this is a recovery week
        
        This isn't optional — pushing through will lead to injury or prolonged plateau. One week of smart recovery prevents 4+ weeks of forced time off.
        
        Trust the process. You'll come back stronger.
        """
    }
    
    // MARK: - Helpers
    
    private static func triggerName(for type: DeloadTriggerType) -> String {
        switch type {
        case .acwrSpike: return "training spike"
        case .chronicFatigue: return "chronic fatigue"
        case .volumeAccumulation: return "volume accumulation"
        }
    }
    
    private static func triggerExplanation(for trigger: DeloadTrigger) -> String {
        switch trigger.type {
        case .acwrSpike:
            return "**Training load spike**: Your training volume jumped sharply — ACWR at \(String(format: "%.2f", trigger.metric ?? 0)). This pattern increases injury risk 2-4x according to research."
        case .chronicFatigue:
            return "**Chronic fatigue**: Your readiness has been below 40 for 3+ consecutive days (avg: \(Int(trigger.metric ?? 0))). Your body isn't recovering between sessions."
        case .volumeAccumulation:
            if let weeks = trigger.metric {
                return "**Volume accumulation**: \(Int(weeks)) weeks since your last deload. Even without acute fatigue, accumulated training stress builds over time."
            } else {
                return "**Volume accumulation**: You haven't taken a deload yet. Consider planning one after 4-6 weeks of consistent training."
            }
        }
    }
    
    private static func acwrInterpretation(_ acwr: Double) -> String {
        switch acwr {
        case ..<0.7:
            return "very low — detraining risk"
        case 0.7..<0.8:
            return "low — consider increasing volume"
        case 0.8...1.3:
            return "optimal range"
        case 1.3..<1.5:
            return "elevated — monitor closely"
        default:
            return "high risk — spike detected"
        }
    }
}
