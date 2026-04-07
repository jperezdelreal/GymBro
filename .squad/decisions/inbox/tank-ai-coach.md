# Decision: AI Coach Backend Architecture (Issue #10)

**By:** Tank (Backend Dev)
**Date:** 2026-04-07
**Status:** Implemented (PR #23)

## Decisions Made

### 1. Protocol-based LLM Abstraction
**Decision:** `AICoachService` protocol with cloud and offline implementations.
**Rationale:** Future-proofs for on-device LLM migration (Core AI / iOS 27) without touching ViewModel or UI code. Swap the implementation, not the interface.

### 2. Environment-based API Configuration
**Decision:** No hardcoded API keys. `AICoachConfiguration.fromEnvironment()` reads from `ProcessInfo.processInfo.environment`.
**Rationale:** Security best practice. Xcode schemes, .xcconfig files, or CI env vars can supply credentials. Never in source control.

### 3. Safety-First Filter Pipeline
**Decision:** Client-side `SafetyFilter` intercepts medical/dangerous queries before they hit the API, plus mandatory disclaimers on all responses.
**Rationale:** Saves API costs on filtered queries. Dual-layer safety (client filter + system prompt instructions) prevents unsafe outputs even if one layer fails.

### 4. Lightweight Snapshot DTOs
**Decision:** Created `UserProfileSnapshot`, `WorkoutSnapshot`, etc. instead of passing `@Model` objects to AI services.
**Rationale:** SwiftData `@Model` objects aren't `Sendable` and are bound to their `ModelContext` thread. Snapshots are plain structs, thread-safe, and testable without a database.

### 5. Streaming-First Response Design
**Decision:** Both cloud and offline services implement `AsyncThrowingStream<String, Error>`.
**Rationale:** Consistent UX — users see tokens arriving regardless of backend. Offline simulates streaming with word-level delays.

## Team Impact
- **Trinity:** CoachChatView is functional but minimal. Trinity should polish styling, animations, and keyboard handling.
- **Neo:** PromptBuilder is ready for richer context — plateau detection data, periodization recommendations can be injected via `CoachContext`.
- **Switch:** 24 unit tests covering prompt building, safety filtering, and offline fallback. Integration tests needed for Azure connectivity.
- **Morpheus:** Free tier (5 questions/week) and premium (unlimited) are enforced. Pricing decision implemented.

## Open Items for Team
- Azure Functions proxy (server-side rate limiting, prompt versioning) — deferred to production hardening
- Multi-turn conversation context (sending chat history to API) — needs team decision on token budget
- On-device LLM timeline — depends on Apple's Core AI announcements at WWDC 2026
