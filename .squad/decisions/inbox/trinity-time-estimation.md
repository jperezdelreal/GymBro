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

## Constants (tunable — calibrated against training-domain SKILL.md)

| Constant | Value | Source | Rationale |
|---|---|---|---|
| WARMUP_TIME_SECONDS | 300 (5 min) | Skill: "5-10 min general warmup" | Low end; specific warmup is per-exercise |
| COOLDOWN_TIME_SECONDS | 180 (3 min) | Skill: static stretching post-workout | Minimum viable cooldown |
| TRANSITION_TIME_COMPOUND | 150 (2.5 min) | Real-world | Load plates, set rack height, safety pins |
| TRANSITION_TIME_ISOLATION | 60 (1 min) | Real-world | Grab dumbbells, adjust machine seat |
| REST_TIME_STRENGTH | 240s (4 min) | Skill: "3-5 min (full ATP-PC recovery)" | Mid-range; serious lifters need this |
| REST_TIME_HYPERTROPHY | 90s | Skill: "60-90s (metabolic stress)" | Upper end; compounds need it |
| REST_TIME_ENDURANCE | 45s | Skill: "30-60s (keeps HR elevated)" | Mid-range |
| REST_TIME_POWER | 300s (5 min) | Skill: "3-5 min" + near-max recovery needs | Full ATP-PC for PL singles |
| REP_DURATION_COMPOUND | 5s/rep | Skill: TUT 30-60s → 5×5s=25s + rest | Accounts for brace, eccentric, grind |
| REP_DURATION_ISOLATION | 3s/rep | Skill: TUT 30-60s → 12×3s=36s ✓ | Controlled tempo, MMC focus |
| MIN_EXERCISES | 3 | Practical | Even 15-min sessions get 3 exercises |
| MAX_EXERCISES | 12 | Practical | Diminishing returns beyond this |

## Rest Scaling for Isolation/Accessories

Flat deductions (e.g., "rest - 30") produce unrealistic results for high-rest goals:
- Strength 240 - 30 = **210s curls** ← absurd
- Power 300 - 45 = **255s cable flies** ← nobody does this

**Solution:** Proportional scaling:
- Isolation: `max(rest × 0.60, 45)` → Str: 144s, Hyp: 54s, Gen: 45s
- Accessory: `max(rest × 0.45, 30)` → Str: 108s, Hyp: 40s, Gen: 30s

Validated against skill: "60-90s for accessories" in BULK matches hyp accessory=40-45s range.

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
