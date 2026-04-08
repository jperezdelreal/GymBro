# GymBro Maestro Test Flows

This directory contains Maestro UI test flows for the GymBro Android app.

## Quick Start

```bash
# Run all flows
maestro test android/.maestro/

# Run a specific flow
maestro test android/.maestro/start-workout.yaml

# Run flows with a specific tag
maestro test --tags core android/.maestro/
```

## Parametrized Test Data

All flows now support environment variables for test data customization. Each variable has a sensible default, so flows work out-of-the-box without configuration.

### Available Variables

| Variable | Default | Used In | Description |
|----------|---------|---------|-------------|
| `EXERCISE_NAME` | `Bench Press` | Most workout flows | Exercise to select from picker |
| `EXERCISE_SEARCH` | `Bench` | Flows with search | Search query for exercise picker |
| `WEIGHT` | `80` | All workout flows | Weight value for sets |
| `REPS` | `8` | All workout flows | Reps value for sets |
| `USER_NAME` | `TestUser` | Onboarding flows | User name for onboarding |

### Usage Examples

#### Use default values (no configuration needed)
```bash
maestro test android/.maestro/start-workout.yaml
```

#### Override specific values via CLI
```bash
# Test with heavier weight and more reps
maestro test -e WEIGHT=100 -e REPS=12 android/.maestro/start-workout.yaml

# Test with different exercise
maestro test -e EXERCISE_NAME="Squat" -e EXERCISE_SEARCH="Squat" android/.maestro/complete-workout.yaml

# Test onboarding with custom user name
maestro test -e USER_NAME="JohnDoe" android/.maestro/onboarding-flow.yaml
```

#### Override via environment variables
```bash
# Set for single command
export WEIGHT=120 REPS=5
maestro test android/.maestro/start-workout.yaml

# Or inline
WEIGHT=150 REPS=3 maestro test android/.maestro/start-workout.yaml
```

#### Use the test-data.env file
You can also edit `test-data.env` and source it:
```bash
# Edit test-data.env with your preferred values
vim android/.maestro/test-data.env

# Source and run
source android/.maestro/test-data.env
maestro test android/.maestro/
```

## Flow Categories

### Core Flows (tag: `core`)
- `start-workout.yaml` — Start workout, add exercise, log sets
- `complete-workout.yaml` — Full workout to completion with summary
- `verify-data-persistence.yaml` — Cross-screen data verification
- `negative-workout-input.yaml` — Invalid input validation

### Smoke Tests (tag: `smoke`)
- `smoke-test.yaml` — Quick app health check
- `navigation-smoke.yaml` — Tab navigation verification
- `onboarding-flow.yaml` — Onboarding completion

### Regression Tests (tag: `regression`)
- `verify-data-persistence.yaml` — Data persistence across screens
- `profile-settings.yaml` — Settings screen verification
- `full-e2e.yaml` — Complete user journey

### E2E Tests (tag: `e2e`)
- `full-e2e.yaml` — Onboarding → workout → history → progress

### Negative Tests (tag: `negative`)
- `negative-workout-input.yaml` — Zero, negative, extreme values
- `search-no-results.yaml` — Empty search results

## Flow Hooks

All flows use standardized hooks:

- **`onFlowStart`** — Setup before flow runs (ensure clean state)
- **`onFlowComplete`** — Cleanup after flow completes (cancel workouts, return to library)

These hooks ensure flows are idempotent and don't interfere with each other.

## Spanish Locale

The app uses Spanish locale by default:
- "Biblioteca de Ejercicios" (Exercise Library)
- "Entrenamiento Activo" (Active Workout)
- "Historial de Entrenamientos" (Workout History)
- "Kilogramos (kg)" (Kilograms)

When creating new flows or customizing exercise names, ensure they match the app's Spanish content.

## Test IDs

Key test IDs available for reliable element selection:
- `workout_fab` — FAB to start workout
- `weight_input` — Weight input field
- `reps_input` — Reps input field
- `search_bar` — Exercise search bar
- `nav_exercise_library` — Exercise Library tab
- `nav_history` — History tab
- `nav_progress` — Progress tab
- `nav_profile` — Profile tab
- `onboarding_name_input` — Onboarding name input
- `onboarding_start` — Onboarding start button

## Adding New Flows

When creating new flows, follow these conventions:

1. **Use environment variables** for test data:
   ```yaml
   - inputText: "${WEIGHT:=80}"  # Variable with default
   ```

2. **Add appropriate tags**:
   ```yaml
   tags:
     - core
     - regression
   ```

3. **Include hooks** for cleanup:
   ```yaml
   onFlowStart:
     - launchApp
     - assertVisible: "Biblioteca de Ejercicios"
   
   onFlowComplete:
     - tapOn:
         id: "nav_exercise_library"
         optional: true
   ```

4. **Take screenshots** at key moments:
   ```yaml
   - takeScreenshot: descriptive_name
   ```

5. **Use testTag IDs** when available instead of text matching

## Troubleshooting

### Flow fails with "Element not found"
- Verify testTag IDs exist in the app
- Check if element is visible (scroll if needed)
- Verify Spanish text matches exactly

### Exercise not found
- Check that `EXERCISE_NAME` matches app content exactly
- Try using `EXERCISE_SEARCH` for partial matching
- Verify exercise exists in the library

### State contamination between flows
- Verify `onFlowComplete` hooks are present
- Check if previous flow left an active workout
- Try running flow in isolation

## CI/CD Integration

For CI/CD, set environment variables in your pipeline:

```yaml
# GitHub Actions example
- name: Run Maestro Tests
  env:
    WEIGHT: 100
    REPS: 10
    USER_NAME: CIUser
  run: maestro test android/.maestro/
```

## Contributing

When adding or modifying flows:
1. Parametrize all hardcoded test data
2. Add sensible defaults using `${VAR:=default}` syntax
3. Update `test-data.env` with new variables
4. Update this README with new variables in the table above
5. Ensure flows work both with and without custom values
