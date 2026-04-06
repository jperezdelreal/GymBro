# GymBro UX Approach
**Version:** 1.0  
**Last Updated:** 2024  
**Author:** Trinity (iOS Developer)

---

## Executive Summary

GymBro is an AI-first iOS training app for serious lifters. This document defines our UX philosophy, interaction patterns, and iOS-native integration strategy. Our north star: **speed is a feature**. The best workout UI is the one you barely notice.

**Core Metrics:**
- **1-2 taps** to log a set (common case)
- **< 3 seconds** from workout start to first exercise
- **Zero context switches** during active training

---

## 1. UX Philosophy: Speed Over Everything

### 1.1 Core Principles

#### **Speed > Decoration**
Every animation, transition, and visual element must serve function, not ego. We measure success in milliseconds saved per workout. A lifter doing 20 sets saves 60 seconds if we shave 3 seconds per set—that's 60 seconds they get back under the bar.

#### **Intelligence > Configuration**
The app should know what you're about to do before you do it. Pre-fill everything: weight, reps, rest timer. Users should only tap when the default is wrong (which should be <20% of the time after 2-3 workouts).

#### **Context > Menus**
No buried settings. No multi-level navigation during workouts. Everything needed for the current context is on screen. Everything else doesn't exist until you need it.

#### **Haptics = Confirmation**
Every action gets immediate haptic feedback. Set logged = success haptic. Rest timer done = alert haptic. PR hit = celebration haptic. The phone becomes an extension of your workout.

### 1.2 Ultra-Fast Workout Logging: What It Actually Looks Like

**1-Tap Logging (Common Case - 80% of sets):**
1. Complete set
2. Tap the glowing "✓ Complete Set" button
3. Done. App auto-fills weight and reps from last workout, starts rest timer.

**2-Tap Logging (Adjustment Case - 20% of sets):**
1. Complete set with different weight/reps
2. Quick-adjust weight dial (+5/-5 buttons or gesture)
3. Tap "✓ Complete Set"
4. Done. Rest timer starts.

**What We Avoid:**
- Multiple text field taps
- Keyboard appearances mid-workout
- Modal overlays that hide context
- Navigation stack depth > 1 during active workout

---

## 2. Core Screens & Flows

### 2.1 **Active Workout Screen** (Most Important Screen in the App)

**Purpose:** Log sets faster than thought.

**Layout (Portrait):**
```
┌─────────────────────────────────┐
│ [Time] 12:34    Chest Day  [⋯]  │  ← Header: minimal, persistent
├─────────────────────────────────┤
│                                 │
│   🎯 BARBELL BENCH PRESS        │  ← Current Exercise (large, bold)
│                                 │
│   Set 3 of 5                    │  ← Progress indicator
│   ────●●●○○                     │
│                                 │
│   Last: 225 lbs × 8 reps        │  ← Context from previous workout
│   Target: 230 lbs × 8 reps      │  ← AI coach suggestion
│                                 │
│  ┌─────────────────────────┐   │
│  │      225 lbs            │   │  ← Large, gestural weight dial
│  │    ⊖   [225]   ⊕        │   │     (swipe or tap +/-)
│  └─────────────────────────┘   │
│                                 │
│  ┌─────────────────────────┐   │
│  │       8 reps            │   │  ← Rep counter (tap or voice)
│  │    ⊖    [8]    ⊕        │   │
│  └─────────────────────────┘   │
│                                 │
│  ┌─────────────────────────┐   │
│  │   ✓ COMPLETE SET        │   │  ← Primary CTA (huge, glowing)
│  │   (or swipe up →)       │   │     Alternative: swipe up gesture
│  └─────────────────────────┘   │
│                                 │
│  Rest Timer: 2:00 → Auto-start │  ← Inline rest timer
│                                 │
├─────────────────────────────────┤
│ ⊕ Add Note   💬 Ask Coach       │  ← Secondary actions (small)
└─────────────────────────────────┘
```

**Interaction Flow:**
1. **Exercise Auto-Loads:** When workout starts, first exercise is pre-loaded with smart defaults.
2. **Gesture Priority:**
   - **Swipe Up on "Complete Set"** = log set, start rest timer (1 tap equivalent)
   - **Swipe Left/Right on Weight** = ±5 lbs (or ±2.5 kg) adjustments
   - **Long-press Weight/Reps** = keyboard for manual entry (rare)
3. **Rest Timer Behavior:**
   - Auto-starts after set completion
   - Shows countdown in Dynamic Island (iOS 16+)
   - Haptic + notification when rest is done
   - Displays next set preview during rest: "Next: 225 × 8"
4. **Auto-Progression:**
   - After final set, automatically advance to next exercise
   - Transition animation: slide left (exercise exits), new exercise enters
   - No "next exercise" button needed

**Key Features:**
- **Super Set Support:** Split screen shows both exercises, toggle with tab gesture
- **Drop Set Mode:** Quick-strip buttons (-10 lbs, -25%, etc.) appear after set completion
- **Warm-Up Auto-Calculate:** Coach suggests warm-up weights (45%, 65%, 85% of working weight)
- **Voice Logging:** "Hey GymBro, eight reps" during set logs automatically (Siri Shortcuts)

---

### 2.2 **AI Coach Chat Interface**

**Purpose:** Get answers without breaking workout flow.

**Access Points:**
1. **Contextual Overlay (Primary):** Float button (💬) on Active Workout screen. Tap → sheet modal slides up from bottom, half-screen height.
2. **Dedicated Tab (Secondary):** Full chat history for pre/post-workout questions.
3. **Proactive Suggestions:** Coach surfaces automatically when relevant (see section 4.2).

**Chat UX:**
```
┌─────────────────────────────────┐
│  🧠 GymBro Coach                │  ← Dismissible sheet (swipe down)
├─────────────────────────────────┤
│                                 │
│  💡 Try adding a 4th set —      │  ← AI suggestion (proactive)
│     you're feeling strong!      │
│                                 │
│  You:                           │
│  "Should I increase weight?"    │  ← User question
│                                 │
│  Coach:                         │
│  "Yes! You hit 8 reps smoothly  │  ← AI response (actionable)
│   last 2 sets. Try 230 lbs      │
│   next week."                   │
│                                 │
│  [✓ Got It]  [Apply 230 lbs]   │  ← Quick actions (apply suggestions)
│                                 │
├─────────────────────────────────┤
│  [Ask a question...]            │  ← Input (voice or text)
│  🎤 Voice                       │
└─────────────────────────────────┘
```

**Interaction Design:**
- **Voice-First:** Microphone button default. "Hey coach, should I add weight?"
- **Quick Replies:** Common questions as chips: "Is this enough volume?" "Why am I not progressing?"
- **Actionable Responses:** Every AI suggestion includes a 1-tap apply button
- **Dismissible:** Swipe down to return to workout. Context persists.

---

### 2.3 **Progress Dashboard** (Home Screen When Not Working Out)

**Purpose:** Show trajectory, not just data.

**Layout:**
```
┌─────────────────────────────────┐
│  👤 Profile    GymBro    🔔 Alerts │
├─────────────────────────────────┤
│                                 │
│  🔥 Workout Streak: 18 days     │  ← Hero metric
│                                 │
│  ┌─────────────────────────┐   │
│  │  📈 Strength Curve      │   │  ← Chart: weight over time
│  │      (Last 8 Weeks)     │   │     (tappable for detail)
│  │  ╱╲                      │   │
│  │ ╱  ╲   ╱                │   │
│  └─────────────────────────┘   │
│                                 │
│  🎯 This Week:                  │
│  ✓ Chest Day — Mon              │  ← Completed workouts (checkmark)
│  ✓ Leg Day — Wed                │
│  ○ Back Day — Today             │  ← Upcoming (tap to start)
│                                 │
│  💪 Recent PRs:                 │
│  • Bench Press: 235 lbs × 5     │  ← Personal records (celebrate)
│  • Squat: 315 lbs × 3           │
│                                 │
│  📊 Body Metrics (from Health)  │
│  • Weight: 185 lbs (↓2 this mo) │  ← HealthKit integration
│  • Body Fat: 15%                │
│                                 │
│  [▶ START WORKOUT]              │  ← Primary CTA (large button)
│                                 │
└─────────────────────────────────┘
```

**Key Features:**
- **Trajectory Focus:** Charts show trends, not just numbers. "Up and to the right" = success.
- **Friction-Free Start:** "Start Workout" button is always visible, loads today's planned workout.
- **Contextual Insights:** "You're 12% stronger than 12 weeks ago" (AI-generated)
- **Widgets (see 5.3):** Lock screen and home screen widgets show streak, next workout, or PRs.

---

### 2.4 **Training Plan / Program View**

**Purpose:** See the forest, not just the trees.

**Layout (Scrollable Calendar):**
```
┌─────────────────────────────────┐
│  🗓 Training Plan: 12-Week Hyper│
│     Week 4 of 12                │
├─────────────────────────────────┤
│  Mon   Tue   Wed   Thu   Fri   │  ← Week view (horizontal scroll)
│  ──────────────────────────────│
│  Chest Back  Rest  Legs  Arms  │
│  ✓     ✓           ○     ○     │  ← Status: ✓ done, ○ planned
│                                 │
│  Today: Leg Day                 │
│  • Squat: 5×5                   │  ← Expanded view of today's workout
│  • Leg Press: 4×10              │     (tap to start)
│  • Hamstring Curl: 3×12         │
│                                 │
│  [▶ Start Leg Day]              │
│                                 │
│  ───────────────────────────────│
│  📊 Program Progress:           │
│  • Total Volume: 142,000 lbs    │  ← Aggregate metrics
│  • Avg Intensity: 78% 1RM       │
│  • Sessions Completed: 15/48    │
│                                 │
│  💬 Coach Notes:                │
│  "You're ahead of schedule on   │  ← AI insights
│   squat progression. Consider   │
│   adding pause squats next week"│
│                                 │
└─────────────────────────────────┘
```

**Interaction:**
- **Tap Day → Expand:** See exercise list for that day
- **Swipe Week:** Horizontal scroll to see past/future weeks
- **Long-Press Exercise → Details:** Exercise history, form videos, notes

---

### 2.5 **Exercise Library**

**Purpose:** Learn, don't browse.

**Layout:**
```
┌─────────────────────────────────┐
│  🔍 Search exercises...         │  ← Search bar (primary access)
│  [Chest] [Back] [Legs] [Arms]   │  ← Filter chips (secondary)
├─────────────────────────────────┤
│  ┌───────────────────────────┐ │
│  │ 🏋️ Barbell Bench Press    │ │  ← Exercise card
│  │ Primary: Chest, Triceps   │ │
│  │ ⭐ Your PR: 235 × 5        │ │  ← Personal context
│  │ [→ View Details]          │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │ 🎯 Incline Dumbbell Press │ │
│  │ Primary: Upper Chest      │ │
│  │ ⚡ Coach Suggests: Try this│ │  ← AI recommendation
│  │ [→ View Details]          │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

**Detail View (Modal):**
- **Hero Video:** Form demonstration (looping, silent)
- **Muscle Map:** Visual diagram of targeted muscles
- **Your History:** Last 5 workouts with this exercise (chart)
- **Substitutions:** "Can't do barbell bench? Try dumbbell press." (AI-powered)
- **Add to Workout:** Quick action button

**Key Features:**
- **Contextual Search:** "exercises for shoulder pain" → coach suggests alternatives
- **Personal Stats Front and Center:** Your PR is more important than generic info

---

### 2.6 **Recovery / Readiness View**

**Purpose:** Train smart, not just hard.

**Layout:**
```
┌─────────────────────────────────┐
│  🌟 Readiness Score: 82/100     │  ← Hero metric (AI-calculated)
│                                 │
│  Today's Recommendation:        │
│  ✅ Full Intensity Training     │  ← AI coaching
│  You're recovered and ready!    │
│                                 │
│  ───────────────────────────────│
│  📊 Recovery Factors:           │
│  • Sleep: 8.2 hrs (Good)        │  ← HealthKit + Apple Watch
│  • HRV: 65ms (Normal)           │
│  • Resting HR: 58 bpm (Good)    │
│  • Soreness: Moderate (chest)   │  ← User-reported
│                                 │
│  💡 Coach Insight:              │
│  "Your chest is still sore from │  ← Contextual advice
│   Monday. Consider lighter      │
│   weight or focus on legs today"│
│                                 │
│  ┌─────────────────────────┐   │
│  │ 📅 Weekly Recovery      │   │  ← Chart: recovery trend
│  │    (Last 7 Days)        │   │
│  │  ─●─●──●───●─●──●─●     │   │
│  └─────────────────────────┘   │
│                                 │
│  [Log Soreness] [View History]  │
└─────────────────────────────────┘
```

**Key Features:**
- **Passive Data First:** Pull from HealthKit/Apple Watch (sleep, HRV, HR)
- **Optional User Input:** Quick soreness check-in (muscle group selector, 1-5 scale)
- **AI Synthesis:** Combine objective data + subjective feel → actionable recommendation

---

### 2.7 **Settings & Profile** (Minimal by Design)

**Purpose:** Set it and forget it.

**Layout:**
```
┌─────────────────────────────────┐
│  👤 John Doe                    │
│  🎯 Goal: Build Muscle          │
├─────────────────────────────────┤
│  ⚙️ Workout Settings            │
│  • Default Rest Timer: 2:00     │
│  • Auto-Start Timer: On         │
│  • Weight Unit: lbs             │
│                                 │
│  🧠 AI Coach Preferences        │
│  • Proactive Suggestions: On    │
│  • Voice Feedback: On           │
│                                 │
│  📊 Data & Privacy              │
│  • HealthKit Integration: On    │
│  • Export Data                  │
│                                 │
│  ℹ️ About                       │
│  • Version 1.0                  │
│  • Send Feedback                │
└─────────────────────────────────┘
```

**Philosophy:** Most settings have smart defaults. Users shouldn't need to configure anything to have a great experience.

---

## 3. Interaction Design for Speed

### 3.1 Smart Defaults

**Pre-Fill Weight & Reps:**
- After 1 workout: Use previous session's data
- After 2+ workouts: Use AI-predicted progression (e.g., +5 lbs if last was easy)
- Warm-ups: Auto-calculate as % of working weight (45% → 65% → 85%)

**Rest Timers:**
- Compound lifts (squat, bench, deadlift): 3 minutes
- Isolation exercises (curls, flies): 90 seconds
- User can override once, app remembers preference per exercise

**Exercise Order:**
- Follow program order by default
- Allow drag-to-reorder if user wants to swap exercises

### 3.2 Gesture-Based Logging

**Swipe Gestures:**
- **Swipe Up on "Complete Set" button:** Log set (alternative to tap)
- **Swipe Left/Right on Weight:** ±5 lbs increments
- **Swipe Left/Right on Reps:** ±1 rep
- **Two-Finger Swipe Down on Exercise:** Skip exercise (with confirmation haptic)

**Long-Press Gestures:**
- **Long-press Weight/Reps:** Manual keyboard entry (for precise values)
- **Long-press Exercise Name:** Quick access to form video / exercise details
- **Long-press "Complete Set":** Mark as failed set (records attempt, doesn't count toward volume)

**Why Gestures?**
- Faster than tap → adjust → tap for common actions
- Feels more physical (swipe = action)
- Reduces accidental taps (gestures require intent)

### 3.3 Haptic Feedback Patterns

**Success Haptics:**
- **Set Completed:** `.success` (firm, satisfying)
- **Workout Finished:** `.success` × 3 (celebration pattern)
- **PR Hit:** `.success` + `.rigid` (double-tap sensation)

**Alert Haptics:**
- **Rest Timer Done:** `.warning` + `.medium` (attention grab, not annoying)
- **Form Warning (future):** `.warning` × 2 (coach detects bad form via Watch sensors)

**Feedback Haptics:**
- **Weight/Rep Adjustment:** `.selection` (light tick)
- **Swipe Gesture:** `.light` (confirms gesture registration)
- **Failed Set:** `.error` (gentle, not punishing)

**Philosophy:** Haptics replace visual confirmation. You should be able to log sets without looking at the screen.

### 3.4 Rest Timer Integration

**Auto-Start Behavior:**
- Timer starts immediately after set completion (no extra tap)
- Countdown displayed inline on Active Workout screen
- Notification + haptic when timer completes

**Next-Set Preview (During Rest):**
```
┌─────────────────────────────────┐
│  ⏱ Rest: 1:23 remaining         │  ← Countdown (large)
│                                 │
│  Next: Set 4 of 5               │  ← Preview next set
│  🎯 Target: 225 lbs × 8 reps    │
│                                 │
│  [Skip Rest] [Add 30s]          │  ← Quick actions
└─────────────────────────────────┘
```

**Smart Timer Adjustments:**
- If user logs next set early, timer cancels (no confirmation needed)
- If user delays past timer, no penalty—just start when ready
- Timer persists across app backgrounding (runs in background, notification when done)

**Dynamic Island Integration (iOS 16+):**
- Timer countdown visible in Dynamic Island while app is backgrounded
- Tap Island → returns to Active Workout screen

### 3.5 Quick-Add Patterns

**Supersets:**
- Tap "Add Superset" → second exercise selector appears inline
- Both exercises displayed side-by-side
- Alternate logging: Exercise A → Exercise B → Rest → repeat

**Drop Sets:**
- After completing final set, tap "Drop Set" → weight auto-reduces (e.g., -25%)
- Log additional set(s) at reduced weight
- Common in bodybuilding programs

**Warm-Up Sets:**
- Before first working set, tap "Add Warm-Ups"
- App suggests 2-3 warm-up sets (45%, 65%, 85% of working weight)
- Quick-log warm-ups with single tap per set

**Why These Patterns?**
- No modal dialogs
- No navigation away from workout screen
- All actions inline and reversible

---

## 4. AI Coach Integration in UX

### 4.1 Where Does the Coach Surface?

**Dedicated Tab (Pre/Post-Workout):**
- Full chat history
- Ask planning questions: "Should I add more chest volume?"
- Review past advice

**Contextual Overlay (Mid-Workout):**
- Float button (💬) on Active Workout screen
- Tap → half-screen sheet modal
- Swipe down to dismiss, return to workout
- Context preserved (coach remembers current exercise, set, etc.)

**Proactive Pop-Ins (Rare, High-Value Only):**
- Coach appears as banner at top of Active Workout screen
- Only for high-confidence suggestions: "Try adding 5 lbs—you've got this!"
- User can dismiss with swipe or tap "Apply"

**Widget (Lock Screen / Home Screen):**
- "Coach Tip of the Day" widget
- One-sentence insight: "Focus on tempo today—slow negatives build strength."

### 4.2 Proactive Suggestions: When to Surface the Coach

**Good Triggers (High Value, Low Annoyance):**
- ✅ **User crushes a set:** "You made that look easy! Try +5 lbs next set."
- ✅ **User struggles:** "That looked hard. Drop 10 lbs next set—quality reps matter."
- ✅ **Deload week incoming:** "You've trained hard for 3 weeks. Next week, drop intensity 20%."
- ✅ **PR potential detected:** "You're 5 lbs away from a PR. Go for it next set!"
- ✅ **Form concern (future):** "Your bar path looks uneven. Check your grip." (requires Watch sensor data)

**Bad Triggers (Avoid):**
- ❌ Every set completion (too chatty)
- ❌ Generic motivational quotes ("You got this!")
- ❌ Ads or upsells disguised as coaching
- ❌ During rest timer (user is mentally preparing, don't interrupt)

**Frequency Cap:** Max 1-2 proactive suggestions per workout. If coach has nothing important to say, stay silent.

### 4.3 How to Ask the Coach Mid-Workout

**Access Pattern:**
1. Tap 💬 float button on Active Workout screen
2. Half-screen sheet slides up
3. Microphone button pre-selected (voice-first)
4. Say: "Should I add weight?" or "Why does my shoulder hurt?"
5. Coach responds in <2 seconds (streamed text)
6. Apply suggestions with 1-tap button, or swipe down to dismiss

**Voice UI:**
- Siri Shortcuts integration: "Hey Siri, ask GymBro Coach..." works even with AirPods
- No wake word needed if app is open: tap mic, speak
- Transcription displayed inline (confirm intent)

**No Keyboard Unless Necessary:**
- Voice is 3x faster than typing in the gym
- Keyboard available as fallback (tap text input field)

### 4.4 Visual Language: AI Suggestions vs User Decisions

**AI Suggestions:**
- 💡 Icon prefix
- Light blue background (distinct from user actions)
- "Coach Suggests:" label
- Dismissible with swipe
- Actionable: [Apply] [Ignore] buttons

**User Decisions:**
- No special styling
- Standard app colors (black text, white background in light mode)
- Confirmatory haptics
- Persisted in workout log

**Example:**
```
┌─────────────────────────────────┐
│  💡 Coach Suggests:             │  ← AI suggestion (light blue bg)
│  "Add 5 lbs to your next set.   │
│   You're ready for more!"       │
│  [✓ Apply 230 lbs] [✕ Ignore]  │
├─────────────────────────────────┤
│  You logged:                    │  ← User decision (standard)
│  225 lbs × 8 reps               │
│  (Set 3 of 5)                   │
└─────────────────────────────────┘
```

---

## 5. iOS-Native Design Leverage

### 5.1 SwiftUI Patterns to Use

**NavigationStack (iOS 16+):**
- Shallow navigation hierarchy: Home → Active Workout → Exercise Details (max depth: 3)
- Pop to root when workout ends: `.navigationDestination()` with programmatic dismiss

**Sheet Modals:**
- Half-height sheets (`.presentationDetents([.medium, .large])`) for Coach overlay, exercise picker
- Swipe-to-dismiss for all modals
- No forced full-screen takeovers

**Custom Transitions:**
- Slide left/right for exercise changes (feels like flipping pages)
- Scale + fade for set completion (celebratory)
- Bounce for PR celebrations (`.spring()` animation)

**ScrollView with Lazy Loading:**
- Exercise Library and Workout History use `LazyVStack` for performance
- Infinite scroll with pagination

**Async/Await for AI Streaming:**
- Coach responses stream in real-time (no loading spinners)
- Use `AsyncStream` for LLM token streaming

### 5.2 Apple Watch Companion Concept

**What Goes on the Wrist:**

**Priority 1: Workout Logging (Simplified)**
- Display current exercise, set number, target reps/weight
- Large "Complete Set" button (full-width)
- Voice logging: "Hey Siri, log 8 reps" (Siri Shortcuts)
- Haptic when rest timer completes

**Priority 2: Health Monitoring**
- Heart rate display during workout
- Calorie burn estimate (HealthKit integration)
- Alerts if HR exceeds target zone (optional)

**Priority 3: Workout Control**
- Pause/resume workout
- Skip exercise
- End workout early

**What NOT to Include:**
- No exercise library browsing (too small)
- No chat with Coach (use iPhone)
- No program planning (use iPhone)

**Interaction Flow (Watch):**
```
┌─────────────────────┐
│  💪 Bench Press     │  ← Exercise name
│  Set 3 of 5         │
│  Target: 225×8      │
│                     │
│  [Complete Set]     │  ← Large button
│                     │
│  HR: 145 bpm        │  ← Health data
│  ⏱ Rest: 2:00      │
└─────────────────────┘
```

**Sync Strategy:**
- iPhone is source of truth
- Watch logs sets → syncs to iPhone immediately (even offline, syncs when reconnected)
- User can start workout on either device

### 5.3 Widget Design

**Lock Screen Widgets (iOS 16+):**

**Inline Widget (Small):**
```
🔥 18-day streak  |  Next: Leg Day
```

**Circular Widget (Small):**
```
┌─────┐
│  🏋️  │
│  3   │  ← Sets logged today
└─────┘
```

**Rectangular Widget (Medium):**
```
┌────────────────────────┐
│  💪 GymBro             │
│  Next: Leg Day (Today) │
│  Tap to start workout  │
└────────────────────────┘
```

**Home Screen Widgets:**

**Small Widget:**
```
┌─────────────┐
│  🔥 Streak  │
│    18 Days  │
│  🎯 Next:    │
│  Leg Day    │
└─────────────┘
```

**Medium Widget:**
```
┌─────────────────────────────┐
│  💪 GymBro Progress         │
│  📊 Strength: ↑12% (12 wks)│
│  🔥 Streak: 18 days         │
│  🎯 Next: Leg Day (Today)   │
└─────────────────────────────┘
```

**Large Widget:**
```
┌──────────────────────────────────┐
│  💪 This Week                    │
│  ✓ Mon: Chest Day                │
│  ✓ Wed: Leg Day                  │
│  ○ Fri: Back Day (Today)         │
│                                  │
│  📈 Recent PRs:                  │
│  • Bench: 235×5                  │
│  • Squat: 315×3                  │
│                                  │
│  [▶ Start Workout]               │
└──────────────────────────────────┘
```

**StandBy Mode Widget (iOS 17+):**
- Large clock + next workout info
- "Back Day in 2 hours" → tap to prepare

### 5.4 Live Activities for Active Workouts

**Purpose:** Show workout progress even when phone is locked or app is backgrounded.

**Lock Screen Live Activity:**
```
┌─────────────────────────────────┐
│  💪 GymBro Workout              │
│  Chest Day — 32:15 elapsed      │
│                                 │
│  🏋️ Bench Press (Set 3 of 5)    │
│  Last: 225 lbs × 8 reps         │
│                                 │
│  ⏱ Rest: 1:23 remaining         │
│                                 │
│  [Tap to Resume]                │
└─────────────────────────────────┘
```

**When to Start Live Activity:**
- Automatically when workout begins
- Persists until workout ends
- User can dismiss, but it's opt-out (default: on)

**Updates:**
- Real-time: exercise changes, set completions, rest timer countdown
- No manual refresh needed

### 5.5 Dynamic Island Integration During Workout

**Compact State (Minimal):**
```
┌─────────┐
│ 💪 3/5  │  ← Set progress
└─────────┘
```

**Expanded State (Long-Press):**
```
┌─────────────────────────┐
│  💪 Bench Press         │
│  Set 3 of 5             │
│  ⏱ Rest: 1:23           │
│  [Tap to Resume]        │
└─────────────────────────┘
```

**Why This Matters:**
- User can check workout progress while browsing music, responding to texts, etc.
- No need to switch back to GymBro app
- Tap Island → returns to Active Workout screen

### 5.6 Siri / Shortcuts Integration Points

**Voice Commands:**
- "Hey Siri, start my workout" → launches GymBro, loads today's workout
- "Hey Siri, log 8 reps" → logs reps for current set (during active workout)
- "Hey Siri, ask GymBro Coach..." → opens Coach chat with question

**Shortcuts Actions:**
- "Log Workout Set" (custom action): weight, reps, exercise name
- "Get Next Exercise" (custom action): returns next exercise in program
- "Check Readiness Score" (custom action): returns AI readiness score

**Automation Examples:**
- "When I arrive at gym (geofence), start GymBro workout"
- "When workout ends, log to Health app"

---

## 6. Accessibility & Inclusivity

### 6.1 VoiceOver for Workout Logging

**Key Screens with VoiceOver Support:**

**Active Workout Screen:**
- "Barbell Bench Press, Set 3 of 5. Target: 225 pounds, 8 reps. Complete Set button."
- Swipe right → "Weight: 225 pounds. Adjustable. Double-tap to edit."
- Swipe right → "Reps: 8. Adjustable. Double-tap to edit."
- Swipe right → "Complete Set button. Double-tap to log set and start rest timer."

**Rest Timer:**
- "Rest timer: 1 minute 23 seconds remaining. Next set preview: 225 pounds, 8 reps."

**Coach Overlay:**
- "Coach suggestion: Add 5 pounds to your next set. Apply button. Ignore button."

**Best Practices:**
- All buttons have descriptive labels (not just icons)
- Gestures have VoiceOver equivalents (swipe = button tap)
- Live Regions for dynamic content (rest timer countdown)

### 6.2 Dynamic Type Support

**Text Scaling:**
- All text uses SwiftUI's `.font(.body)`, `.font(.title)`, etc. (automatically scales with user settings)
- Minimum touch target: 44×44 points (WCAG 2.1 AA)
- Buttons scale with text (no fixed sizes)

**Layout Adjustments:**
- At largest text size, buttons stack vertically instead of horizontally
- Exercise cards expand to accommodate longer text

**Testing:**
- Test all screens at "Accessibility Sizes" (Settings → Accessibility → Display & Text Size)

### 6.3 One-Handed Operation

**Problem:** In the gym, one hand is often holding a dumbbell, barbell, or using a machine. The other hand operates the phone.

**Design Solutions:**

**Bottom-Heavy UI:**
- Primary actions (Complete Set, weight/rep adjustments) in bottom half of screen
- Avoid top-corner buttons (hard to reach with thumb)

**Reachability Support:**
- All critical actions accessible within thumb zone (bottom 60% of screen)
- Secondary actions (coaching, settings) can be higher (less frequent)

**Gesture Shortcuts:**
- Swipe up on Complete Set button = log set (faster than tapping small button)
- Swipe left/right on weight = adjust (no need to tap +/- buttons)

**Large Touch Targets:**
- Complete Set button: 60pt height (easy to tap without precision)
- Weight/rep buttons: 50pt height

**Landscape Mode (Optional):**
- iPad support: side-by-side exercise view + coach chat
- iPhone: landscape view shows simplified workout screen (set counter, timer, complete button)

---

## 7. Competitive Analysis & Differentiation

### 7.1 Strong App
**What They Do Well:**
- Precision tracking (granular set types, warmups, supersets)
- Offline-first (works without internet)
- Data export (CSV, analytics)
- Minimal distractions (no social features)

**Where We Differentiate:**
- **AI Coaching:** Strong has no AI. We have proactive suggestions, form feedback, program adaptation.
- **Speed:** Strong requires 3-4 taps per set (select weight, select reps, log set). We do it in 1-2 taps with smart defaults.
- **Modern iOS:** We use Live Activities, Dynamic Island, Lock Screen widgets. Strong feels like an iOS 12 app.

### 7.2 Hevy App
**What They Do Well:**
- Social features (community, sharing routines)
- Gamification (badges, streaks)
- Beautiful UI (dark mode default, animations)
- Apple Watch app

**Where We Differentiate:**
- **AI-First:** Hevy has basic analytics. We have a conversational AI coach that learns your training.
- **Intelligence > Social:** Hevy motivates through community. We motivate through personalized AI guidance.
- **Faster Logging:** Hevy is pretty fast, but still requires manual input. We pre-fill everything.

### 7.3 FitBod App
**What They Do Well:**
- AI-generated workouts (automatic program design)
- Muscle group recovery tracking (visual muscle maps)
- Apple Health integration

**Where We Differentiate:**
- **Conversational AI:** FitBod's AI is algorithmic (black box). Our AI explains *why* it suggests things and adapts to feedback.
- **Serious Lifters:** FitBod is for general fitness. We're built for progressive overload, periodization, strength training.
- **Speed:** FitBod requires users to follow guided workouts. We let advanced users log freely while providing AI guidance.

### 7.4 Apple Fitness+
**What They Do Well:**
- Tight Apple ecosystem integration (Watch, TV, Music)
- Instructor-led video classes
- HealthKit sync

**Where We Differentiate:**
- **Strength Training Focus:** Fitness+ is cardio/yoga heavy. We're barbell-first.
- **AI Coach > Human Instructors:** Videos don't adapt to *you*. Our AI does.
- **Workout Logging:** Fitness+ doesn't log sets/reps/weight. We do, with AI analysis.

---

## 8. Design Metrics & Success Criteria

### 8.1 Speed Metrics
- **Time to log a set (common case):** < 5 seconds (target: 3 seconds)
- **Time from app launch to first exercise loaded:** < 3 seconds
- **Number of taps to log a set (common case):** 1-2 taps (target: 1)
- **Rest timer accuracy:** ±1 second

### 8.2 Engagement Metrics
- **Workout completion rate:** >80% (users finish workouts they start)
- **Daily active users (during training days):** >70% (users open app on scheduled workout days)
- **Coach interaction rate:** >30% (users ask coach ≥1 question per week)

### 8.3 Satisfaction Metrics
- **App Store rating:** >4.5 stars
- **NPS (Net Promoter Score):** >50
- **User feedback sentiment:** >80% positive mentions of "speed," "easy," "fast"

### 8.4 Platform Adoption Metrics
- **Live Activity usage:** >60% (users keep Live Activity enabled)
- **Apple Watch adoption:** >40% (users with Watch use Watch app)
- **Widget usage:** >50% (users add ≥1 widget to home/lock screen)

---

## 9. Future UX Explorations (Post-V1)

### 9.1 AR Form Feedback
- Use iPhone camera + ML to analyze squat depth, bench bar path, deadlift back angle
- Real-time overlay: "Hips too high" or "Perfect rep!"
- Requires Vision framework, high compute

### 9.2 Gym Equipment Integration
- Bluetooth-enabled barbells/machines that auto-log weight
- NFC tags on equipment: tap phone to barbell → auto-load exercise

### 9.3 Social Training Partners
- Invite friends to "train together" (shared Live Activity)
- Real-time set-for-set logging (competitive or cooperative)
- Strava-style activity feed for lifters

### 9.4 Advanced Periodization
- Automatic deload weeks based on recovery data
- Tapering for powerlifting meets
- Hypertrophy → strength → peaking cycles

### 9.5 Nutrition Integration
- AI coach suggests post-workout meal based on training volume
- Calorie/protein tracking tied to workout intensity

---

## 10. Conclusion

GymBro's UX is built on a single premise: **the best workout app is the one you don't think about**. We achieve this through:

1. **Ultra-Fast Logging:** 1-2 taps per set, smart defaults, gesture-based interactions
2. **Contextual AI:** Coach surfaces when helpful, stays silent when not
3. **iOS-Native Design:** Live Activities, Dynamic Island, widgets, Watch—we leverage every platform advantage
4. **Accessibility-First:** VoiceOver, Dynamic Type, one-handed operation—everyone can train

This isn't a fitness tracker. It's a training partner that lives in your pocket, learns your lifting, and gets out of your way when you need to lift heavy.

**Next Steps:**
1. Build prototype of Active Workout screen (SwiftUI)
2. Test 1-tap logging flow with real lifters
3. Design AI coach conversation flows (what questions do users actually ask?)
4. Implement Live Activities + Dynamic Island POC

---

**Document Version History:**
- v1.0 (2024): Initial UX approach defined by Trinity
