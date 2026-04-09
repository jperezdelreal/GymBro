# Paparazzi Visual Regression Testing Setup

This document describes the Paparazzi screenshot testing setup for GymBro's Compose screens.

## Overview

Paparazzi is a snapshot testing library from Cash App that renders Compose views off-screen (no emulator needed) and provides pixel-perfect comparison for visual regression testing.

## What's Been Added

### 1. Build Configuration

**android/gradle/libs.versions.toml:**
- Added `paparazzi = "1.3.4"` version
- Added `paparazzi` library reference
- Added `paparazzi` plugin reference

**android/build.gradle.kts:**
- Applied `paparazzi` plugin at root level (with `apply false`)

**android/feature/build.gradle.kts:**
- Applied `paparazzi` plugin to feature module
- Added `testImplementation(libs.paparazzi)` dependency
- Added `testImplementation(project(":app"))` for `GymBroTheme` access

### 2. Screenshot Tests Created

Four screenshot test files covering critical screens:

1. **OnboardingScreenshotTest.kt** - `/feature/onboarding/screenshots/`
   - Tests initial state, with user name, and with LBS selected
   
2. **ExerciseLibraryScreenshotTest.kt** - `/feature/exerciselibrary/screenshots/`
   - Tests with exercises list, loading state, search, and muscle group filter

3. **ActiveWorkoutScreenshotTest.kt** - `/feature/workout/screenshots/`
   - Tests empty workout and workout with exercises and sets
   - Simplified from full screen rendering due to complex dependencies

4. **ProgressScreenshotTest.kt** - `/feature/progress/screenshots/`
   - Tests progress dashboard with data and loading state
   - Simplified from full screen rendering due to private visibility

## Usage

### Record Baseline Screenshots

Run this command to generate golden images that will be committed to the repo:

```bash
cd android
.\gradlew.bat :feature:recordPaparazziDebug
```

This creates snapshot PNG files in: `android/feature/src/test/snapshots/`

### Verify Against Baselines

Run this to compare current rendering against baselines:

```bash
.\gradlew.bat :feature:verifyPaparazziDebug
```

Any differences will cause the test to fail and generate a diff image.

### View Test Reports

After running tests, view the HTML report at:
```
android/feature/build/reports/paparazzi/index.html
```

## Notes for Manual Verification

- **Some screens have complex dependencies** (VoiceRecognitionService, UserPreferences) that make full-screen snapshot testing challenging in JVM tests
- The simplified tests capture state models and theme application, which is valuable for detecting theme regressions
- For full UI testing of interactive screens, consider complementary approach with Android instrumented tests

## CI Integration

Paparazzi runs on JVM (no emulator required), making it ideal for CI:
- Fast execution
- No Android device/emulator needed
- Deterministic rendering

Add to CI workflow:
```yaml
- name: Run Paparazzi screenshot tests
  run: ./gradlew verifyPaparazziDebug
```

## Troubleshooting

If you encounter "Unresolved reference 'GymBroTheme'" errors:
- Ensure `testImplementation(project(":app"))` is in feature/build.gradle.kts
- Clean and rebuild: `.\gradlew.bat clean :feature:testDebugUnitTest`

## Next Steps

1. Run `recordPaparazziDebug` to generate baseline screenshots
2. Verify baselines look correct visually
3. Commit baseline images to Git
4. Set up CI to run `verifyPaparazziDebug` on PRs
5. Consider adding more test cases for:
   - Different screen sizes (tablet, foldable)
   - Dark/light theme variants (if implemented)
   - Accessibility font scaling
   - Error states
