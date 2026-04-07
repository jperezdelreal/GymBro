# Performance Optimization Reference

> **Source:** [aldefy/compose-skill](https://github.com/aldefy/compose-skill) — MIT License

## Three Phases: Composition, Layout, Drawing

Every frame consists of three phases. Understanding state reads in each phase prevents unnecessary recompositions.

### Composition Phase
- Executes composable functions, evaluates state reads
- **State reads here trigger recomposition** of the entire scope

### Layout Phase
- Calculates size and position, runs `measure` and `layout` blocks
- Can read state without triggering composition recomposition
- Prefer `Modifier.offset { }` over `Modifier.offset()`

### Drawing Phase
- Emits draw operations, runs `Canvas` and custom `DrawScope`

**Source**: `androidx/compose/runtime/Composer.kt`

---

## Recomposition Skipping with Compiler Reports

Enable compiler reports to inspect stability and skippability:

```kotlin
// build.gradle.kts
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}
```

After building, check `build/compose_reports/`:
- **`*_composables.txt`** — shows each composable's restartability and skippability
- **`*_classes.txt`** — shows stability inference for each class

### Stability — @Stable and @Immutable

Mark stable types explicitly:

```kotlin
@Immutable
data class Person(val name: String, val age: Int)

@Stable
class UserViewModel : ViewModel {
    private val _state = MutableState(UserState())
    val state: State<UserState> = _state
}
```

---

## Strong Skipping Mode (Default)

AGP 8.0+ and Compose compiler 1.5.0+ enable **strong skipping mode**:
- Lambdas become stable if all captured variables are stable
- Fewer unnecessary recompositions

---

## Defer State Reads to Layout/Draw Phase

```kotlin
// ❌ Bad: Recomposition on Every Offset Change
@Composable
fun Box(offsetX: State<Float>) {
    val x = offsetX.value  // Reads in composition
    Box(modifier = Modifier.offset(x.dp, 0.dp))
}

// ✅ Good: Deferred Read in Layout Phase
@Composable
fun Box(offsetX: State<Float>) {
    Box(modifier = Modifier.offset { IntOffset(offsetX.value.toInt(), 0) })
}
```

---

## derivedStateOf — Reducing Recomposition Frequency

```kotlin
// ✅ Good: only recomposes if filtered result actually changes
@Composable
fun SearchResults(items: List<Item>, query: String) {
    val filtered = remember(items, query) {
        derivedStateOf { items.filter { query in it.title } }
    }
    LazyColumn { items(filtered.value) { /* ... */ } }
}
```

---

## LazyList Performance — Keys and ContentType

### Always Provide Keys

```kotlin
LazyColumn {
    items(users, key = { it.id }) { user -> UserRow(user) }
}
```

### ContentType for Efficient Reuse

```kotlin
LazyColumn {
    items(items, key = { it.hashCode() }, contentType = { item ->
        when (item) {
            is ListItem.Header -> "header"
            is ListItem.User -> "user"
        }
    }) { item -> /* ... */ }
}
```

---

## Baseline Profiles

Use Jetpack Macrobenchmark to record profiles — reduces startup time and jank:

```kotlin
@RunWith(AndroidBenchmarkRunner::class)
class StartupBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCompilation() = benchmarkRule.measureRepeated(
        packageName = "com.example.app",
        metrics = listOf(StartupTimings.FIRST_FRAME),
        iterations = 10,
        setupBlock = { pressHome(); startActivityAndWait() }
    ) { /* Interact with app */ }
}
```

---

## R8/ProGuard Compose Rules

```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

Custom rules to preserve stability:
```proguard
-keep @androidx.compose.runtime.Stable class **
-keep @androidx.compose.runtime.Immutable class **
```

---

## Zero-Size DrawScope Guard

```kotlin
// ✅ GOOD: always guard
Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
    if (size.minDimension <= 0f) return@Canvas
    val radius = size.minDimension / 2
    drawCircle(color = Color.Blue, radius = radius)
}
```

Rule: Never use `fillMaxSize()` on Canvas without an explicit height constraint.

---

## Composition Tracing

```kotlin
@Composable
fun ExpensiveScreen() {
    trace("ExpensiveScreen") { /* composable body — visible in system traces */ }
}
```

---

## movableContentOf

Avoid recomposition when moving content between layout positions:

```kotlin
val movableContent = remember {
    movableContentOf { ExpensiveChild() }
}
if (isExpanded) ExpandedLayout { movableContent() }
else CollapsedLayout { movableContent() }
```

---

## Production Performance Rules

1. **R8: strip previews + semantics in release**
2. **`@Suppress("ComposeUnstableCollections")`** — pragmatic skipping when stability isn't worth complexity
3. **ImmutableList for stability** — `kotlinx.collections.immutable`
4. **ReportDrawnWhen** for startup performance
5. **Canvas always explicitly sized**

---

## Compose Multiplatform Performance

| Tool | Android | Desktop | iOS | Web |
|------|---------|---------|-----|-----|
| Baseline Profiles | Yes | No | No | No |
| Macrobenchmark | Yes | No | No | No |
| Layout Inspector | Yes (AS) | No | No | No |
| R8/ProGuard | Yes | ProGuard separately | N/A | N/A |

iOS-specific:
- Enable ProMotion with `CADisableMinimumFrameDurationOnPhone = true` in Info.plist
- Kotlin/Native has different GC behavior than Android ART
