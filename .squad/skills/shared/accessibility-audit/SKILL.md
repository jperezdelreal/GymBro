---
name: accessibility-audit
confidence: low
description: >
  Mobile accessibility auditing for Android (Compose) and iOS (SwiftUI).
  Covers TalkBack/VoiceOver testing, touch target sizing, content descriptions,
  and gym-specific situational accessibility (sweaty hands, noisy environments,
  one-handed use). Adapted from agency-agents Accessibility Auditor.
  Use when: reviewing UI for accessibility, testing with assistive tech,
  or building new screens. Relevant for Switch (QA) and Trinity (UI).
source: https://github.com/msitarzewski/agency-agents/blob/main/testing/testing-accessibility-auditor.md
---

# Accessibility Audit Skill

## Core Principle
Automated tools catch ~30% of accessibility issues. **Manual testing with assistive tech is mandatory.**
For GymBro specifically: users have sweaty hands, are in noisy gyms, may use one hand, and are focused
on their workout — not the screen. Every interaction must be fast, forgiving, and reachable.

## GymBro-Specific Accessibility Concerns

### Situational Disabilities in the Gym
| Situation | Impact | Design Response |
|-----------|--------|----------------|
| Sweaty hands | Touch targets missed | Minimum 48dp touch targets, generous padding |
| Noisy environment | Can't hear audio cues | Visual + haptic feedback always |
| One-handed use | Can't reach top of screen | Critical actions in bottom half |
| Between sets (rushed) | Low attention span | Minimal cognitive load, 1-2 tap actions |
| Gloves | Reduced touch precision | Extra-large touch targets for set logging |
| Bright gym lights | Screen glare | High contrast, large text |

## Android Compose Accessibility Checklist

### Content Descriptions
```kotlin
// ✅ Good — meaningful description
Icon(
    Icons.Default.Add,
    contentDescription = stringResource(R.string.add_set)
)

// ❌ Bad — no description for interactive element
IconButton(onClick = { }) {
    Icon(Icons.Default.Delete, contentDescription = null) // FAIL
}

// ✅ Decorative only — explicitly null is correct
Icon(
    Icons.Default.FitnessCenter,
    contentDescription = null // Decorative, text label nearby
)
```

### Touch Targets
```kotlin
// Minimum 48dp touch target (WCAG 2.5.8 Target Size)
IconButton(
    onClick = { /* action */ },
    modifier = Modifier.size(48.dp) // Enforced minimum
) { /* icon */ }

// For set logging buttons in gym context — go bigger (56dp+)
Button(
    onClick = onLogSet,
    modifier = Modifier
        .height(56.dp)
        .fillMaxWidth()
) { Text("Log Set") }
```

### Semantics & Roles
```kotlin
// Merge child semantics for TalkBack grouping
Row(
    modifier = Modifier.semantics(mergeDescendants = true) { }
) {
    Text("Bench Press")
    Text("80kg × 8 reps")
    // TalkBack reads: "Bench Press, 80kg × 8 reps"
}

// Custom actions for complex components
Modifier.semantics {
    customActions = listOf(
        CustomAccessibilityAction("Delete set") { deleteSet(); true },
        CustomAccessibilityAction("Edit weight") { editWeight(); true }
    )
}

// State descriptions
Modifier.semantics {
    stateDescription = if (isCompleted) "Completed" else "Pending"
}
```

### Focus Management
```kotlin
// After adding a set, move focus to the new row
val focusRequester = remember { FocusRequester() }
LaunchedEffect(setCount) {
    focusRequester.requestFocus()
}
TextField(
    modifier = Modifier.focusRequester(focusRequester),
    // ...
)
```

## iOS SwiftUI Accessibility Checklist

### Labels & Traits
```swift
// Meaningful labels
Button(action: addSet) {
    Image(systemName: "plus.circle")
}
.accessibilityLabel("Add set")

// Combined elements
HStack {
    Text("Bench Press")
    Text("80kg × 8")
}
.accessibilityElement(children: .combine)

// Custom actions
.accessibilityAction(named: "Delete set") { deleteSet() }
```

## Audit Protocol for New Screens

### Step 1: Automated Scan
- Run Android Accessibility Scanner on the screen
- Check Compose Preview accessibility info
- Verify all interactive elements have content descriptions

### Step 2: TalkBack/VoiceOver Walk-Through
- Enable TalkBack (Android) or VoiceOver (iOS)
- Navigate entire screen by swiping right (next element)
- Complete the primary user flow using only TalkBack
- Verify: logical reading order, all actions reachable, state changes announced

### Step 3: Keyboard/Switch Navigation
- Connect external keyboard
- Tab through all interactive elements
- Verify visible focus indicator on every element
- Verify no focus traps

### Step 4: Visual Checks
- Text contrast ratio ≥ 4.5:1 (body) / ≥ 3:1 (large text)
- Touch targets ≥ 48dp
- Respects system font size (Dynamic Type / sp units)
- Works in dark mode
- No information conveyed by color alone

## Severity Classification
| Severity | Definition | Example |
|----------|-----------|---------|
| 🔴 Critical | Blocks task completion for AT users | Button with no label, focus trap |
| 🟠 Serious | Major barrier, workaround exists | Wrong reading order, missing state |
| 🟡 Moderate | Causes difficulty | Small touch target, low contrast |
| ⚪ Minor | Annoyance | Verbose descriptions, redundant info |

## Report Template
```markdown
## Accessibility Audit — [Screen Name]
**Date:** YYYY-MM-DD | **Platform:** Android/iOS | **AT Tested:** TalkBack/VoiceOver

### Summary
- Issues found: X (Critical: X, Serious: X, Moderate: X, Minor: X)
- Conformance: PASS / PARTIAL / FAIL

### Issues
#### [Issue Title]
- **WCAG:** [Criterion number and name]
- **Severity:** Critical/Serious/Moderate/Minor
- **Impact:** [Who is affected and how]
- **Current:** [What happens now]
- **Fix:** [Code-level fix with example]

### What's Working Well
- [Positive findings to preserve]
```
