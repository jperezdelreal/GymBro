---
name: android-navigation-compose
description: Navigation Compose expert guidance — NavHost setup with bottom navigation, back stack management, type-safe argument passing, savedStateHandle for data passing, testing, and anti-patterns.
domain: navigation
confidence: low
source: earned
---

# Android Navigation Compose

> **Source:** Extracted from GymBro Android codebase patterns

This skill provides practical guidance on using Navigation Compose for Android apps, including bottom navigation setup, argument passing, back stack management, and testing navigation flows.

## Context

Use this skill when:
- Setting up navigation for a Compose app
- Implementing bottom navigation with FAB
- Passing data between screens
- Managing back stack behavior
- Testing navigation flows
- Debugging navigation issues

## Patterns

### 1. NavHost Setup with Bottom Navigation

**Use `NavHost` with a single `NavController`** and conditional bottom bar visibility:

```kotlin
@Composable
fun GymBroNavGraph(userPreferences: UserPreferences) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBottomBar = currentDestination?.route in BottomNavTab.entries.map { it.route }
    
    val hasCompletedOnboarding by userPreferences.hasCompletedOnboarding
        .collectAsStateWithLifecycle(initialValue = false)
    val startDestination = if (hasCompletedOnboarding) "exercise_library" else "onboarding"
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                GymBroBottomNavBar(
                    currentRoute = currentDestination?.route,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("exercise_library") { ExerciseLibraryRoute() }
            composable("history") { HistoryListRoute() }
            // ...
        }
    }
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:94`

**Key points:**
- Single `NavController` created with `rememberNavController()`
- `currentBackStackEntryAsState()` to observe current destination
- Conditional bottom bar based on route
- Dynamic start destination based on app state

### 2. Bottom Navigation Best Practices

**Use `popUpTo` + `saveState` + `restoreState`** for proper bottom nav behavior:

```kotlin
navController.navigate(tab.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:115`

**What this does:**
- `popUpTo(startDestination)` — clears intermediate screens
- `saveState = true` — saves state when leaving tab
- `launchSingleTop = true` — reuses existing screen if already on that tab
- `restoreState = true` — restores saved state when returning to tab

**Result:** Users can switch tabs without losing scroll position, form data, etc.

### 3. Argument Passing with NavType

**Define routes with arguments** using `{argumentName}` syntax:

```kotlin
composable(
    route = "workout_summary/{duration}/{volume}/{sets}/{exercises}",
    arguments = listOf(
        navArgument("duration") { type = NavType.LongType },
        navArgument("volume") { type = NavType.FloatType },
        navArgument("sets") { type = NavType.IntType },
        navArgument("exercises") { type = NavType.IntType },
    )
) { backStackEntry ->
    val duration = backStackEntry.arguments?.getLong("duration") ?: 0L
    val volume = backStackEntry.arguments?.getFloat("volume")?.toDouble() ?: 0.0
    val sets = backStackEntry.arguments?.getInt("sets") ?: 0
    val exercises = backStackEntry.arguments?.getInt("exercises") ?: 0
    
    WorkoutSummaryScreen(
        durationSeconds = duration,
        totalVolume = volume,
        totalSets = sets,
        exerciseCount = exercises,
        onDone = { navController.navigate("exercise_library") }
    )
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:281`

**Navigate with arguments:**

```kotlin
navController.navigate("workout_summary/$duration/$volume/$sets/$exercises")
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:230`

**Supported NavTypes:** `StringType`, `IntType`, `LongType`, `FloatType`, `BoolType`

### 4. Passing Complex Objects with SavedStateHandle

**For complex objects (not primitive types), use `savedStateHandle`:**

```kotlin
// In source screen (before navigation)
navController.currentBackStackEntry
    ?.savedStateHandle
    ?.set("summary_prs", prs)  // List<PersonalRecord>

navController.navigate("workout_summary/$duration/$volume/$sets/$exercises") {
    popUpTo("active_workout") { inclusive = true }
}

// In destination screen
composable("workout_summary/{duration}/{volume}/{sets}/{exercises}") { backStackEntry ->
    val prs = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<List<PersonalRecord>>("summary_prs") ?: emptyList()
    
    WorkoutSummaryScreen(personalRecords = prs)
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:228,294`

**Why use savedStateHandle:**
- Navigation library doesn't support complex objects as URL arguments
- Avoids JSON serialization/deserialization
- Type-safe (uses generics)
- Survives process death (if object is Parcelable)

**Important:** Data is stored on the `currentBackStackEntry` before navigating, then retrieved from `previousBackStackEntry` in the destination.

### 5. Picker Pattern (Navigate for Result)

**Use savedStateHandle to return data from a picker screen:**

```kotlin
// Navigate to picker
navController.navigate("exercise_picker")

// In picker screen: write result to previous entry
composable("exercise_picker") {
    ExerciseLibraryRoute(
        onExercisePicked = { exercise ->
            navController.previousBackStackEntry?.savedStateHandle?.apply {
                set("picked_exercise_id", exercise.id.toString())
                set("picked_exercise_name", exercise.name)
                set("picked_exercise_muscle", exercise.muscleGroup.name)
            }
            navController.popBackStack()
        }
    )
}

// In calling screen: read result
composable("active_workout") { backStackEntry ->
    val savedStateHandle = backStackEntry.savedStateHandle
    val pickedId = savedStateHandle.get<String>("picked_exercise_id")
    val pickedName = savedStateHandle.get<String>("picked_exercise_name")
    
    val pickedExercise = if (pickedId != null && pickedName != null) {
        savedStateHandle.remove<String>("picked_exercise_id")
        savedStateHandle.remove<String>("picked_exercise_name")
        Exercise(id = UUID.fromString(pickedId), name = pickedName, /* ... */)
    } else null
    
    ActiveWorkoutRoute(pickedExercise = pickedExercise)
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:199,257`

**Pattern:**
1. Caller navigates to picker
2. Picker writes to `previousBackStackEntry.savedStateHandle` and pops
3. Caller reads from its own `savedStateHandle` and recomposes with new data
4. Caller clears savedStateHandle keys to avoid re-triggering

### 6. Back Stack Management

**Clear back stack when navigating to a terminal screen:**

```kotlin
navController.navigate("workout_summary/$duration/$volume/$sets/$exercises") {
    popUpTo("active_workout") { inclusive = true }
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:233`

**What this does:**
- Removes "active_workout" and all screens above it from back stack
- User can't press back to return to active workout (intended — workout is completed)

**Navigate to root and clear everything:**

```kotlin
navController.navigate("exercise_library") {
    popUpTo("exercise_library") { inclusive = true }
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:304`

**Effect:** Clears entire back stack and navigates to fresh "exercise_library" screen.

### 7. Shared Transitions and Animations

**Define enter/exit animations globally for NavHost:**

```kotlin
NavHost(
    navController = navController,
    startDestination = startDestination,
    enterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
    },
    exitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    },
    popEnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
    },
    popExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    }
) { /* ... */ }
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:145`

**Effect:** All forward navigations slide left + fade, all back navigations slide right + fade.

**Per-destination override:**

```kotlin
composable(
    route = "special_screen",
    enterTransition = { fadeIn() },
    exitTransition = { fadeOut() }
) { /* ... */ }
```

### 8. Testing Navigation with TestNavHostController

**Use `TestNavHostController` to verify navigation behavior:**

```kotlin
@Test
fun clickingWorkoutFabNavigatesToActiveWorkout() {
    val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    navController.navigatorProvider.addNavigator(ComposeNavigator())
    
    composeTestRule.setContent {
        NavHost(navController, startDestination = "exercise_library") {
            composable("exercise_library") {
                FloatingActionButton(
                    onClick = { navController.navigate("active_workout") }
                ) { Icon(Icons.Default.Add, null) }
            }
            composable("active_workout") { Text("Active Workout") }
        }
    }
    
    composeTestRule.onNodeWithContentDescription("Start workout").performClick()
    
    assertEquals("active_workout", navController.currentBackStackEntry?.destination?.route)
}
```

**Key APIs:**
- `TestNavHostController` — test-friendly NavController
- `navController.currentBackStackEntry?.destination?.route` — assert current destination
- `navController.navigatorProvider.addNavigator(ComposeNavigator())` — required setup

## Anti-Patterns

### ❌ Don't create NavController in sub-composables

```kotlin
// BAD: NavController created inside child composable
@Composable
fun ExerciseLibraryScreen() {
    val navController = rememberNavController()  // WRONG! Lost on recomposition
    Button(onClick = { navController.navigate("detail") }) { }
}

// GOOD: NavController passed from parent
@Composable
fun GymBroNavGraph() {
    val navController = rememberNavController()
    NavHost(navController, "start") {
        composable("library") {
            ExerciseLibraryRoute(
                onNavigateToDetail = { navController.navigate("detail/$it") }
            )
        }
    }
}
```

**Why:** NavController should be created once at the NavHost level and navigation should be triggered via lambdas.

### ❌ Don't navigate in composition (outside of LaunchedEffect)

```kotlin
// BAD: Navigation called during composition
@Composable
fun MyScreen(shouldNavigate: Boolean, navController: NavController) {
    if (shouldNavigate) {
        navController.navigate("next")  // WRONG! Called during composition
    }
}

// GOOD: Navigate in LaunchedEffect
@Composable
fun MyScreen(shouldNavigate: Boolean, navController: NavController) {
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            navController.navigate("next")
        }
    }
}
```

**Why:** Navigation is a side effect and should only happen in effect handlers.

### ❌ Don't pass NavController to ViewModels

```kotlin
// BAD: ViewModel knows about navigation
class MyViewModel(private val navController: NavController) : ViewModel() {
    fun onButtonClick() {
        navController.navigate("next")  // WRONG! Breaks separation of concerns
    }
}

// GOOD: ViewModel emits navigation events, screen handles them
class MyViewModel : ViewModel() {
    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()
    
    fun onButtonClick() {
        viewModelScope.launch {
            _effect.send(Effect.NavigateToNext)
        }
    }
}

@Composable
fun MyRoute(viewModel: MyViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                Effect.NavigateToNext -> navController.navigate("next")
            }
        }
    }
}
```

**Source pattern:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutViewModel.kt:34` (effect channel)

### ❌ Don't forget to clear back stack for terminal screens

```kotlin
// BAD: User can press back and return to workout after completing it
navController.navigate("workout_summary/$duration/$volume")

// GOOD: Clear active workout from stack
navController.navigate("workout_summary/$duration/$volume") {
    popUpTo("active_workout") { inclusive = true }
}
```

### ❌ Don't rely on URL arguments for complex objects

```kotlin
// BAD: Trying to serialize complex object
navController.navigate("detail/${Json.encodeToString(exercise)}")  // URL too long, error-prone

// GOOD: Use savedStateHandle
navController.currentBackStackEntry?.savedStateHandle?.set("exercise", exercise)
navController.navigate("detail/${exercise.id}")
```

### ❌ Don't forget to remove savedStateHandle data after reading

```kotlin
// BAD: pickedExercise triggers re-composition on every back stack change
val pickedId = savedStateHandle.get<String>("picked_exercise_id")

// GOOD: Remove after reading to avoid re-triggering
val pickedId = savedStateHandle.get<String>("picked_exercise_id")
if (pickedId != null) {
    savedStateHandle.remove<String>("picked_exercise_id")
}
```

**Source:** `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt:208`

## Summary

- Create NavController once at NavHost level with `rememberNavController()`
- Use `currentBackStackEntryAsState()` to observe current destination
- Bottom nav: use `popUpTo(startDestination)` + `saveState` + `restoreState`
- Pass primitives via route arguments, complex objects via `savedStateHandle`
- Picker pattern: write to `previousBackStackEntry.savedStateHandle`, pop, read in caller
- Clear back stack with `popUpTo { inclusive = true }` for terminal screens
- Define global transitions in NavHost for consistent animations
- Test with `TestNavHostController` and assert `currentBackStackEntry?.destination?.route`
- Never create NavController in child composables or pass it to ViewModels
- Never navigate during composition — use `LaunchedEffect` instead
