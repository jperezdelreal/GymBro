---
name: compose-performance
description: Optimize Jetpack Compose performance — remember vs rememberSaveable, derivedStateOf, LaunchedEffect dependencies, Modifier stability, LazyColumn optimization, and recomposition anti-patterns.
domain: performance
confidence: low
source: earned
---

# Compose Performance Optimization

> **Source:** Extracted from GymBro Android codebase patterns

This skill provides practical guidance on writing performant Compose code that minimizes unnecessary recompositions, avoids expensive operations in the composition phase, and optimizes list rendering.

## Context

Use this skill when:
- Composables recompose too frequently
- Scrolling in LazyColumn is janky
- Animations are dropping frames
- You need to optimize state management
- Reviewing Compose code for performance issues

## Patterns

### 1. remember vs rememberSaveable

**Use `remember`** for transient UI state that doesn't need to survive configuration changes:

```kotlin
@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }  // Resets on rotation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        label = "card_scale"
    )
    
    Card(
        modifier = Modifier.scale(scale)
            .clickable { isPressed = true; onClick() }
    ) { /* ... */ }
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/exerciselibrary/ExerciseLibraryScreen.kt:328`

**Use `rememberSaveable`** for state that should survive configuration changes (process death):

```kotlin
@Composable
fun ActiveWorkoutScreen() {
    var showCompleteSetTooltip by rememberSaveable { mutableStateOf(false) }
    // Survives rotation, app backgrounding, etc.
}
```

**Rule of thumb:** 
- Animation state, focus state, pressed state → `remember`
- User input, form data, expanded state → `rememberSaveable`

### 2. derivedStateOf for Computed Values

**Use `derivedStateOf`** when you derive a value from other state and want to minimize recompositions:

```kotlin
@Composable
fun WorkoutSummaryScreen(exercises: List<Exercise>) {
    // BAD: Recomputes on every recomposition
    val totalVolume = exercises.sumOf { it.volume }
    
    // GOOD: Only recomputes when exercises changes
    val totalVolume by remember(exercises) {
        derivedStateOf { exercises.sumOf { it.volume } }
    }
}
```

**GymBro pattern:** The codebase doesn't currently use `derivedStateOf` extensively (most computation is in ViewModels), but it would benefit screens like `ProgressScreen` that derive analytics from workout data.

**When to use:**
- Filtering/sorting large lists
- Computing aggregates (sum, average, max)
- Deriving boolean flags from complex state

### 3. LaunchedEffect Key Dependencies

**Always specify the correct keys** for `LaunchedEffect` — it restarts when any key changes:

```kotlin
@Composable
fun ActiveWorkoutRoute(
    pickedExercise: Exercise?,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    // Restarts when pickedExercise changes (correct)
    LaunchedEffect(pickedExercise) {
        if (pickedExercise != null) {
            viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(pickedExercise))
        }
    }
    
    // Runs once (Unit key means "never restart")
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ActiveWorkoutEffect.ShowExercisePicker -> onNavigateToExercisePicker()
            }
        }
    }
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt:145,151`

**Common mistake:**
```kotlin
// BAD: Restarts on EVERY recomposition (state is new object every time)
LaunchedEffect(state) { doSomething(state) }

// GOOD: Restart only when specific field changes
LaunchedEffect(state.workoutId) { doSomething(state) }
```

### 4. Modifier Stability and Allocation

**Hoist Modifiers outside composables** when they don't depend on composition state:

```kotlin
// BAD: Allocates new Modifier on every recomposition
@Composable
fun MyCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) { /* ... */ }
}

// GOOD: Reuses stable Modifier
private val CardModifier = Modifier.fillMaxWidth().padding(16.dp)

@Composable
fun MyCard() {
    Card(modifier = CardModifier) { /* ... */ }
}
```

**GymBro pattern:** Most modifiers in GymBro are inline (allocated per composition), which is fine for simple UIs. For high-frequency recomposition (e.g., inside `LazyColumn` items), consider hoisting.

**Modifier.then() for conditional modifiers:**

```kotlin
@Composable
fun FilterChip(selected: Boolean) {
    Card(
        modifier = Modifier.then(
            if (selected) {
                Modifier.background(Brush.horizontalGradient(listOf(AccentGreenStart, AccentGreenEnd)))
            } else {
                Modifier
            }
        )
    ) { /* ... */ }
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/exerciselibrary/ExerciseLibraryScreen.kt:288`

### 5. LazyColumn Optimization

**Always provide `key` for LazyColumn items** to enable efficient recomposition and animation:

```kotlin
@Composable
fun WorkoutHistoryList(workouts: List<Workout>) {
    LazyColumn {
        items(
            items = workouts,
            key = { workout -> workout.id }  // Stable key
        ) { workout ->
            WorkoutCard(workout)
        }
    }
}
```

**Source pattern:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt:275` uses `itemsIndexed` without keys (works but suboptimal).

**Better approach with keys:**

```kotlin
LazyColumn {
    itemsIndexed(
        items = state.value.exercises,
        key = { _, exercise -> exercise.exercise.id }
    ) { index, exerciseUi ->
        ExerciseWorkoutCard(exerciseUi, index, onEvent)
    }
}
```

**contentType for heterogeneous lists:**

```kotlin
LazyColumn {
    items(
        items = feedItems,
        key = { it.id },
        contentType = { item ->
            when (item) {
                is WorkoutItem -> "workout"
                is AdItem -> "ad"
                is HeaderItem -> "header"
            }
        }
    ) { item -> /* ... */ }
}
```

**Why:** Compose can reuse composition slots for items of the same `contentType`, reducing recomposition cost.

### 6. Avoid Heavy Work in Composable Bodies

**Move expensive computation to ViewModels** — composable bodies should be fast and side-effect-free:

```kotlin
// BAD: Expensive computation in composable body (runs on every recomposition)
@Composable
fun ProgressScreen(workouts: List<Workout>) {
    val totalVolume = workouts
        .flatMap { it.sets }
        .filter { !it.isWarmup }
        .sumOf { it.weightKg * it.reps }  // Recalculates on every recomposition!
    
    Text("Total volume: $totalVolume kg")
}

// GOOD: Compute in ViewModel
@HiltViewModel
class ProgressViewModel @Inject constructor() : ViewModel() {
    val state = workoutRepository.getRecentWorkouts().map { workouts ->
        ProgressState(
            totalVolume = workouts.flatMap { it.sets }
                .filter { !it.isWarmup }
                .sumOf { it.weightKg * it.reps }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressState())
}
```

**GymBro pattern:** ViewModels compute totals and emit them as state fields (e.g., `ActiveWorkoutState.totalVolume`, `totalSets`). Screens just display the values.

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutViewModel.kt:216`

### 7. Avoid Unstable Lambdas in Composables

**Hoist lambdas to stable references** when possible:

```kotlin
// BAD: New lambda instance on every recomposition
@Composable
fun MyScreen(items: List<Item>) {
    LazyColumn {
        items(items) { item ->
            ItemCard(
                onClick = { viewModel.onItemClick(item.id) }  // New lambda each time!
            )
        }
    }
}

// GOOD: Stable lambda reference
@Composable
fun MyScreen(items: List<Item>, onItemClick: (String) -> Unit) {
    LazyColumn {
        items(items) { item ->
            ItemCard(
                onClick = { onItemClick(item.id) }  // Stable lambda from parent
            )
        }
    }
}
```

**GymBro pattern:** Event handlers are passed as `onEvent: (Event) -> Unit` from Route to Screen, ensuring stability.

```kotlin
@Composable
fun ActiveWorkoutRoute(viewModel: ActiveWorkoutViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    
    ActiveWorkoutScreen(
        state = state.value,
        onEvent = viewModel::onEvent,  // Stable reference
    )
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt:119`

### 8. remember for Expensive Object Creation

**Use `remember` to cache expensive object creation:**

```kotlin
@Composable
fun MyScreen() {
    // BAD: Creates new Brush on every recomposition
    val gradient = Brush.horizontalGradient(listOf(Color.Red, Color.Blue))
    
    // GOOD: Creates once and reuses
    val gradient = remember {
        Brush.horizontalGradient(listOf(Color.Red, Color.Blue))
    }
}
```

**GymBro pattern:** Gradients and color transformations are often inlined (acceptable for simple cases), but `remember` would help in high-frequency recomposition scenarios.

### 9. AnimatedVisibility for Enter/Exit Animations

**Use `AnimatedVisibility`** instead of manual alpha/scale animations for better performance:

```kotlin
@Composable
fun RestTimerOverlay(isVisible: Boolean) {
    // GOOD: Compose optimizes enter/exit transitions
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        RestTimerCard()
    }
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt` uses manual show/hide logic — could be refactored to `AnimatedVisibility`.

## Anti-Patterns

### ❌ Don't call .reduce() or .map() in composable bodies

```kotlin
// BAD: Recalculates on every recomposition
@Composable
fun SummaryScreen(workouts: List<Workflow>) {
    val exerciseCount = workouts.map { it.exercises.size }.sum()  // Allocates new list!
    Text("Exercises: $exerciseCount")
}

// GOOD: Compute in ViewModel or use derivedStateOf
val exerciseCount by remember(workouts) {
    derivedStateOf { workouts.sumOf { it.exercises.size } }
}
```

### ❌ Don't use mutableStateOf without remember

```kotlin
// BAD: Resets on every recomposition
@Composable
fun MyScreen() {
    var count = mutableStateOf(0)  // Lost on recomposition!
}

// GOOD: Wrap with remember
@Composable
fun MyScreen() {
    var count by remember { mutableStateOf(0) }
}
```

### ❌ Don't mutate state outside snapshot system

```kotlin
// BAD: Bypasses Compose's snapshot system
val state = MyState()
state.count = 42  // Won't trigger recomposition!

// GOOD: Use mutableStateOf
var state by remember { mutableStateOf(MyState()) }
state = state.copy(count = 42)  // Triggers recomposition
```

### ❌ Don't use LaunchedEffect without keys (unless intentional)

```kotlin
// BAD: Runs on EVERY recomposition (if you don't control parent composition)
LaunchedEffect(true) { loadData() }

// GOOD: Run once
LaunchedEffect(Unit) { loadData() }

// GOOD: Run when ID changes
LaunchedEffect(itemId) { loadData(itemId) }
```

### ❌ Don't forget contentType for heterogeneous LazyColumn

```kotlin
// BAD: Compose can't optimize composition slot reuse
LazyColumn {
    items(feedItems, key = { it.id }) { item ->
        when (item) {
            is WorkoutItem -> WorkoutCard(item)
            is AdItem -> AdCard(item)
        }
    }
}

// GOOD: Enables composition slot reuse
LazyColumn {
    items(
        items = feedItems,
        key = { it.id },
        contentType = { if (it is WorkoutItem) "workout" else "ad" }
    ) { /* ... */ }
}
```

### ❌ Don't create new Modifier instances unnecessarily

```kotlin
// BAD: New Modifier chain on every recomposition
@Composable
fun MyCard() {
    val padding = 16.dp
    Card(modifier = Modifier.fillMaxWidth().padding(padding)) { }
}

// GOOD: Stable Modifier reference
private val MyCardModifier = Modifier.fillMaxWidth().padding(16.dp)

@Composable
fun MyCard() {
    Card(modifier = MyCardModifier) { }
}
```

## Summary

- Use `remember` for transient state, `rememberSaveable` for persistent state
- Use `derivedStateOf` to minimize recompositions from computed values
- Specify correct `LaunchedEffect` keys — restart only when dependencies change
- Provide `key` for `LazyColumn` items and `contentType` for heterogeneous lists
- Move expensive computation to ViewModels, not composable bodies
- Hoist stable lambdas and Modifiers to avoid allocations
- Use `AnimatedVisibility` for performant enter/exit animations
- Avoid `.map()`, `.filter()`, `.reduce()` in composable bodies — use `derivedStateOf` or ViewModel
