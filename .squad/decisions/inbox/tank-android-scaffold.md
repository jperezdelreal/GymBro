# Android Project Scaffold — Tank

**Date:** 2026-07-XX
**By:** Tank (Backend Dev)

## Decision

Scaffolded the Android project in `android/` with a three-module Gradle architecture:
- `:app` — Entry point (Hilt Application, single Activity, Compose, theme, navigation)
- `:core` — Shared infrastructure (Room database, domain models, Hilt modules, repository interfaces)
- `:feature` — Feature screens (depends on `:core`, owns UI + ViewModels)

## Stack Locked In

| Component | Choice | Version |
|-----------|--------|---------|
| Language | Kotlin | 2.1.21 |
| Build | Gradle + AGP | 8.14.2 / 8.10.1 |
| UI | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| DI | Hilt | 2.56.2 |
| Database | Room | 2.7.1 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Images | Coil 3 | 3.2.0 |
| Background | WorkManager | 2.10.1 |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 36 | — |

## Rationale

- Module structure mirrors iOS SPM packages for team mental model consistency
- Version catalog centralizes all dependency versions for easy upgrades
- Domain models (Exercise, Workout, ExerciseSet) structurally match iOS models
- All weights stored as kg with computed lbs conversion — same as iOS
- Dark-only theme with colors matching iOS palette

## Impact

Unblocks all Android feature development. Any squad member can now build features by adding screens to `:feature` and entities to `:core`.
