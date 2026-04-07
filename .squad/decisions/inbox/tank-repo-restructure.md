# Decision: Repo Restructure for Dual-Platform

**Author:** Tank (Backend Dev)
**Date:** 2025-07-04
**Issue:** #133
**PR:** #142

## What Changed

Restructured the GymBro monorepo from a single-platform iOS layout to a dual-platform structure:

```
Before:                     After:
GymBro/                     ios/GymBro/
GymBroWatch/                ios/GymBroWatch/
GymBroWidgets/              ios/GymBroWidgets/
Packages/                   ios/Packages/
Package.swift               ios/Package.swift
                            android/          (new)
                            shared/data/      (new)
.squad/skills/{flat}        .squad/skills/ios/
                            .squad/skills/android/
                            .squad/skills/shared/
```

## Key Decisions

1. **Package.swift paths unchanged** — relative paths from `ios/Package.swift` to `ios/Packages/*` are identical since they moved together. No code changes.

2. **Seed data stays in iOS packages** — `exercises-seed.json` and `programs-seed.json` are loaded via Swift `Bundle.module` in `ExerciseDataSeeder.swift` and `ProgramSeeder.swift`. Moving them would break the build. Future task: create a shared data loader and copy seed data to `shared/data/`.

3. **No .xcodeproj/.xcworkspace** — the project uses SPM exclusively, so no Xcode project file references to update.

4. **`.swiftlint.yml` stays at repo root** — CI runs `swiftlint lint --strict` from the repo root. Moving it would break CI. May need adjusting later to scope to `ios/`.

5. **CI will need updates** — `.github/workflows/ci.yml` runs `xcodebuild` and `swiftlint` from the repo root. After this restructure, the build step needs `cd ios` or a `-project` flag update. Filed as follow-up work, not blocking the restructure.

6. **Skills reorganized into platform subdirs** — 26 iOS skills → `.squad/skills/ios/`, 13 shared skills → `.squad/skills/shared/`, Android skills already in `.squad/skills/android/`.

## Follow-Up Tasks

- [ ] Update `.github/workflows/ci.yml` to build from `ios/` directory
- [ ] Create shared data loading layer for cross-platform seed data
- [ ] Scaffold Android project structure in `android/`
