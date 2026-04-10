---
name: behavioral-nudges
confidence: low
description: >
  Behavioral psychology patterns for fitness app engagement and habit formation.
  Covers nudge design, momentum building, cognitive load reduction, and
  motivational reinforcement — without over-gamification. Adapted from
  agency-agents Behavioral Nudge Engine for fitness/gym context.
  Use when: designing AI coach interactions, notifications, onboarding flows,
  workout reminders, progress feedback, or any user engagement feature.
  Relevant for Neo (AI coach) and Trinity (UI/UX).
source: https://github.com/msitarzewski/agency-agents/blob/main/product/product-behavioral-nudge-engine.md
---

# Behavioral Nudges Skill — Fitness Context

## Core Principle
**Show the ONE next action, not the full plan.** Overwhelm kills motivation.
GymBro targets serious lifters — respect their intelligence, don't patronize.
Nudges should feel like a knowledgeable training partner, not a pushy app.

## ⚠️ GymBro Constraint: No Over-Gamification
Per team decision: avoid over-gamification. No XP points, no streaks with punishment,
no leaderboards. Reinforcement should feel like a good training log, not a mobile game.

## The Nudge Framework for Gym Context

### 1. Pre-Workout Nudges (Activation)
**Goal:** Get them to the gym. This is the hardest part.

| Pattern | Example | Psychology |
|---------|---------|-----------|
| Default bias | "Your Push day is ready. Tap to start." (not "What do you want to train?") | Reduce decision fatigue |
| Implementation intention | "You usually train at 6pm on Tuesdays. Ready?" | Anchor to existing habit |
| Social proof (subtle) | "Bench press is the #1 exercise logged today" | Normative influence |
| Loss aversion | "You're 2 sessions into your mesocycle — keep the momentum" | Protect investment |

### 2. During-Workout Nudges (Momentum)
**Goal:** Keep logging, maintain intensity.

| Pattern | Example | Psychology |
|---------|---------|-----------|
| Micro-celebration | "PR! 85kg × 8 — new 8RM 🏆" | Immediate reinforcement |
| Smart defaults | Pre-fill weight from last session + progression | Reduce friction |
| Progress anchor | "Set 3/4 — almost there" | Goal gradient effect |
| RPE check-in | "How hard was that? (RPE 7-8-9-10)" | Self-awareness, not punishment |

### 3. Post-Workout Nudges (Reinforcement)
**Goal:** Make them feel good about what they did.

| Pattern | Example | Psychology |
|---------|---------|-----------|
| Summary win | "45 min, 12 sets, 2 PRs. Solid session." | Completion reward |
| Trend positive | "Volume is up 8% this week vs last" | Progress visibility |
| Recovery nudge | "Rest day tomorrow — your muscles need it" | Permission to rest |
| Off-ramp | Show summary, no call to action. They're done. | Respect completion |

### 4. Between-Session Nudges (Retention)
**Goal:** Bring them back without being annoying.

| Pattern | Example | Psychology |
|---------|---------|-----------|
| Gentle reminder | "Push day ready when you are" (once, then silence) | No nagging |
| Curiosity hook | "Your squat trend looks interesting — check progress?" | Information gap |
| Coach suggestion | "Based on last week's RPE, I'd suggest..." | Expert authority |
| ❌ DON'T | "You haven't trained in 3 days! 😢" | Guilt = churn |

## Notification Strategy

### Frequency Rules
- Max 1 push notification per day (non-workout days)
- Zero notifications on rest days unless user opted in
- Workout day: 1 reminder at their usual time, that's it
- Never stack notifications — replace, don't add

### Tone
- ✅ "Your workout is ready" (neutral, enabling)
- ✅ "New PR on deadlift! 140kg × 5" (celebrating facts)
- ❌ "Don't break your streak!" (guilt-driven)
- ❌ "You're falling behind!" (punishment)
- ❌ "🔥🔥🔥 CRUSH IT TODAY" (cringe)

## AI Coach Conversation Nudges

### After a Failed Set
```
User logs: Bench 80kg × 4 (target was 8)
Bad: "You failed your target. Try harder next time."
Good: "Tough set. RPE 10? I'll adjust next session — 
       maybe 75kg × 8 to rebuild volume."
```

### Suggesting a Deload
```
Bad: "Your performance is declining. You need to deload."
Good: "RPE has been trending up the last 2 weeks while 
       volume stayed flat. A lighter week now usually 
       means a stronger week after. Want me to program one?"
```

### After Consistent Training
```
Bad: "Great job! Keep it up! 🌟⭐💪"
Good: "4 weeks consistent, volume up 12%. 
       Your squat 5RM estimate moved from 120 to 125kg."
```

## Implementation Checklist
- [ ] Smart defaults: pre-fill from last session + progression suggestion
- [ ] PR detection: automatic, with subtle celebration (not a fireworks screen)
- [ ] RPE collection: quick picker, not a form
- [ ] Post-workout summary: facts and trends, not cheerleading
- [ ] Notification: max 1/day, respectful tone, easy to disable
- [ ] Coach tone: knowledgeable partner, never patronizing
- [ ] No streaks, no XP, no points, no leaderboards
- [ ] Recovery awareness: suggest rest when fatigue signals are high
