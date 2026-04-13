# Decision: Bottom-Up Time Estimation for Workout Plan Generation

**Author:** Trinity (Mobile Dev)
**Date:** 2026-04-14
**Status:** Implemented

## Context

WorkoutPlanGenerator used a rigid lookup table to map session duration → exercise count. This produced identical exercise counts regardless of training goal, which is incorrect — strength training with 3-min rest periods fits far fewer exercises than hypertrophy with 90s rest in the same session length.

## Decision

Replace the lookup table with a bottom-up time estimation model. Each exercise's duration is calculated from its physical parameters:

```
exerciseTime = (sets × (reps × repDuration + restTime)) + transitionTime
```

The generator accumulates exercises until the time budget (session − warmup − cooldown) is exhausted.

## Constants (tunable)

| Constant | Value | Purpose |
|---|---|---|
| WARMUP_TIME_SECONDS | 300 (5 min) | Pre-workout warmup |
| COOLDOWN_TIME_SECONDS | 180 (3 min) | Post-workout cooldown |
| TRANSITION_TIME_SECONDS | 120 (2 min) | Equipment setup between exercises |
| REST_TIME_STRENGTH | 180s | Heavy compound rest |
| REST_TIME_HYPERTROPHY | 90s | Moderate load rest |
| REST_TIME_ENDURANCE | 45s | Light, high-rep rest |
| REST_TIME_POWER | 240s | Explosive, full recovery |
| REP_DURATION_COMPOUND | 4s/rep | Compound movement tempo |
| REP_DURATION_ISOLATION | 3s/rep | Isolation movement tempo |
| MIN_EXERCISES | 3 | Floor (even 15-min sessions) |
| MAX_EXERCISES | 12 | Ceiling (diminishing returns) |

## Goal-Specific Base Sets

| Goal | Base Sets | Typical Rep Range |
|---|---|---|
| Strength | 5 | 3-5 |
| Powerlifting | 5 | 1-5 |
| Hypertrophy | 4 | 8-12 |
| General Fitness | 3 | 10-15 |

## Implications

- **Volume multiplier** (BULK 1.2x / CUT 0.8x / MAINTENANCE 1.0x) is applied AFTER exercise selection to avoid distorting the time budget
- `adjustDayForDuration()` now requires a `goal` parameter for accurate re-estimation
- A 60-min strength session yields ~4 exercises; a 60-min hypertrophy session yields ~6-7
- Constants are in a companion object — easy to tune without touching algorithm logic
