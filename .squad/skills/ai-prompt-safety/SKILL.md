---
name: "ai-prompt-safety"
description: "Safety review framework for GymBro's AI coach prompts and responses"
domain: "ai-safety"
confidence: "high"
source: "adapted from github/awesome-copilot (ai-prompt-engineering-safety-review skill)"
---

## Context

GymBro's AI coach gives training advice to real people who will perform physical exercises based on that advice. Unsafe, biased, or medically inappropriate responses can cause injury. Every prompt and system instruction for the AI coach must pass safety review.

## GymBro-Specific Safety Concerns

### Critical Safety Areas

| Risk | Example | Mitigation |
|------|---------|------------|
| Injury risk | Recommending heavy singles to a fatigued user | Check readiness score before intensity recommendations |
| Medical advice | "Your shoulder pain is probably a rotator cuff tear" | Never diagnose — always refer to medical professional |
| Eating disorders | "Cut calories to 1200 to make weight class" | Refuse extreme calorie restriction advice |
| Overtraining | Ignoring fatigue signals, pushing through pain | Respect deload triggers, flag pain reports |
| Supplement advice | Recommending specific supplements or PEDs | Decline all supplement-specific recommendations |

### Prompt Review Checklist

For every AI coach system prompt or update:

1. **Safety Assessment**
   - Could this produce advice that leads to physical injury?
   - Could responses be interpreted as medical diagnosis?
   - Are there guardrails for vulnerable users (beginners who self-report as advanced)?

2. **Bias Detection**
   - Does the prompt assume a specific body type, gender, or ability level?
   - Are training recommendations inclusive of different body compositions?
   - Does it account for age-related training modifications?

3. **Boundary Enforcement**
   - System prompt includes explicit refusal instructions for medical/supplement queries
   - Maximum intensity recommendations are capped based on training history
   - Pain/injury reports trigger immediate "see a professional" response

4. **Transparency**
   - AI always explains its reasoning ("I recommend reducing weight because your RPE was 9.5 last session")
   - Users can override all recommendations
   - Disclaimer appears on every response

## Patterns

### System Prompt Safety Blocks

```
SAFETY RULES (never override):
- Never diagnose injuries or medical conditions
- Never recommend specific supplements, medications, or PEDs
- Never suggest caloric intake below 1500 kcal/day for any reason
- If user reports pain during exercise: immediately recommend stopping and consulting a healthcare provider
- Never recommend 1RM attempts without 4+ weeks of progressive loading data
- Always respect deload recommendations from the training engine
- If readiness score < 50: recommend light session or rest day, never high-intensity work
```

### Response Filtering Pipeline

```
User Query → Content Safety API (block harmful input)
           → AI Coach (generate response)
           → Safety Filter (check output against rules)
           → Disclaimer Append
           → User
```

### Red Flag Triggers (auto-escalate to safety response)

- User mentions: pain, injury, dizzy, nauseous, chest pain, numbness
- User requests: maximum weight, competition prep without history, extreme cut
- AI suggests: weight > 2x progression rate, removing rest days, ignoring RPE feedback

## Anti-Patterns

- ❌ Deploying prompt changes without safety review
- ❌ Allowing AI to make claims about injury recovery timelines
- ❌ Training recommendations that ignore user-reported limitations
- ❌ System prompts without explicit refusal instructions
- ❌ Removing disclaimers to "improve UX flow"
- ❌ A/B testing safety guardrail removal
