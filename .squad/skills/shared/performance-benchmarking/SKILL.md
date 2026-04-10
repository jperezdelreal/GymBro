---
name: performance-benchmarking
confidence: low
description: >
  Mobile app performance benchmarking methodology. Covers cold start measurement,
  UI rendering profiling, memory/battery analysis, and Android-specific tooling.
  Adapted from agency-agents Performance Benchmarker for mobile-first context.
  Use when: startup audit, scroll jank, memory leaks, battery drain, or any
  performance-related issue. Relevant for Switch (testing) and Trinity (UI perf).
source: https://github.com/msitarzewski/agency-agents/blob/main/testing/testing-performance-benchmarker.md
---

# Performance Benchmarking Skill

## Core Principle
Always establish a **baseline before optimizing**. Measure → Identify → Fix → Verify.
Never optimize based on gut feeling — use profiling data.

## Android Cold Start Measurement

### Baseline with ADB
```bash
# Measure cold start time (fully-qualified activity)
adb shell am start-activity -W -S com.gymbro.app/.MainActivity
# Look for: TotalTime, WaitTime, ThisTime in output

# Trace startup for detailed analysis
adb shell am start -W -S --start-profiler /data/local/tmp/startup.trace com.gymbro.app/.MainActivity
```

### Key Metrics
| Metric | Target | Tool |
|--------|--------|------|
| Cold start | < 1.5s | `adb shell am start-activity -W -S` |
| Warm start | < 500ms | Same command without -S |
| First frame | < 700ms | Macrobenchmark `StartupTimingMetric` |
| TTID (Time to Initial Display) | < 1s | Logcat `Displayed` line |
| TTFD (Time to Full Display) | < 2s | `reportFullyDrawn()` in Activity |

### Jetpack Macrobenchmark Setup
```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.gymbro.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

## UI Rendering Performance

### Compose-Specific Profiling
```kotlin
// Add to debug builds for recomposition tracking
@Composable
fun DebugRecomposition(tag: String) {
    val ref = remember { Ref(0) }
    SideEffect { ref.value++ }
    Log.d("Recomposition", "$tag: ${ref.value}")
}
```

### Jank Detection
- Target: **0 janky frames** during scroll in LazyColumn
- Use Android Studio Profiler → Frames chart
- Watch for frames > 16ms (60fps) or > 8ms (120fps)
- Common causes: allocations in composition, unstable keys, missing `key` in LazyColumn items

## Memory Analysis

### Baseline Memory Profile
```bash
# Capture heap dump
adb shell am dumpheap com.gymbro.app /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof

# Monitor memory in real-time
adb shell dumpsys meminfo com.gymbro.app
```

### Targets
| Metric | Threshold | Alert |
|--------|-----------|-------|
| PSS (Proportional Set Size) | < 150MB active | > 200MB = investigate |
| Native heap growth | Stable after warmup | Continuous growth = leak |
| Java heap | < 80% of max | > 90% = GC pressure |

## Battery Impact
- Profile with Battery Historian or Android Studio Energy Profiler
- Watch for: wakelocks, excessive network, GPS polling, sensor abuse
- Gym context: sensors (accelerometer for rep counting) should batch, not stream

## Performance Report Template
```markdown
## Performance Report — [Feature/Screen]
**Date:** YYYY-MM-DD | **Device:** [Model, API level] | **Build:** [variant]

### Startup
- Cold start: Xms (target: <1500ms) ✅/❌
- Warm start: Xms (target: <500ms) ✅/❌

### Rendering
- Janky frames during scroll: X/Y (target: 0) ✅/❌
- Worst frame time: Xms

### Memory
- Baseline PSS: XMB
- After 5min usage: XMB (delta: +XMB)
- Leak detected: yes/no

### Recommendations
1. [High priority fix]
2. [Medium priority fix]
3. [Low priority / future]
```

## Workflow
1. **Baseline** — Measure current state on target device
2. **Profile** — Use Android Studio Profiler or Macrobenchmark
3. **Identify** — Find the bottleneck (startup, rendering, memory, battery)
4. **Fix** — Apply targeted optimization
5. **Verify** — Re-measure and compare with baseline
6. **Guard** — Add benchmark test to CI to prevent regression
