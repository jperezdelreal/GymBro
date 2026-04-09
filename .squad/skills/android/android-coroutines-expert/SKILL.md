---
name: android-coroutines-expert
description: Expert guidance on Kotlin Coroutines for Android — viewModelScope, Dispatchers, Flow collection in Compose, error handling, testing, and common anti-patterns.
domain: concurrency
confidence: low
source: earned
---

# Android Coroutines Expert

> **Source:** Extracted from GymBro Android codebase patterns

This skill provides practical guidance on using Kotlin Coroutines in Android applications, with a focus on ViewModels, Flow collection in Compose, dispatcher selection, error handling, and testing.

## Context

Use this skill when working with:
- Asynchronous operations in ViewModels
- Flow collection in Compose screens
- Background work that needs lifecycle awareness
- Testing coroutines-based code
- Debugging concurrency issues

## Patterns

### 1. viewModelScope for ViewModel Coroutines

**Always use `viewModelScope`** for ViewModel coroutines — it's lifecycle-aware and cancels automatically when the ViewModel is cleared.

```kotlin
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    
    fun onEvent(event: ActiveWorkoutEvent) {
        when (event) {
            is ActiveWorkoutEvent.CompleteSet -> {
                viewModelScope.launch {
                    workoutRepository.addSet(workoutId, exerciseSet)
                    _state.update { it.copy(isCompleted = true) }
                    _effect.send(ActiveWorkoutEffect.ShowSuccess)
                }
            }
        }
    }
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutViewModel.kt:64,159`

### 2. Dispatcher Selection

**Choose the right dispatcher** for the task:

| Dispatcher | Use Case | Example |
|------------|----------|---------|
| `Dispatchers.Main` | UI updates, Compose state | `_state.update { ... }` |
| `Dispatchers.IO` | Network, database, file I/O | Repository calls, Room DAO operations |
| `Dispatchers.Default` | CPU-intensive work | Parsing large JSON, complex calculations |

**GymBro pattern:** Repository layer uses `Dispatchers.IO` implicitly (Room DAO operations). ViewModels use `viewModelScope` which defaults to `Dispatchers.Main.immediate`.

```kotlin
@Singleton
class OfflineSyncManager @Inject constructor(
    private val cloudSyncService: CloudSyncService,
    private val connectivityObserver: ConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private fun observeConnectivity() {
        scope.launch {
            connectivityObserver.isConnected.collect { isConnected ->
                if (isConnected) flushQueue()
            }
        }
    }
}
```

**Source:** `android/core/src/main/java/com/gymbro/core/sync/service/OfflineSyncManager.kt:36,100`

### 3. Flow Collection in Compose with `collectAsStateWithLifecycle`

**Always use `collectAsStateWithLifecycle`** to collect Flows in Compose — it respects the Compose lifecycle and stops collecting when the composable leaves the screen.

```kotlin
@Composable
fun ActiveWorkoutRoute(
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    
    ActiveWorkoutScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
    )
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt:119`

**With initial value (for Flow<T> from DataStore):**

```kotlin
val weightUnit = userPreferences.weightUnit.collectAsStateWithLifecycle(
    initialValue = UserPreferences.WeightUnit.KG
)
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt:142`

### 4. LaunchedEffect for Side Effects

**Use `LaunchedEffect`** for one-time or key-dependent side effects:

```kotlin
@Composable
fun ActiveWorkoutRoute(
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
    pickedExercise: Exercise? = null,
) {
    // Run when pickedExercise changes
    LaunchedEffect(pickedExercise) {
        if (pickedExercise != null) {
            viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(pickedExercise))
        }
    }
    
    // Run once on first composition
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

**Key dependencies:** `LaunchedEffect` restarts when any key changes. Use `Unit` for "run once", use state variables to trigger re-execution.

### 5. Long-Running Jobs in ViewModels

**Store Jobs as private properties** when you need to cancel them manually (e.g., timers, polling):

```kotlin
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor() : ViewModel() {
    
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    
    private fun startElapsedTimer() {
        timerJob?.cancel()  // Cancel previous job
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restTimerJob?.cancel()
    }
}
```

**Source:** `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutViewModel.kt:36,51,309`

**Note:** `viewModelScope` cancels all its children automatically, but explicit cancellation in `onCleared()` is good practice for clarity.

### 6. Error Handling with try-catch

**Wrap repository calls in try-catch** when you need to handle errors gracefully:

```kotlin
fun loadWorkout(workoutId: String) {
    viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        try {
            val workout = workoutRepository.getWorkout(workoutId)
            _state.update { it.copy(workout = workout, isLoading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message, isLoading = false) }
            _effect.send(WorkoutEffect.ShowError(e.message ?: "Unknown error"))
        }
    }
}
```

**GymBro pattern:** Most repository calls in GymBro don't throw exceptions (Room DAOs return `null` or empty lists on failure), so explicit try-catch is rare. Use it when calling network APIs or when you need to log errors.

### 7. Testing with TestDispatcher

**Use `TestDispatcher` and `MainDispatcherRule`** to test coroutines:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class ActiveWorkoutViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)
    
    @Test
    fun completeSetMarksAsCompleted() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.CompleteSet(0, 0))
        advanceUntilIdle()  // Run all pending coroutines
        
        assertTrue(viewModel.state.value.exercises[0].sets[0].isCompleted)
    }
}
```

**Source:** `android/feature/src/test/java/com/gymbro/feature/MainDispatcherRule.kt:13`, `android/feature/src/test/java/com/gymbro/feature/workout/ActiveWorkoutViewModelTest.kt:152`

**Key APIs:**
- `runTest { }` — runs test with TestDispatcher
- `advanceUntilIdle()` — advances time until all coroutines complete
- `advanceTimeBy(millis)` — advances virtual time
- `StandardTestDispatcher` — controlled dispatcher for fine-grained testing
- `UnconfinedTestDispatcher` — executes eagerly (useful for `MainDispatcherRule`)

### 8. Testing Flow Emissions with Turbine

**Use Turbine** to test Flow emissions:

```kotlin
@Test
fun addExerciseClickedEmitsEffect() = runTest(testDispatcher) {
    viewModel.effect.test {
        viewModel.onEvent(ActiveWorkoutEvent.AddExerciseClicked)
        assertEquals(ActiveWorkoutEffect.ShowExercisePicker, awaitItem())
    }
}
```

**Source:** `android/feature/src/test/java/com/gymbro/feature/workout/ActiveWorkoutViewModelTest.kt:177`

**Turbine APIs:**
- `.test { }` — starts collecting the Flow
- `awaitItem()` — waits for and returns next emission
- `awaitComplete()` — waits for Flow completion
- `cancelAndIgnoreRemainingEvents()` — stops collection

## Anti-Patterns

### ❌ Don't block Dispatchers.Main

```kotlin
// BAD: Blocking Main thread
viewModelScope.launch {
    Thread.sleep(1000)  // Freezes UI!
    updateState()
}

// GOOD: Use delay() which is non-blocking
viewModelScope.launch {
    delay(1000)
    updateState()
}
```

### ❌ Don't use GlobalScope in ViewModels

```kotlin
// BAD: GlobalScope outlives the ViewModel
GlobalScope.launch {
    workoutRepository.sync()
}

// GOOD: Use viewModelScope
viewModelScope.launch {
    workoutRepository.sync()
}
```

**Why:** `GlobalScope` coroutines survive ViewModel destruction, causing memory leaks and crashes when accessing dead ViewModel state.

### ❌ Don't use `.collect()` in Compose body

```kotlin
// BAD: Leaks — collect() never stops
@Composable
fun MyScreen(viewModel: MyViewModel) {
    viewModel.state.collect { state ->  // WRONG!
        Text(state.message)
    }
}

// GOOD: Use collectAsStateWithLifecycle
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Text(state.message)
}
```

### ❌ Don't forget to cancel long-running jobs

```kotlin
// BAD: timerJob keeps running after ViewModel is cleared
class MyViewModel : ViewModel() {
    private val timerJob = viewModelScope.launch {
        while (true) { delay(1000); tick() }
    }
}

// GOOD: Store job reference and cancel explicitly
class MyViewModel : ViewModel() {
    private var timerJob: Job? = null
    
    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) { delay(1000); tick() }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
```

### ❌ Don't use `runBlocking` in production code

```kotlin
// BAD: runBlocking blocks the thread
fun saveWorkout() {
    runBlocking {
        repository.save(workout)
    }
}

// GOOD: Launch coroutine properly
fun saveWorkout() {
    viewModelScope.launch {
        repository.save(workout)
    }
}
```

**Exception:** `runBlocking` is acceptable in tests and `main()` functions.

### ❌ Don't ignore CoroutineExceptionHandler needs

```kotlin
// BAD: Uncaught exceptions crash the app
val scope = CoroutineScope(Dispatchers.IO)
scope.launch {
    throw RuntimeException("Boom!")  // App crashes!
}

// GOOD: Use CoroutineExceptionHandler for root coroutines
val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Log.e(TAG, "Coroutine failed", throwable)
}
val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
```

**Note:** In GymBro, ViewModels use `viewModelScope` which already has exception handling. Custom scopes (like `OfflineSyncManager.scope`) should use `SupervisorJob()` + exception handler.

## Summary

- Use `viewModelScope` for ViewModel coroutines (lifecycle-aware)
- Choose `Dispatchers.IO` for I/O, `Dispatchers.Main` for UI updates
- Collect Flows with `collectAsStateWithLifecycle` in Compose
- Use `LaunchedEffect` for side effects with proper key dependencies
- Test with `TestDispatcher`, `runTest`, `advanceUntilIdle()`
- Test Flow emissions with Turbine's `.test { awaitItem() }`
- Avoid blocking Main thread, `GlobalScope`, and `runBlocking` in production
