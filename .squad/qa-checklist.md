# GymBro QA Checklist — First-Time User Journey
## Prerequisite: pm clear com.gymbro.app (fresh install)

### 1. COLD START
- [ ] App shows branded splash screen (not white/black flash)
- [ ] Splash transitions smoothly to onboarding

### 2. ONBOARDING (simulate: intermediate lifter, hypertrophy, 4 days/week)
- [ ] Name field accepts input and persists
- [ ] Weight unit selector: can switch between KG and LBS
- [ ] Goal selector: can tap ALL options (strength, hypertrophy, general). Selection visually changes.
- [ ] Experience selector: can tap ALL options (beginner, intermediate, advanced). NOT stuck on one.
- [ ] Frequency selector: can change days/week. NOT stuck on default.
- [ ] Completing onboarding generates a workout plan (not blank home)
- [ ] Snackbar/banner confirms plan creation ("Tu plan de X dias...")
- [ ] ALL text is in the correct language (no mixed EN/ES, no raw variables)

### 3. HOME SCREEN (post-onboarding)
- [ ] Shows today's workout from generated plan
- [ ] Workout card shows exercise names (not empty)
- [ ] Stats show 0 workouts, 0 active days, 0 streak (NOT fake data like 42)
- [ ] "Start Workout" button is visible and tappable
- [ ] No raw template variables (dollar-sign anything)
- [ ] No hardcoded English if device is in Spanish

### 4. START WORKOUT (the core flow)
- [ ] Tapping "Start Workout" loads exercise list FROM the plan (NOT empty)
- [ ] Each exercise shows: name, target sets, target reps
- [ ] Weight is pre-filled from smart defaults (or empty for first time — not 0)
- [ ] ProgressionEngine suggestion visible: "Last: Xkg x Y reps → Suggested: Zkg" (issue #507)
- [ ] Can log a set by entering weight/reps and tapping complete
- [ ] Haptic feedback on set completion
- [ ] Rest timer auto-starts after completing a set
- [ ] Rest timer vibrates/sounds when it ends
- [ ] Can add another exercise via "+"
- [ ] RPE input available per set
- [ ] Voice input button (red mic) is visible and functional
- [ ] Can finish workout — summary screen shows results
- [ ] Back button works (doesn't trap user)

### 5. WORKOUT HISTORY
- [ ] Completed workout appears in history
- [ ] History card shows date, exercises, volume
- [ ] Mini sparkline chart visible (if 2+ workouts)
- [ ] Can tap into workout detail
- [ ] Back button works

### 6. PROGRAMS
- [ ] Generated plan visible with correct name (not "Your First Program" with raw vars)
- [ ] Plan shows days with exercise names (not dollar-sign variables)
- [ ] Can view plan day detail
- [ ] Each day shows exercises with sets/reps
- [ ] Back button works from every sub-screen

### 7. EXERCISE LIBRARY
- [ ] Accessible from Profile
- [ ] Can search exercises
- [ ] Can filter by muscle group
- [ ] Exercise detail shows description (not blank)
- [ ] Description in correct language (bilingual)
- [ ] Equipment type visible
- [ ] Back button works (was broken — #492 fix)
- [ ] Can create custom exercise

### 8. PROGRESS / ANALYTICS
- [ ] Charts load (or show empty state gracefully — not crash)
- [ ] Plateau alerts visible if applicable (issue #502)
- [ ] PR tracking shows any records from completed workouts
- [ ] Back button works

### 9. PROFILE
- [ ] Stats reflect REAL data (0 if no workouts)
- [ ] Settings accessible
- [ ] Theme toggle works (dark/light/system)
- [ ] ALL screens respect selected theme (no mixed dark/light)
- [ ] Default theme is DARK (or SYSTEM, not forced light)
- [ ] Training phase selector works
- [ ] Can redo onboarding setup
- [ ] Back button works from all sub-screens

### 10. CROSS-CUTTING
- [ ] No crashes on any screen
- [ ] No ANR (app not responding)
- [ ] No hardcoded strings (all from strings.xml)
- [ ] No raw template variables visible anywhere
- [ ] All interactive elements have haptic feedback
- [ ] Bottom navigation works on all tabs
- [ ] App works offline (airplane mode test)
- [ ] Rotating device doesn't crash or lose state