# Decision: Onboarding Data → Auto-Generated First Program

**By:** Neo (AI/ML Engineer)  
**Date:** 2026-04-10  
**Issue:** #394  
**PR:** #408

## Decision

After onboarding completes, the app now auto-generates a personalized workout plan using the collected data (goal, experience level, training frequency) and routes the user to the Programs screen instead of the Exercise Library.

## Rationale

- Onboarding collected valuable data (goal, experience, frequency) but discarded it — user landed on an empty Exercise Library with no guidance
- The `WorkoutPlanGenerator` already existed and accepted exactly the data onboarding collects
- Routing to Programs with a pre-generated plan creates an immediate payoff for completing onboarding
- Plan generation is fire-and-forget with graceful degradation — if generation fails, onboarding still completes normally

## Architecture

- `OnboardingViewModel` now injects `WorkoutPlanGenerator` + `ActivePlanStore` (both are already Hilt-provided singletons)
- `ActivePlanStore` gained an `isFromOnboarding` flag to distinguish onboarding-generated plans from manual generation
- `ProgramsViewModel` loads the active plan from the store on init, so it's immediately visible on navigation
- Plan is named "Your First Program" to differentiate from manually generated plans

## Implications

- **Trinity (UX):** The post-onboarding destination changed from Exercise Library to Programs. Any onboarding flow changes should verify this routing.
- **Tank (Architecture):** `ActivePlanStore` is in-memory only. If plan persistence to database is needed later, this is the hook point.
- **Switch (QA):** Maestro E2E tests updated to expect Programs screen after onboarding. The `waitForAnimationToEnd` timeout was increased to 5s to account for plan generation time.
