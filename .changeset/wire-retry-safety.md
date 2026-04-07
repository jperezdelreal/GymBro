---
'gymbro': patch
---

Wire RetryingAICoachService, SafetyFilter, and DeterministicCoachFallback into CoachChatViewModel pipeline. Shows subtle "Retrying..." indicator during transient errors instead of immediate error; only surfaces error after all retries exhausted.
