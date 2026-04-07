# Production Crash Playbook for Jetpack Compose

> **Source:** [aldefy/compose-skill](https://github.com/aldefy/compose-skill) — MIT License

Real-world crash patterns observed in Compose applications at scale. Each section documents the root cause, the failing pattern, the fix, and the rule to prevent recurrence.

---

## 1. remember {} Without Configuration-Derived Key

**Root Cause:** `remember {}` without keys caches the initial computation and never recalculates. When the remembered value derives from configuration state (screen dimensions, font scale, density), a configuration change leaves the cached value stale.

```kotlin
// ❌ BAD: shimmerCount is cached, stale after rotation
val shimmerCount = remember { (screenHeightDp / itemHeightDp).toInt() }

// ✅ GOOD: screenHeightDp is a key
val shimmerCount = remember(screenHeightDp) {
    (screenHeightDp / itemHeightDp).toInt().coerceAtLeast(1)
}
```

**Rule:** Any value derived from `LocalConfiguration`, `LocalDensity`, or `LocalLayoutDirection` MUST include that configuration source in `remember`'s key parameters.

---

## 2. indexOf() Inside items {}

**Root Cause:** `list.indexOf(item)` inside LazyColumn's `items {}` is O(n) per item = O(n²) total. Worse, `indexOf` may return `-1` if objects are recreated, causing `IndexOutOfBoundsException`.

```kotlin
// ❌ BAD: O(n²) and crashes when indexOf returns -1
items(messages) { message ->
    val index = messages.indexOf(message)
}

// ✅ GOOD: Use itemsIndexed
itemsIndexed(items = messages, key = { _, message -> message.id }) { index, message ->
    MessageRow(message = message, isEven = index % 2 == 0)
}
```

**Rule:** Never call `indexOf()` inside a `LazyListScope` item factory. Use `itemsIndexed`.

---

## 3. DrawScope Without Zero-Size Guard

**Root Cause:** During initial composition, a `Canvas` may receive `Size.Zero`. Dividing by zero-dimension values produces `NaN` or `Infinity`, crashing Skia.

```kotlin
// ❌ BAD
Canvas(modifier = Modifier.fillMaxSize()) {
    val radius = size.minDimension / 2  // NaN when size is zero
    drawCircle(color = Color.Blue, radius = radius)
}

// ✅ GOOD
Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
    if (size.minDimension <= 0f) return@Canvas
    val radius = size.minDimension / 2
    drawCircle(color = Color.Blue, radius = radius)
}
```

**Rule:** Always guard `DrawScope` blocks against zero-size. Never use `fillMaxSize()` on `Canvas` without an explicit height constraint.

---

## 4. Duplicate LazyColumn Keys

**Root Cause:** `LazyColumn` requires unique keys. Duplicate IDs (from backend or WebSocket reconnection) cause `IllegalArgumentException: Key ... was already used`.

### Fix: Dedup Index Pattern

```kotlin
fun List<Notification>.withDedupIndex(): List<Notification> {
    val seen = mutableMapOf<String, Int>()
    return map { item ->
        val count = seen.getOrDefault(item.id, 0)
        seen[item.id] = count + 1
        item.also { it.dedupIndex = count }
    }
}

// Key: "${it.id}_${it.dedupIndex}" — guaranteed unique
```

**Rule:** Never trust backend data to provide unique keys. Deduplicate or use the dedup-index pattern.

---

## 5. derivedStateOf Driving Collection Size

**Root Cause:** `derivedStateOf` exposes only a count, but `items(count)` may use a stale count out of sync with the actual collection → `IndexOutOfBoundsException`.

```kotlin
// ❌ BAD: derive only the count
val itemCount by remember { derivedStateOf { allItems.count { ... } } }
items(itemCount) { index -> allItems.filter { ... }[index] }  // IOOB crash

// ✅ GOOD: derive the full filtered list
val filteredItems by remember { derivedStateOf { allItems.filter { ... } } }
items(items = filteredItems, key = { it.id }) { item -> ItemRow(item) }
```

**Rule:** Never use `derivedStateOf` to expose a count that a `LazyList` will use to access a separate collection.

---

## 6. collectAsState vs collectAsStateWithLifecycle

```kotlin
// ❌ BAD: continues collecting in background
val state by viewModel.uiState.collectAsState()

// ✅ GOOD: stops collecting when lifecycle drops below STARTED
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

**Rule:** On Android, always use `collectAsStateWithLifecycle` for StateFlow/SharedFlow.

### CMP expect/actual Pattern
```kotlin
// commonMain
@Composable
expect fun <T> Flow<T>.collectAsStateMultiplatform(initial: T): State<T>

// androidMain
actual fun ... = collectAsStateWithLifecycle(initialValue = initial)

// iosMain / desktopMain
actual fun ... = collectAsState(initial = initial)
```

---

## 7. Multi-Field Keys with Collision Prefixes

**Root Cause:** Same entity ID in multiple sections (pinned, live, archived) → key collision → crash.

```kotlin
// ✅ GOOD: prefix keys with section type
items(pinnedMessages, key = { "pinned_${it.id}" }) { ... }
items(liveMessages, key = { "live_${it.id}" }) { ... }
```

---

## 8. rememberSaveable in List Items → TransactionTooLargeException

```kotlin
// ❌ BAD: rememberSaveable deep in a list item (bloats Bundle)
items(messages) { message ->
    var expanded by rememberSaveable { mutableStateOf(false) }  // Bundle explodes
}

// ✅ GOOD: rememberSaveable at screen level, remember inside items
var searchQuery by rememberSaveable { mutableStateOf("") }
items(messages, key = { it.id }) { message ->
    var expanded by remember { mutableStateOf(false) }
}
```

**Rule:** `rememberSaveable` only at NavGraph/screen level. Bundle has ~1MB limit.

---

## 9. Production State Rules Summary

1. **mutableStateOf ONLY in composables, never in ViewModels** — use `StateFlow`
2. **SharedFlow for events, NOT Channel** — `Channel` drops events during lifecycle transitions
3. **rememberSaveable only at NavGraph level**
4. **snapshotFlow + distinctUntilChanged() for reactive scroll**
5. **stateIn() with map() for derived flows**
