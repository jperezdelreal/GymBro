---
'@bradygaster/gymbro': minor
---

feat: Seed 12 built-in workout templates for common training splits

Added comprehensive workout template seeding to WorkoutTemplateRepository:
- Starting Strength 5×5 (Day A & B): Classic beginner strength program
- PPL (Push/Pull/Legs): 3-day hypertrophy split with 6 exercises each
- Upper/Lower (4 days): Upper A/B and Lower A/B with strength/hypertrophy balance
- Full Body (3 days): Complete body workouts with varied movement patterns

Templates are created on first database init and marked as `isBuiltIn = true`. Each template includes:
- Specific exercise references by exact name match
- Sets/reps optimized for goal (strength: 5×5, hypertrophy: 3-4×8-12)
- Proper exercise ordering (compounds first, accessories last)
- Descriptive names and context-rich descriptions

Closes #387
