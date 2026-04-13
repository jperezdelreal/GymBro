# Orchestration: Trinity — Duration Scaling with Bottom-Up Time Estimation (2026-04-14T13:45)

**Agent:** Trinity (Mobile Dev)  
**Mode:** Background  
**Task:** Rework workout plan duration scaling with bottom-up time estimation  
**Status:** Complete ✅  

## Task

Replace rigid duration-to-exercise-count lookup table with bottom-up time estimation model in `WorkoutPlanGenerator`.

### Problem

Previous implementation used fixed mappings:
- 15min session → 3 exercises (always)
- 30min session → 4 exercises (always)
- 60min session → 5 exercises (always)

This ignored:
- Goal-specific rest periods (strength=240s vs hypertrophy=90s)
- Exercise type (compound vs isolation have different time budgets)
- Rep ranges (3-5 reps vs 8-12 reps change duration)
- Realistic set/rep/rest calculations

Result: **Identical exercise counts** for completely different training styles in the same session length.

## Solution

### Algorithm

Per-exercise duration calculated bottom-up:

```
exerciseTime = (sets × (reps × repDuration + restTime)) + transitionTime
```

Generator accumulates exercises until session time budget exhausted:
```
remainingTime = sessionDuration - warmupTime - cooldownTime
until (remainingTime < nextExerciseEstimate):
  add exercise
  remainingTime -= exerciseTime
```

### Tuning Constants

Constants calibrated against Neo's **Training Domain** skill:

| Constant | Value | Source |
|---|---|---|
| WARMUP_TIME_SECONDS | 300 | "5-10 min general warmup" |
| COOLDOWN_TIME_SECONDS | 180 | Static stretching minimum |
| TRANSITION_TIME_COMPOUND | 150 | Real-world load/setup time |
| TRANSITION_TIME_ISOLATION | 60 | Quick dumbbell grab |
| REST_TIME_STRENGTH | 240 | "3-5 min (ATP-PC recovery)" |
| REST_TIME_HYPERTROPHY | 90 | "60-90s (metabolic stress)" |
| REST_TIME_ENDURANCE | 45 | "30-60s (elevated HR)" |
| REST_TIME_POWER | 300 | Full ATP-PC for powerlifting |
| REP_DURATION_COMPOUND | 5s | ~25s per set (5 reps) |
| REP_DURATION_ISOLATION | 3s | ~36s per set (12 reps) |
| MIN_EXERCISES | 3 | Even 15-min gets 3 |
| MAX_EXERCISES | 12 | Diminishing returns |

### Rest Time Scaling for Accessories

Isolation/accessory exercises use proportional scaling:
- Isolation: `max(rest × 0.60, 45)`  
- Accessory: `max(rest × 0.45, 30)`

Prevents absurd values like "245s for cable flyes in strength goal."

### Goal-Specific Base Sets

| Goal | Sets | Rep Range |
|---|---|---|
| Strength | 5 | 3-5 |
| Powerlifting | 5 | 1-5 |
| Hypertrophy | 4 | 8-12 |
| General Fitness | 3 | 10-15 |

Volume multiplier (BULK 1.2×, CUT 0.8×, MAINTENANCE 1.0×) applied **after** exercise selection to avoid distorting time budget.

## Results

### Validation

11 tests pass covering:
- Time budget exhaustion (no underflow)
- Goal-specific exercise counts (strength=fewer, hypertrophy=more for same duration)
- Realistic 60-min sessions (strength~4 exercises, hypertrophy~6-7 exercises)
- Rest scaling sanity

### Impact

- 60-min strength session yields ~4 exercises (vs rigid 5)
- 60-min hypertrophy session yields ~6-7 exercises (vs rigid 5)
- Session goal directly affects exercise selection, not just volume multiplier
- Easy to tune: constants in companion object, algorithm untouched

## Files Modified

**GymBroCore:**
- WorkoutPlanGenerator.kt (new `estimateExerciseTimeSeconds()` function, bottom-up logic)
- WorkoutPlanGeneratorTest.kt (11 new test cases)

## Next Steps

- Integrate with UI duration selector (already added to PlanDayDetailScreen)
- Ready for production merge
- Decision documentation saved to `.squad/decisions/` for team reference
