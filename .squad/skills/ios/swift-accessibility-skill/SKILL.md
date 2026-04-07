---
name: swift-accessibility-skill
description: Apply platform accessibility best practices to SwiftUI, UIKit, and AppKit code. Essential companion to any SwiftUI, UIKit, or AppKit skill ÔÇö always use together. Use whenever writing, editing, or reviewing ANY SwiftUI views, UIKit view controllers, AppKit views/window controllers, or platform UI ÔÇö even when the user doesn't mention accessibility. Also use when the user mentions VoiceOver, Voice Control, Dynamic Type, Reduce Motion, screen reader, a11y, WCAG, accessibility audit, Nutrition Labels, accessibilityLabel, UIAccessibility, NSAccessibility, assistive technologies, or Switch Control. Not for server-side Swift, non-UI packages, or CLI tools.
---

# Platform Accessibility

## Overview

Apply accessibility for SwiftUI, UIKit, and AppKit across all supported platforms. Covers all 9 App Store Accessibility Nutrition Label categories ÔÇö VoiceOver, Voice Control, Larger Text, Dark Interface, Differentiate Without Color, Sufficient Contrast, Reduced Motion, Captions, and Audio Descriptions.

This skill prioritizes native platform APIs (which provide free automatic support) and fact-based guidance without architecture opinions.

## First-Draft Rules

Include accessibility in the **first draft** ÔÇö never write a bare element and patch it later. Retrofitting accessibility is harder, gets skipped, and produces worse results than building it in from the start.

No inline commentary unless a pattern is non-obvious. Mark inferred labels with `// [VERIFY]` because SF Symbol names don't always match the intended user-facing meaning.

| Situation | Required on first write |
|---|---|
| `Button` / `NavigationLink` ÔÇö icon-only | `.accessibilityLabel("ÔÇª")` with `// [VERIFY]` |
| `Button` / `NavigationLink` ÔÇö visible text | Nothing extra ÔÇö text is the label automatically |
| `Image` ÔÇö meaningful | `.accessibilityLabel("ÔÇª")` |
| `Image` ÔÇö decorative | `.accessibilityHidden(true)` |
| `withAnimation` / `.transition` / `.animation` | `@Environment(\.accessibilityReduceMotion)` + gate animation |
| `.font(.system(size:))` | Replace with `.font(.body)` or `@ScaledMetric` |
| Color conveys state/status | Add shape, icon, or text alongside color |
| `onTapGesture` on non-`Button` | `.accessibilityElement(children: .ignore)` + `.accessibilityAddTraits(.isButton)` + `.accessibilityLabel` |
| Custom slider / toggle / stepper | `.accessibilityRepresentation { ÔÇª }` or `.accessibilityValue` + `.accessibilityAdjustableAction` |
| Async content change | Post announcement with availability guards (`AccessibilityNotification.Announcement` on iOS 17+, fallback to `UIAccessibility.post`) |
| System `.sheet` / `.fullScreenCover` | Nothing extra ÔÇö SwiftUI traps focus automatically (custom overlays still need focus management) |
| `AVPlayer` / video | Use `AVPlayerViewController` ÔÇö captions and Audio Descriptions for free |
| Custom tappable view | `.frame(minWidth: 44, minHeight: 44)` |
| Any new SwiftUI view | Verify with Xcode Canvas Variants (see Accessibility Summary) |
| `NSButton` ÔÇö icon-only (AppKit) | `setAccessibilityLabel("ÔÇª")` with `// [VERIFY]` |
| Custom `NSView` interactive element (AppKit) | `setAccessibilityElement(true)` + role (`setAccessibilityRole(.button)`) + label |
| AppKit modal/popup UI | Trap focus and ensure dismiss action is keyboard + VoiceOver reachable |
| Any new AppKit view/controller | Verify with Accessibility Inspector and full keyboard navigation |

Prefer native controls (`Button`, `Toggle`, `Stepper`, `Slider`, `Picker`, `TextField`) ÔÇö they get full accessibility automatically. Custom interactive views require explicit work.
For AppKit, prefer native controls (`NSButton`, `NSPopUpButton`, `NSSlider`, `NSSegmentedControl`, `NSTextField`) before custom `NSView` interaction.

**Example ÔÇö icon-only button:**
```swift
Button {
    shareAction()
} label: {
    Image(systemName: "square.and.arrow.up")
}
.accessibilityLabel("Share") // [VERIFY] confirm label matches intent
```

**Example ÔÇö gating animation on Reduce Motion:**
```swift
@Environment(\.accessibilityReduceMotion) private var reduceMotion

withAnimation(reduceMotion ? nil : .spring()) {
    isExpanded.toggle()
}
```

Full testing and verification procedures ÔåÆ `references/testing-auditing.md`

## Workflow

### Reference Routing Rule
Before answering, select one **primary** reference file that best matches the user's intent and load it first.
Load additional reference files only when the request explicitly spans multiple domains (for example, VoiceOver + Dynamic Type + WCAG mapping) or when the primary file does not cover a required criterion.

### 1) Implement new code
Apply **First-Draft Rules** ÔÇö accessibility in the first draft, no commentary.
For APIs introduced after iOS 15, always add `#available` guards and provide older-OS fallback behavior.
After writing, verify against the First-Draft Rules table ÔÇö fix any gaps before outputting.
After the code, append an **Accessibility Summary** (see below).

### 2) Improve or fix existing code
Apply fixes silently, no commentary.
For APIs introduced after iOS 15, always add `#available` guards and provide older-OS fallback behavior.
After fixing, verify against the First-Draft Rules table ÔÇö fix any gaps before outputting.
After the code, append an **Accessibility Summary**.
- For transformation patterns ÔåÆ `examples/before-after-swiftui.md`, `examples/before-after-uikit.md`, or `examples/before-after-appkit.md`
- For platform issues ÔåÆ `references/platform-specifics.md`

### 3) Audit existing code
Only when user explicitly asks ("audit", "how accessible is this?", "review accessibility").

**Quick fix mode** ÔÇö when the user asks for blocker-only/critical-only scope (for example: "just fix the blockers", "quick fix", "critical only"): address only Blocks Assistive Tech and Degrades Experience issues. Skip Incomplete Support.

**Comprehensive mode** (default) ÔÇö address all severity levels including Incomplete Support and Nutrition Label gaps.

- Identify issues by category ÔåÆ **Triage Playbook** below
- Format with **Audit Output Format** below
- For WCAG compliance mapping ÔåÆ `references/wcag-mapping.md`
- Hand off to QA ÔåÆ `resources/qa-checklist.md`

### 4) Prepare Nutrition Label recommendation
ÔåÆ `references/nutrition-labels.md` ÔÇö all 9 categories with official pass/fail criteria

When the user asks to prepare or draft an App Store Accessibility Nutrition Label recommendation, output this format:

```
**Accessibility Nutrition Label recommendation**

**App version evaluated:** [version or "Current build"]
**Scope reviewed:** [common tasks / screens evaluated]

**You could claim:**
- [labels where every common task is Ô£à or ÔÇö]

**Why you could claim them:**
- [label]: [brief reason tied to completed common-task coverage]

**You should not claim:**
- [labels blocked by any ÔØî]
- [labels that are not applicable]

**Why you should not claim them:**
- [label]: [blocked task or why the label is not applicable]

**Common-task verification**
| Common Task | VoiceOver | Voice Control | Larger Text | Dark Mode | No Color | Contrast | Motion | Captions | Audio Desc |
|---|---|---|---|---|---|---|---|---|---|
| [task] | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö | Ô£à / ÔØî / ÔÇö |

**Recommendation summary**
- You could claim: [labels]
- You should not claim: [labels]
```

Do not say "claim" without qualification. Phrase the output as a recommendation based on the reviewed scope.
Do not suggest a label if any common task in that column is ÔØî. Use `ÔÇö` only when the label is genuinely not applicable to that app or flow.

## Accessibility Summary

Append after all code generation and fix tasks (modes 1, 2), unless the user explicitly requests code-only output. No preamble.

```
**Accessibility applied:**
- [one bullet per pattern added ÔÇö e.g. "`.accessibilityLabel` on icon-only Share button"]

**Verify in Xcode:**
- Use Canvas **Dynamic Type Variants** (grid icon ÔåÆ Dynamic Type Variants) to check layout at all text sizes
- Use Canvas **Color Scheme Variants** to check light and dark mode
- Use **Accessibility Inspector** (Xcode ÔåÆ Open Developer Tool) Settings tab to simulate Increase Contrast, Reduce Motion, Bold Text on the Simulator

**If Xcode is unavailable:**
- Run equivalent checks with platform accessibility inspector tools and manual setting toggles (Dynamic Type, Contrast, Reduce Motion, VoiceOver/Voice Control)

**Test on device:**
- [relevant items from Must Test on Device checklist]
```

Omit "Accessibility applied" entirely if nothing was added (all native controls).
Omit "Nutrition Label readiness" unless the user asked about it.

## Audit Output Format

Only when user explicitly requests an audit. Never during code generation or fixes.

**­ƒö┤ Blocks Assistive Tech** ÔÇö completely unreachable, fix immediately
**­ƒƒí Degrades Experience** ÔÇö reachable but significant friction
**­ƒƒá Incomplete Support** ÔÇö gaps preventing Nutrition Label claims
**Ô£à Verified in code** ÔÇö confirmed correct by static analysis

Close with:
> **Must test on device**: relevant items from the Review Checklist.
> **Nutrition Label readiness**: Achievable / Blocked by [issue] / Not applicable.

## Core Guidelines

### Principles
- **Identify framework and platform first.** SwiftUI and UIKit have different APIs; using the wrong one causes silent failures.
- **Every modifier needs a semantic reason.** Adding `.accessibilityLabel` to a `Button` with visible text actually *hurts* ÔÇö it overrides the text VoiceOver would read automatically.
- **Gate iOS 17+ APIs with `#available`.** Version-specific APIs crash on older OS without availability checks.
- **Mark inferred labels with `[VERIFY]`.** SF Symbol names (e.g. `square.and.arrow.up`) rarely match what users expect to hear ("Share"). Inferred labels need human review.
- **Don't change core UI semantics or layout based on `UIAccessibility.isVoiceOverRunning`.** Adapt to the actual user need by checking the relevant accessibility setting directly. Narrow coordination exceptions are fine, such as avoiding overlapping speech or extending transient timeouts while assistive tech is active.
- **Nutrition Labels require complete flow coverage.** Claiming "VoiceOver supported" means *every* user flow works ÔÇö login, onboarding, purchase, settings ÔÇö not just the main screen.
- **Test contrast in both light and dark mode.** A color pair that passes WCAG 4.5:1 in light mode often fails in dark mode due to different background values.

### VoiceOver
- Every non-decorative element needs a concise, context-independent label
- Icon-only buttons need `.accessibilityLabel` ÔÇö blank is never acceptable
- Decorative images: `.accessibilityHidden(true)`
- State is a trait, not a label: `.accessibilityAddTraits(.isSelected)` not `"Selected photo"`
- Group related elements: `.accessibilityElement(children: .combine)`
- Announce dynamic changes: iOS 17+ `AccessibilityNotification.Announcement("Upload complete").post()`, fallback `UIAccessibility.post(notification: .announcement, argument: "Upload complete")`
- Deep reference ÔåÆ `references/voiceover-swiftui.md` or `references/voiceover-uikit.md`

### Voice Control
- Labels must exactly match visible text ÔÇö mismatches silently break "Tap [name]"
- `.accessibilityInputLabels(["Compose", "New Message"])` for icon-only elements
- Every interactive element must appear in "Show numbers" and "Show names" overlays
- Hidden-on-swipe UI needs a voice-accessible alternative (`.accessibilityAction`)
- Deep reference ÔåÆ `references/voice-control.md`

### Dynamic Type
- Text styles only: `.font(.body)` not `.font(.system(size: 16))`
- Scale custom values: `@ScaledMetric(relativeTo: .body) var spacing: CGFloat = 8`
- Fixed-size UI chrome: `.accessibilityShowsLargeContentViewer()`
- Adaptive layout: prefer `ViewThatFits` (iOS 16+) over manual `dynamicTypeSize` checks ÔÇö it automatically picks the layout that fits
- Deep reference ÔåÆ `references/dynamic-type.md`

### Display Settings
- Reduce Motion: replace meaningful animations with dissolve/fade; remove decorative ones
- Contrast: semantic colors (`Color(.label)`); WCAG 4.5:1 text, 3:1 non-text
- Differentiate Without Color: add shape/icon/text alongside color
- Reduce Transparency: replace `.ultraThinMaterial` with opaque when enabled
- Deep reference ÔåÆ `references/display-settings.md`

### Semantic Structure
- Reading order: `.accessibilitySortPriority(_:)` (higher = read first)
- Focus on new screen: post `.screenChanged` notification
- Modal focus: `accessibilityViewIsModal = true`
- Custom navigation: `accessibilityRotor(_:entries:)`
- Deep reference ÔåÆ `references/semantic-structure.md`

### Motor / Input
- Touch targets: minimum 44├ù44pt
- Keyboard: every element via Tab, every modal via Escape
- Switch Control: `UIAccessibilityCustomAction` for swipe-only gestures
- Deep reference ÔåÆ `references/motor-input.md`

## Quick Reference

### SwiftUI Modifiers

| Modifier | Purpose |
|---|---|
| `.accessibilityLabel(_:)` | VoiceOver text for non-text elements |
| `.accessibilityHint(_:)` | Brief result description |
| `.accessibilityValue(_:)` | Current value (sliders, progress) |
| `.accessibilityHidden(true)` | Hide decorative elements |
| `.accessibilityAddTraits(_:)` | Semantic role or state |
| `.accessibilityRemoveTraits(_:)` | Remove inherited trait |
| `.accessibilityElement(children:)` | `.combine` / `.contain` / `.ignore` |
| `.accessibilitySortPriority(_:)` | Reading order (higher = earlier) |
| `.accessibilityAction(_:_:)` | Named custom action |
| `.accessibilityAdjustableAction(_:)` | Increment/decrement |
| `.accessibilityInputLabels(_:)` | Voice Control alternate names |
| `.accessibilityFocused(_:)` | Programmatic focus |
| `.accessibilityRotor(_:entries:)` | Custom VoiceOver rotor |
| `.accessibilityRepresentation(_:)` | Replace AX tree for custom controls |
| `.accessibilityIgnoresInvertColors(true)` | Protect images in Smart Invert |
| `.accessibilityShowsLargeContentViewer()` | Large Content Viewer for fixed-size UI |

### @Environment Values

| Value | Purpose |
|---|---|
| `\.accessibilityReduceMotion` | Gate animations |
| `\.accessibilityReduceTransparency` | Replace blur effects |
| `\.accessibilityDifferentiateWithoutColor` | Add non-color indicators |
| `\.colorSchemeContrast` | `.standard` / `.increased` |
| `\.dynamicTypeSize` | Current text size |

### Nutrition Labels ÔåÆ APIs

| Label | Key APIs | Reference |
|---|---|---|
| VoiceOver | `accessibilityLabel`, traits, actions, rotors | `voiceover-swiftui.md`, `voiceover-uikit.md` |
| Voice Control | `accessibilityInputLabels`, visible text match | `voice-control.md` |
| Larger Text | `@ScaledMetric`, text styles, Large Content Viewer | `dynamic-type.md` |
| Dark Interface | `colorScheme`, semantic colors | `display-settings.md` |
| Differentiate Without Color | shapes + color | `display-settings.md` |
| Sufficient Contrast | WCAG 4.5:1 text / 3:1 non-text | `display-settings.md` |
| Reduced Motion | `accessibilityReduceMotion`, animation gate | `display-settings.md` |
| Captions | `AVPlayerViewController` | `media-accessibility.md` |
| Audio Descriptions | `AVMediaCharacteristic.describesVideoForAccessibility` | `media-accessibility.md` |

## Review Checklist

### Verifiable in Code
- [ ] Icon-only buttons have `.accessibilityLabel`
- [ ] Decorative images have `.accessibilityHidden(true)`
- [ ] State expressed as traits, not labels
- [ ] No hardcoded font sizes ÔÇö text styles or `@ScaledMetric`
- [ ] Animations gated on `accessibilityReduceMotion`
- [ ] Semantic colors (not hardcoded hex)
- [ ] `.accessibilityInputLabels` on icon-only elements
- [ ] Touch targets ÔëÑ 44├ù44pt
- [ ] Modals use `.sheet()` or `accessibilityViewIsModal`
- [ ] Swipe-only actions have `.accessibilityAction` alternatives
- [ ] `AVPlayerViewController` for video
- [ ] Photos/maps/video have `.accessibilityIgnoresInvertColors()`
- [ ] XCUITest includes `performAccessibilityAudit()` with `#available` guards (iOS 17+ / macOS 14+), plus fallback assertions on older OS versions

### Must Test on Device
- VoiceOver: navigation order, reading flow, focus after push/modal
- Voice Control: "Show numbers" coverage, "Tap [name]" activation
- Switch Control: scanning path, custom actions reachable
- Full Keyboard Access: Tab order, Escape dismissal
- Dynamic Type: layout at max size ÔÇö no clipping or overlap
- Reduce Motion: all animations verified
- Grayscale filter: information comprehensible without color
- Dark + Increase Contrast: contrast in both modes
- Captions and Audio Descriptions: auto-enable

Full testing procedures ÔåÆ `references/testing-auditing.md`

## Triage Playbook

### Blocks Assistive Tech ÔÇö fix immediately
- Icon-only button has no label ÔåÆ `references/voiceover-swiftui.md`
- Image missing alt text ÔåÆ `references/voiceover-swiftui.md`
- Custom view not in accessibility tree ÔåÆ `references/voiceover-uikit.md`
- VoiceOver loops or can't exit element ÔåÆ `references/semantic-structure.md`

### Degrades Experience ÔÇö significant friction
- Voice Control misses an element ÔåÆ `references/voice-control.md`
- Voice label doesn't match visible text ÔåÆ `references/voice-control.md`
- Touch target < 44├ù44pt ÔåÆ `references/motor-input.md`
- Color is the only differentiator ÔåÆ `references/display-settings.md`
- Wrong reading order ÔåÆ `references/semantic-structure.md`
- Modal doesn't trap focus ÔåÆ `references/semantic-structure.md`

### Incomplete Support ÔÇö blocks Nutrition Label claims
- Text doesn't scale with Dynamic Type ÔåÆ `references/dynamic-type.md`
- Animations ignore Reduce Motion ÔåÆ `references/display-settings.md`
- Low contrast in dark mode ÔåÆ `references/display-settings.md`
- No captions or audio descriptions ÔåÆ `references/media-accessibility.md`
- Platform not accessible ÔåÆ `references/platform-specifics.md`
- Nutrition Label preparation ÔåÆ `references/nutrition-labels.md`

## Troubleshooting

### Wrong framework APIs applied
**Symptom:** SwiftUI modifiers used in UIKit/AppKit code, or platform APIs mixed across frameworks.
**Fix:** Identify framework from imports (`import SwiftUI`, `import UIKit`, `import AppKit`) before applying APIs. SwiftUI uses modifiers, UIKit uses `UIAccessibility` properties, and AppKit uses `NSAccessibility` APIs.

### Over-labeling native controls
**Symptom:** `.accessibilityLabel` added to a `Button("Save")` or `Toggle("Dark Mode")` that already has visible text.
**Fix:** Do not add `.accessibilityLabel` when the control has visible text ÔÇö it overrides the automatic label and can desync with what's on screen. Only add labels to icon-only or non-text elements.

### API requires newer OS than project target
**Symptom:** Code uses platform-versioned APIs (iOS/macOS/tvOS/watchOS/visionOS) without availability checks.
**Fix:** Gate with `#available` for every target OS you support and use older equivalents when needed. Common substitutions:
- `AccessibilityNotification.Announcement("ÔÇª").post()` ÔåÆ `UIAccessibility.post(notification: .announcement, argument: "ÔÇª")`
- `performAccessibilityAudit()` ÔåÆ manual XCTest assertions
- `ViewThatFits` (iOS 16+) ÔåÆ `@Environment(\.dynamicTypeSize)` with manual layout switching
- Prefer multi-platform guards when code is shared:
  `if #available(iOS 17, macOS 14, tvOS 17, watchOS 10, visionOS 1, *) { ... }`

### Accessibility Summary missing
**Symptom:** Code generated without the "Accessibility applied" / "Test on device" summary block.
**Fix:** Always append the Accessibility Summary after code generation and fix tasks (workflow modes 1 and 2). Omit only when no accessibility patterns were added (all native controls with visible text).

### [VERIFY] comments missing on inferred labels
**Symptom:** `.accessibilityLabel` derived from SF Symbol names or method names without a `// [VERIFY]` comment.
**Fix:** Any label that was inferred (not provided by the user) must include `// [VERIFY] confirm label matches intent`. SF Symbol names like `square.and.arrow.up` rarely match what users expect to hear.

## References

- `references/voiceover-swiftui.md` ÔÇö SwiftUI accessibility modifiers, traits, actions, rotors, announcements
- `references/voiceover-uikit.md` ÔÇö UIAccessibility protocol, custom elements, containers, notifications
- `references/voice-control.md` ÔÇö Input labels, "Show numbers/names", voice-accessible alternatives
- `references/motor-input.md` ÔÇö Switch Control, Full Keyboard Access, AssistiveTouch, tvOS focus
- `references/dynamic-type.md` ÔÇö Dynamic Type, @ScaledMetric, Large Content Viewer, adaptive layouts
- `references/display-settings.md` ÔÇö Reduce Motion, Contrast, Dark Mode, Color, Transparency, Invert
- `references/semantic-structure.md` ÔÇö Grouping, reading order, focus management, rotors, modal focus
- `references/media-accessibility.md` ÔÇö Captions, Audio Descriptions, Speech synthesis, Charts
- `references/testing-auditing.md` ÔÇö Accessibility Inspector, Xcode Canvas Variants, XCTest, `performAccessibilityAudit()`, manual testing
- `references/nutrition-labels.md` ÔÇö All 9 Nutrition Labels with pass/fail criteria
- `references/wcag-mapping.md` ÔÇö WCAG 2.2 Level A/AA success criteria mapped to SwiftUI/UIKit/AppKit APIs
- `references/assistive-access.md` ÔÇö Assistive Access (iOS 17+), design principles, testing
- `references/platform-specifics.md` ÔÇö macOS, watchOS, tvOS, visionOS specifics
- `examples/before-after-swiftui.md` ÔÇö SwiftUI before/after transformations
- `examples/before-after-uikit.md` ÔÇö UIKit before/after transformations
- `examples/before-after-appkit.md` ÔÇö AppKit (macOS) before/after transformations
- `resources/audit-template.swift` ÔÇö Drop-in XCUITest file for automated accessibility auditing (iOS 17+)
- `resources/qa-checklist.md` ÔÇö Standalone QA checklist for manual testing (hand to testers)
