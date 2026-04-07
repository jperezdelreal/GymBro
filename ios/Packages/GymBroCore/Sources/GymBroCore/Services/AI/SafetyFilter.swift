import Foundation

/// Filters user messages and AI responses for safety concerns.
public struct SafetyFilter {

    public init() {}

    // MARK: - Input filtering

    /// Check if a user message triggers safety concerns that need special handling.
    public func checkUserMessage(_ message: String) -> SafetyResult {
        let lowered = message.lowercased()

        for pattern in medicalPatterns {
            if lowered.contains(pattern) {
                return .flagged(
                    category: .medical,
                    advisory: "I'm not qualified to give medical advice. Please consult a doctor or physiotherapist for injury/pain concerns."
                )
            }
        }

        for pattern in dangerousPatterns {
            if lowered.contains(pattern) {
                return .flagged(
                    category: .dangerous,
                    advisory: "I can't recommend potentially dangerous practices. Please work with a qualified coach for advanced techniques."
                )
            }
        }

        return .safe
    }

    /// Append a disclaimer to AI responses when discussing training advice.
    public func appendDisclaimerIfNeeded(_ response: String) -> String {
        let disclaimer = "\n\n*⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*"
        if response.contains(disclaimer) { return response }
        return response + disclaimer
    }

    // MARK: - Patterns

    private let medicalPatterns = [
        "diagnose", "diagnosis", "torn", "rupture", "fracture",
        "sharp pain", "shooting pain", "numbness", "tingling",
        "herniated", "slipped disc", "surgery", "medication",
        "prescription", "dosage", "steroid cycle", "trt dose"
    ]

    private let dangerousPatterns = [
        "max out alone", "no spotter", "ego lift",
        "train through injury", "ignore pain"
    ]
}

public enum SafetyResult {
    case safe
    case flagged(category: SafetyCategory, advisory: String)
}

public enum SafetyCategory {
    case medical
    case dangerous
}
