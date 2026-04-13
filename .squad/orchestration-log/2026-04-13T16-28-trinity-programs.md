# Orchestration: Trinity (Mobile Dev) — Workout Programs + Dynamic Duration Audit

**Timestamp:** 2026-04-13T16:28:00Z  
**Agent:** Trinity (Mobile Dev)  
**Mode:** Background Task  
**Priority:** P0 (Critical Duration Scaling Bug)

## Task

Audit and fix workout program generation, focusing on the duration scaling bug reported by user:
- **Bug:** Workouts always generate with 5 exercises, regardless of selected duration
- **Root Cause:** `buildExerciseList()` only selecting 1 compound + 1 isolation per muscle group
- **Impact:** Duration slider had no effect on exercise count

## Outcome

✅ **COMPLETE** — Root cause identified, fixed with 4-phase exercise selection, duration selector added

### Root Cause Analysis

`WorkoutPlanGenerator.buildExerciseList()` was capping exercise selection at:
- 1 compound per muscle group
- 1 isolation per muscle group
- **Total:** 5 exercises (fixed, non-scalable)

Duration slider in UI existed but was ignored by the generation logic.

### Solution Implemented

**4-Phase Exercise Selection Model:**

| Phase | What | Constraint | Per Muscle Group |
|-------|------|-----------|-----------------|
| 1 | Compound exercises | Always selected | 1 |
| 2 | Isolation exercises | If space available | 1 |
| 3 | Accessory exercises | If space available | 1 |
| 4 | Extra compound variants | Fills remaining slots | N (as needed) |

**Dynamic Duration Scaling:**

| Duration Range | Exercises | Sets Multiplier |
|---|---|---|
| 15–20 min | 3 | 0.6x |
| 21–30 min | 4 | 0.75x |
| 31–45 min | 5 | 0.85x |
| 46–60 min | 6 | 1.0x (base) |
| 61–75 min | 7 | 1.15x |
| 76–90 min | 8 | 1.15x |
| 91–105 min | 9 | 1.3x |
| 106–120 min | 10 | 1.3x |

### Files Modified
- `WorkoutPlanGenerator.kt` — Refactored `buildExerciseList()` with 4-phase logic
- `ProgramDay.kt` — Enhanced model to support variable exercise counts
- `PlanDayDetailScreen.kt` — Added duration selector chip group
- `PlanDayDetailViewModel.kt` — New `adjustDayForDuration(day, minutes)` method

### Public API

```kotlin
// Adjust a day's exercises and volume for target duration
WorkoutPlanGenerator.adjustDayForDuration(
    day: ProgramDay,
    minutes: Int,
    phase: TrainingPhase
): ProgramDay
```

Used by UI when user taps duration chips (15, 30, 45, 60, 90, 120 min).

### Commit
- **Branch:** Feature branch in development
- **Message:** Fix duration scaling: 4-phase exercise selection, dynamic sets multiplier, duration selector UI

## Technical Decisions

**Saved to `.squad/decisions/inbox/trinity-duration-scaling.md`:**
- New generators must use `getMaxExercisesForDuration()` and `getBaseSetsForDuration()`
- Template programs (fixed exercise lists) NOT affected — only dynamic generation scales
- Volume multiplier (BULK/CUT/MAINTENANCE) stacks on top of duration scaling

---

**Status:** Complete, decision documented
