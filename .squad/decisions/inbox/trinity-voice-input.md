# Voice Input UX Placement Decision

**By:** Trinity (iOS/Android Dev)
**Date:** 2026-04-10
**Issue:** #392

## Decision

Voice input button is placed in the **exercise card header** (next to delete icon), not per-set-row. When triggered, it auto-fills the **first incomplete set** for that exercise.

## Rationale

- Adding a mic button to every SetRow creates visual clutter in an already compact layout (Set# | Weight | Reps | Complete).
- One mic per exercise is sufficient — users typically voice-log the current working set.
- Auto-targeting the first incomplete set matches natural workout flow (sets are completed in order).
- Feedback toast shows parsed result beneath the header so user can verify before completing.

## Permission Flow

Three-state RECORD_AUDIO permission handling:
1. **First request**: Direct system permission dialog
2. **Previously denied**: Rationale dialog explaining why mic is needed, then system dialog
3. **Permanently denied**: Dialog with "Open Settings" button redirecting to app settings

## Implications

- All voice input strings must be maintained in both `values/strings.xml` and `values-es/strings.xml`.
- VoiceRecognitionService uses device locale instead of hardcoded en-US — future locale additions only require VoiceInputParser updates.
- The `ActiveWorkoutEvent.VoiceInput` event already existed; no ViewModel changes were needed.
