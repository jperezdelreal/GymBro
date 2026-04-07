# Jetpack Compose State Management Reference

> **Source:** [aldefy/compose-skill](https://github.com/aldefy/compose-skill) — MIT License

## State Fundamentals

State in Compose is observable data that triggers recomposition when changed.

### Creating State

Use type-specific state holders for efficiency:

```kotlin
// General-purpose state (Any type)
val name = mutableStateOf("Alice")

// Primitive specializations (avoid boxing)
val count = mutableIntStateOf(0)
val progress = mutableFloatStateOf(0.5f)
val enabled = mutableStateOf(true)  // Boolean has no specialization
```

**Pitfall:** Using `mutableStateOf<Int>()` instead of `mutableIntStateOf()` causes unnecessary boxing on every read/write. Primitive specializations are located in `androidx.compose.runtime` (source: `State.kt`).

## remember vs rememberSaveable

Both associate state with a composition key, but differ in persistence scope.

### remember
- Lives for the composition's lifetime
- Lost on process death, configuration changes, back navigation
- Best for UI state: selection, expanded/collapsed, scroll position

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

### rememberSaveable
- Survives process death and configuration changes
- Uses `Bundle`-compatible types by default (String, Int, Boolean, etc.)
- For custom types, provide a `Saver` or use `@Parcelize`
- Best for data that represents user input or navigation state

```kotlin
@Composable
fun SearchScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    // survives configuration change
}

// Custom type requires explicit Saver
data class User(val id: Int, val name: String)
val userSaver = Saver<User, String>(
    save = { "${it.id}:${it.name}" },
    restore = { parts -> User(parts.split(":")[0].toInt(), parts.split(":")[1]) }
)
var user by rememberSaveable(stateSaver = userSaver) { mutableStateOf(User(1, "Alice")) }
```

## State Hoisting

Move state up to a parent composable to enable reusability and testing.

### Stateful vs Stateless Pattern

```kotlin
// ❌ Stateful version (tightly coupled)
@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) { Text(count.toString()) }
}

// ✅ Stateless version (reusable, testable)
@Composable
fun Counter(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Button(onClick = { onCountChange(count + 1) }) { Text(count.toString()) }
}
```

**Rule:** Push state as high as needed, but no higher.

## derivedStateOf

Computes a value from existing state, recomputing only when dependencies change.

```kotlin
// ✅ Correct: recomputes only when count changes
val isEven = derivedStateOf { count % 2 == 0 }
```

**When to use:**
- Expensive computations from state (e.g., filtering, sorting lists)
- Combining multiple state values
- Creating intermediate state for conditional logic

**Pitfall:** Using `derivedStateOf` for cheap operations adds overhead. Only use when the computation is non-trivial.

## snapshotFlow

Converts Compose state to Kotlin Flow for side effects and external APIs.

```kotlin
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(500)
            .distinctUntilChanged()
            .collect { viewModel.search(it) }
    }
}
```

## SnapshotStateList and SnapshotStateMap

Observable collections that trigger recomposition on structural changes.

```kotlin
val items = remember { mutableStateListOf<Item>() }
// ✅ Triggers recomposition (list structure changed)
items[0] = Item(1, "Updated")
// ❌ Does NOT trigger recomposition (object mutated in-place)
items[0].name = "Updated"
// ✅ Correct: use copy()
items[0] = items[0].copy(name = "Updated")
```

## @Stable and @Immutable Annotations

These annotations help the compiler optimize recomposition (strong skipping mode).

### @Immutable
- All public fields are read-only primitives or other `@Immutable` types
- Instances never change after construction

### @Stable
- Implements structural equality (`equals`)
- Public properties are read-only or observable
- Weaker guarantee than `@Immutable`

## State in ViewModels: StateFlow vs Compose State

### StateFlow (Recommended for ViewModel)
```kotlin
class UserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}

@Composable
fun UserScreen(viewModel: UserViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

### Compose State (For UI-only state)
- Use for temporary, UI-local state
- Don't hoist to ViewModel

## Sealed UiState Pattern

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

## Production State Rules

1. **mutableStateOf ONLY in composables, never in ViewModels** — use `StateFlow` in ViewModels
2. **SharedFlow for one-shot events, NOT Channel** — `Channel` drops events during lifecycle transitions
3. **rememberSaveable only at NavGraph level** — avoid in list items (Bundle ~1MB limit)
4. **snapshotFlow + distinctUntilChanged() for reactive scroll**
5. **stateIn() with map() for derived flows**

## Compose Multiplatform Notes

- `rememberSaveable`, `Bundle`, and `@Parcelize` are **Android-only**. CMP uses `@Serializable` instead.
- `collectAsStateWithLifecycle()` is Android-specific. CMP uses `collectAsState()` or the multiplatform lifecycle library (2.10.0+).

---

**Source references:** `androidx.compose.runtime.State`, `androidx.compose.runtime.saveable`, `androidx.lifecycle.runtime.compose`
