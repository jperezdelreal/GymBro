# Decision: Exercise Suitability Tags (Nivel 2)

**By:** Neo (AI/ML Engineer)  
**Date:** 2026-04-08  
**Status:** Implemented  

## Context

Nivel 1 (equipment priority sorting in `buildExerciseList`) solved the equipment-ordering problem but didn't prevent powerlifting-specific technique exercises from appearing in hypertrophy plans. Board Press, Spoto Press, Larsen Press, and Pause Bench Press all use barbell (priority 0) and are compound movements, so they ranked highly in hypertrophy exercise selection despite being inappropriate for muscle growth goals.

## Decision

Add a `suitability` metadata field to every exercise in `exercises-seed.json`. Each exercise gets an array of applicable training goals: `["strength", "hypertrophy", "powerlifting", "general_fitness"]`. The WorkoutPlanGenerator filters exercises by suitability before equipment priority sorting.

## Classification Rules

| Exercise Type | Suitability Tags |
|---|---|
| Standard barbell compounds (Bench, Squat, Deadlift, OHP, Row) | All 4 goals |
| Dumbbell compounds | strength, hypertrophy, general_fitness |
| Powerlifting variants (Board Press, Spoto Press, Larsen Press, Pause Bench, Reverse Band) | powerlifting only, or powerlifting + strength |
| Cable/machine exercises | hypertrophy, general_fitness |
| Isolation exercises | hypertrophy, general_fitness |
| Olympic lifts | strength, powerlifting |
| Advanced bodyweight (Archer Push-Up, Ring exercises) | general_fitness only |
| Skill/gymnastics (Lever, Planche, Muscle-Up) | empty (already in UNSUITABLE_FOR_PLANS) |

## Implementation

1. **Data layer:** `suitability` array added to all 209 exercises in JSON seed — no Room schema change
2. **Runtime:** Suitability map loaded lazily from seed JSON via `org.json.JSONArray` on first plan generation
3. **Filter:** `isSuitableForGoal()` applied in all 4 phases of `buildExerciseList` + `adjustDayForDuration` extras
4. **Backward compatible:** Missing suitability data → exercise treated as suitable for all goals

## Implications

- **Morpheus/Trinity:** No UX changes needed — filtering is invisible to users
- **Tank:** No database migration — suitability lives in seed JSON only
- **Neo:** Foundation for Nivel 3 (per-exercise priority scoring within a goal) if needed
- **Switch:** Unit tests for `isSuitableForGoal` can be added to verify classification correctness
