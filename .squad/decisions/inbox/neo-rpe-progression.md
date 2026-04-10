# RPE-Based Progression Engine Design

**By:** Neo (AI/ML Engineer)  
**Date:** 2026-04-08  
**Issue:** #393  
**PR:** #406  

## Decision

Progression logic uses simple heuristic rules (not ML) based on RPE data:
- **Progress:** All working sets RPE ≤7 → +2.5kg
- **Regress:** Last 2 sets RPE 10 → −5% (rounded to 2.5kg)
- **Maintain:** RPE 8-9 → same weight

RPE picker is a tap-to-cycle widget (6→7→8→9→10→clear) rather than dropdown or slider, because speed matters during active workouts.

## Implications

- **Trinity (UI):** RPE column is 48dp wide in the set row. If layout feels cramped on smaller screens, we may need to make RPE collapsible or show it only after first set completion.
- **Tank (Data):** Room DB is now version 6. Migration adds `rir INTEGER` column to `workout_sets`.
- **Switch (Testing):** ProgressionEngine and RpeTrendService have unit tests. Integration tests for the full flow (log sets → get suggestion → verify weight) would be valuable.
- **Neo (Future):** This heuristic engine is the foundation. Future ML-based autoregulation can replace the rules with a trained model once we have enough RPE data to train on.

## Rationale

Heuristics before ML — a well-tuned rule set is more explainable and debuggable than a black-box model. Users can understand "your RPE was low, so we increased weight" far better than "the model predicted you should increase weight."
