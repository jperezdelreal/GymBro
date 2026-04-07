# Decision: Android Skills Installed

**Author:** Tank (Backend Dev)
**Date:** Auto
**Status:** Implemented
**PR:** #141

## Context

Morpheus analyzed 4 source repos and recommended 5 P0 Android skills for GymBro's dual-platform expansion (Issue #134). Tank installed all 5 from their upstream GitHub repos.

## Decision

Installed 5 Android agent skills to `.squad/skills/android/`:

1. **compose-expert** (aldefy/compose-skill) — Premier Compose skill with actual androidx source code backing. Includes 4 reference files (state-management, performance, navigation, production-crash-playbook).
2. **android-architecture** (new-silvermoon/awesome-android-agent-skills) — Clean Architecture + Hilt + modularization.
3. **android-data-layer** (new-silvermoon/awesome-android-agent-skills) — Repository pattern + Room + offline-first sync.
4. **kotlin-mvi** (Meet-Miyani/compose-skill) — MVI Event/State/Effect pattern, Ktor, Paging, Room.
5. **android-testing** (new-silvermoon/awesome-android-agent-skills) — JUnit, Roborazzi screenshot tests, Hilt testing.

## Implications

- All agents building Android features should consult these skills before writing code.
- GymBro-specific notes were added to skills where relevant (mapping iOS patterns to Android equivalents).
- The compose-expert skill has a large reference tree — additional reference files can be pulled from the source repo as needed.
- Skills are under `.squad/skills/android/` to keep them separate from iOS-specific content.
