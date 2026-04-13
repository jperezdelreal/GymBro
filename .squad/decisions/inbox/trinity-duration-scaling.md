# Duration Scaling Architecture

**By:** Trinity (Mobile Dev)
**Date:** 2026-04-13

## Decision

Workout duration now dynamically controls **both** exercise count AND set volume, not just sets.

### Scaling Tiers (getMaxExercisesForDuration)
| Duration | Exercises | Sets Multiplier |
|----------|-----------|-----------------|
| 15-20min | 3         | 0.6x            |
| 21-30min | 4         | 0.75x           |
| 31-45min | 5         | 0.85x           |
| 46-60min | 6         | 1.0x (base)     |
| 61-75min | 7         | 1.15x           |
| 76-90min | 8         | 1.15x           |
| 91-105min| 9         | 1.3x            |
| 106-120min| 10       | 1.3x            |

### Exercise Selection Priority
`buildExerciseList()` now fills slots in 4 phases:
1. **Compound** (1 per muscle group) — always first
2. **Isolation** (1 per muscle group) — reduced sets/rest
3. **Accessory** (1 per muscle group) — lighter volume
4. **Extra compound variants** — fills remaining slots

### On-the-fly Duration Adjustment
New public API: `WorkoutPlanGenerator.adjustDayForDuration(day, minutes, phase)`.
Used by `PlanDayDetailViewModel` when user taps duration chips.
Adjusts existing exercises (trims or extends) without regenerating the full plan.

## Implications
- Any new training goal generators must use `getMaxExercisesForDuration()` and `getBaseSetsForDuration()`
- Template programs are NOT affected — they have fixed exercise lists
- Volume multiplier (BULK/CUT/MAINTENANCE) is applied on top of duration scaling
