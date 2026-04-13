# Orchestration: Switch — Maestro E2E Five-Workout Journey (2026-04-14T14:30)

**Agent:** Switch (Tester)  
**Mode:** Background  
**Status:** Complete ✅  
**Result:** 489 steps passed, 0 failures  

## Task

Execute comprehensive 5-workout realistic user simulation via Maestro E2E flow (`android/.maestro/realistic-5workout-journey.yaml`).

## Outcomes

**Test Results:**
- Total steps: 489
- Passed: 489 ✅
- Failed: 0 ✅
- Execution time: ~12 minutes (emulator warmup + realistic delays)

**Coverage:**
- Onboarding (4 workouts; different goals/exercises)
- Workout logging (set completion, weight/rep adjustments, PR detection)
- History tracking (workout persistence, volume display)
- Profile state (persistence across app relaunch)
- Analytics (weekly summary, consistency metrics)

## Critical Issues Found

### 🐛 BUG: Volume Integer Overflow (HIGH SEVERITY)

**Symptom:** Workout summary and History display **Integer.MAX_VALUE (2,147,483,647 kg)** instead of actual volume.

**Reproduction:** 100% consistent on every workout completion.
- Expected: Bench Press 70kg × (10 + 9 + 8) = 1,890 kg
- Actual: 2,147,483,647 kg

**Root Cause Hypothesis:**
- Weight stored in smaller unit (grams/milligrams) and multiplied by reps/sets
- Or volume calculation uses `Int` instead of `Long` in data pipeline

**Screenshots:**
- j15_workout1_summary.png (first overflow)
- j16_history_1workout.png (history card overflow)

**Recommendation:**
1. Search volume calculation code (likely ActiveWorkoutViewModel, WorkoutSummaryScreen, repository)
2. Change `Int` → `Long` for volume fields
3. Add unit tests for realistic weights
4. Verify historical data integrity

## Minor Issues Found

### ℹ️ Profile Screen Layout Changed

New layout: "Not signed in" header + stats, "Talk to AI Coach", "Progress & Stats", "Account", "Settings"  
Old Maestro flows have stale assertions for removed sections ("Cuenta", "Preferencias", "Acerca de").

### ℹ️ Language Default: English Despite es-ES Emulator

App displays English text even with es-ES locale when launched with `clearState: true`. Language selection tied to app preferences (cleared by `clearState`), not system locale.  
Impact: Spanish-only text selectors fail; all selectors need bilingual regex patterns.

## Files Modified

- `android/.maestro/realistic-5workout-journey.yaml` — Comprehensive 5-workout flow with realistic delays
- `.squad/decisions/inbox/switch-e2e-bugs.md` — Detailed bug report (moved to decisions)

## Next Steps

- **BLOCKER:** Fix volume integer overflow before prod launch
- Update Maestro flows with new Profile layout assertions
- Consider localizing Maestro selectors for non-English testing
