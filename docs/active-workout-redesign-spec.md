# ActiveWorkoutScreen Redesign Specification

**Issue:** #216  
**Author:** Tank (Backend Dev)  
**Date:** 2025-01-05  
**Status:** Design Specification

## Overview

This document outlines the redesign of `ActiveWorkoutScreen` to implement a hero timer and fast UX based on UX roadmap section 4.2.

## Design Goals

1. **Hero Timer**: Large, prominent circular progress timer during rest periods
2. **Bigger Touch Targets**: 56dp minimum for all weight/reps input fields
3. **GlassmorphicCard UI**: Modern glass-effect cards for exercises
4. **Haptic Feedback**: Tactile response on set completion
5. **Sticky Finish Button**: Animated glow when ≥3 exercises completed
6. **FAB for Adding Exercises**: Gradient floating action button

## Implementation Plan

### 1. Hero Rest Timer

```kotlin
@Composable
private fun HeroRestTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    val progress = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero circular timer with AnimatedProgressCircle
        AnimatedProgressCircle(
            progress = progress,
            size = 200.dp,
            strokeWidth = 16.dp,
            gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${remainingSeconds}s",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = glowAlpha),
                )
                Text(
                    text = "REST",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controls with GradientButton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(onClick = { onAdjust(-15) }) { Text("-15s") }
            GradientButton(text = "Skip", onClick = onSkip)
            OutlinedButton(onClick = { onAdjust(15) }) { Text("+15s") }
        }
    }
}
```

###2. Bigger Set Input Fields

**Before:** 40dp height fields  
**After:** 56dp height fields with bold, centered text

```kotlin
@Composable
private fun BiggerNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier.height(56.dp), // ← 56dp minimum touch target
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        ),
        // ... colors & styling
    )
}
```

### 3. Exercise Cards with GlassmorphicCard

```kotlin
GlassmorphicCard(
    modifier = Modifier.padding(horizontal = 16.dp),
    accentColor = getMuscleGroupColor(exercise.muscleGroup),
) {
    ExerciseCardContent(
        exerciseUi = exerciseUi,
        isCollapsed = exerciseUi.sets.all { it.isCompleted },
        // ... other params
    )
}
```

**Muscle Group Colors:**
- Chest → AccentGreenStart
- Back → AccentCyanStart
- Legs → AccentAmberStart
- Shoulders → #FF6B9D
- Arms → #9D4EDD
- Core → #FFBE0B

### 4. Haptic Feedback on Complete Set

```kotlin
val haptic = LocalHapticFeedback.current

Box(
    modifier = Modifier
        .clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onEvent(ActiveWorkoutEvent.CompleteSet(exerciseIndex, setIndex))
        }
) {
    // Complete button UI
}
```

### 5. Sticky Finish Button with Animated Glow

```kotlin
@Composable
private fun FinishWorkoutButton(
    enabled: Boolean,
    completedExercises: Int,
    onClick: () -> Unit,
) {
    val shouldGlow = completedExercises >= 3
    
    val infiniteTransition = rememberInfiniteTransition(label = "finish_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (shouldGlow) 0.3f else 0f,
        targetValue = if (shouldGlow) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finish_glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (shouldGlow) {
                    val gradient = Brush.radialGradient(
                        colors = listOf(
                            AccentGreenStart.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                    drawCircle(gradient, radius = size.width)
                }
            }
    ) {
        GradientButton(
            text = "Finish Workout",
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

### 6. Gradient FAB

```kotlin
Scaffold(
    floatingActionButton = {
        FloatingActionButton(
            onClick = { onEvent(ActiveWorkoutEvent.AddExerciseClicked) },
            containerColor = Color.Transparent,
            modifier = Modifier.drawBehind {
                val gradient = Brush.horizontalGradient(
                    colors = listOf(AccentGreenStart, AccentGreenEnd)
                )
                drawCircle(gradient)
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Exercise", tint = Color.White)
        }
    }
) { ... }
```

## Color Palette

```kotlin
private val AccentGreenStart = Color(0xFF00FF87)
private val AccentGreenEnd = Color(0xFF00D9B5)
private val AccentCyanStart = Color(0xFF00D4FF)
private val AccentCyanEnd = Color(0xFF0091FF)
private val AccentAmberStart = Color(0xFFFFB800)
private val AccentAmberEnd = Color(0xFFFF8A00)
private val AccentRed = Color(0xFFFF3B30)
private val SurfacePrimary = Color(0xFF141414)
private val Background = Color(0xFF0A0A0A)
```

## Layout Structure

```
ActiveWorkoutScreen
├── TopAppBar (Simple, no finish button in actions)
├── FloatingActionButton (Gradient FAB)
└── LazyColumn
    ├── HeroRestTimer (visible when rest timer active)
    ├── Stats Card (GlassmorphicCard with elapsed time, volume, sets)
    ├── Exercise Cards (GlassmorphicCard with accent color per muscle group)
    │   ├── Expanded view with bigger input fields (56dp)
    │   └── Collapsed view showing "X sets completed ✓"
    └── Sticky Finish Button at bottom (with glow animation when ≥3 exercises)
```

## Dependencies

- `AnimatedProgressCircle.kt` - Circular progress indicator
- `GlassmorphicCard.kt` - Glass-effect card container
- `GradientButton.kt` - Button with gradient background
- `Color.kt` - New gradient color definitions
- `Gradients.kt` - Brush gradient utilities

## Build Issues

**Status:** The project currently has pre-existing build issues with the feature module's dependency on app module theme colors. The common components (`AnimatedProgressCircle`, `GlassmorphicCard`, `GradientButton`) reference `com.gymbro.app.ui.theme.*` colors which are not accessible from the feature module.

**Resolution Required:**
1. Move theme colors to a shared module, OR
2. Pass colors as parameters to common components, OR
3. Duplicate color definitions in feature module (current approach in spec)

## Next Steps

1. Resolve module dependency issues for theme colors
2. Implement hero timer redesign
3. Update set input fields to 56dp with bigger typography
4. Integrate glassmorphic cards
5. Add haptic feedback
6. Implement sticky finish button with glow
7. Test on device for UX validation

## References

- UX Roadmap: `.squad/decisions/inbox/morpheus-ux-roadmap.md` section 4.2
- Original Issue: #216
- Related Files:
  - `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutScreen.kt`
  - `android/feature/src/main/java/com/gymbro/feature/workout/ActiveWorkoutContract.kt`
  - `android/feature/src/main/java/com/gymbro/feature/common/*.kt`
