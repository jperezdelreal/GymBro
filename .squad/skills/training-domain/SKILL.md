---
name: "Training Domain Knowledge"
description: "Comprehensive exercise science reference for AI coach, library expansion, program design, and exercise swap logic"
domain: "exercise-science"
confidence: "medium"
source: "manual - authored by Neo based on exercise science fundamentals"
---

## Context

This skill applies when:
- Expanding the exercise library (`exercises-seed.json`)
- Building AI coach workout prompts (modality-specific programming)
- Implementing exercise swap logic (movement pattern matching)
- Validating program design for user goals
- Quality-checking exercise data (form cues, equipment, muscle targeting)

**Use this skill as a reference** — not rules to memorize, but principles to consult when making exercise-related decisions.

## Training Modalities

Understanding training modalities is critical for program design and exercise selection.

### Strength / Powerlifting

**Goal:** Maximum force production (1-5 rep range)

**Key lifts:** Squat, bench press, deadlift, overhead press (the "big four")

**Programming principles:**
- Linear progression for beginners (add weight each session)
- Block periodization for intermediates (accumulation → intensification → realization)
- Conjugate/DUP for advanced (rotating max effort/dynamic effort or daily undulation)

**Accessories:** Target weak points:
- Lockout weakness → Board presses, rack pulls, floor press
- Off-chest weakness → Paused bench, tempo work
- Deficit work → Deficit deadlifts, pause squats

**Rest periods:** 3-5 minutes between main lifts (full ATP-PC recovery)

**Rep schemes:** 1-5 reps @ 80-100% 1RM, 3-6 sets

**Exercise selection priority:** Barbell compounds, minimal machines, minimal isolation

### Hypertrophy / Bodybuilding

**Goal:** Muscle size through mechanical tension, metabolic stress, muscle damage

**Key principles:**
- Time under tension (TUT): 30-60 seconds per set ideal
- Mind-muscle connection > absolute load
- Progressive overload via volume (sets × reps × weight)
- Training to or near failure (0-3 RIR)

**Split types:**
- PPL (Push/Pull/Legs) — 6 days/week or 3 days/week
- Upper/Lower — 4 days/week
- Bro split (body part per day) — 5-6 days/week
- Arnold split (Chest+Back, Shoulders+Arms, Legs) — 6 days/week

**Accessories:** Isolation is KING:
- Cables for constant tension (flies, crossovers, pulldowns)
- Machines for stability and targeting (leg press, pec deck, leg curl)
- Dumbbells for unilateral work and ROM (incline DB press, DB rows)

**Rest periods:** 60-90 seconds (metabolic stress accumulation)

**Rep schemes:** 8-15 reps @ 60-75% 1RM, 3-4 sets per exercise

**Exercise selection priority:** Mix of compounds + high isolation volume, cables/machines welcome

### Toning / General Fitness

**Goal:** Lean appearance, muscular endurance, moderate strength, overall health

**Key principles:**
- Moderate weight (50-70% 1RM)
- Higher rep ranges (12-20 reps)
- Circuit-style options for conditioning
- Emphasis on "feeling the muscle" over max load

**Split types:**
- Full body 3x/week (most common for this goal)
- Upper/Lower 4x/week
- Push/Pull 2x/week with cardio days

**Exercises:** 
- Mix of compound + isolation (not pure isolation like bodybuilding)
- Bodyweight exercises welcome and encouraged
- Machines for safety and ease of learning

**Rest periods:** 30-60 seconds (keeps heart rate elevated)

**Rep schemes:** 12-20 reps, 2-3 sets, moderate intensity

**Exercise selection priority:** Accessible movements, low injury risk, variety for engagement

### Calisthenics / Bodyweight Training

**Goal:** Bodyweight mastery, relative strength, skill acquisition, movement quality

**Critical concept: PROGRESSIONS** — Every exercise has a skill ladder. Users must start at their current level.

**Fundamental progressions:**

| Skill | Progression Ladder (easiest → hardest) |
|-------|----------------------------------------|
| **Push** | Wall push-up → Incline push-up → Knee push-up → Full push-up → Diamond push-up → Archer push-up → One-arm push-up |
| **Pull** | Dead hang → Scapular pull → Negative pull-up → Band-assisted pull-up → Full pull-up → Weighted pull-up → Muscle-up |
| **Squat** | Assisted pistol (TRX) → Box pistol → Bulgarian split squat → Full pistol squat → Shrimp squat |
| **Dip** | Bench dip → Chair dip → Parallel bar dip → Ring dip → Weighted ring dip |
| **Core** | Plank → Hollow body hold → L-sit progression → Hanging leg raise → Dragon flag → Front lever |
| **Handstand** | Wall handstand hold → Chest-to-wall handstand → Freestanding handstand → Handstand push-up (wall) → Freestanding HSPU |

**Key skills (advanced):**
- Muscle-up (pull + dip transition)
- Handstand push-up
- Front lever / Back lever
- Planche (tuck → straddle → full)
- Human flag
- One-arm pull-up

**Programming:**
- Greasing the groove (frequent sub-maximal practice)
- Skill work BEFORE strength work (neurological freshness)
- Frequency > volume for skills

**Exercise selection priority:** Progressive overload via leverage changes, not added weight (initially)

### Olympic Lifting

**Goal:** Explosive power, rate of force development, technical proficiency

**Lifts:**
- Clean & jerk (power clean, squat clean, hang clean, clean pulls)
- Snatch (power snatch, squat snatch, hang snatch, snatch pulls)

**Programming:**
- Daily undulation (vary intensity daily)
- Technique blocks (high volume, sub-maximal loads)
- Competition peaking (taper volume, increase intensity)

**Accessories:**
- Front squat (receiving position strength)
- Overhead squat (snatch stability)
- Clean pulls / Snatch pulls (power development)
- Jerk variations (split jerk, power jerk, push jerk)

**Exercise selection priority:** Technical mastery before loading, variations for weak points

## Movement Patterns (for exercise swap logic)

Movement patterns are the foundation of smart exercise substitution. **Same pattern = valid swap** (equipment/experience adjusted).

| Pattern | Primary Movers | Examples | Valid Swap Rules |
|---------|---------------|----------|------------------|
| **horizontal_push** | Chest, anterior deltoids, triceps | Bench press, push-up, dips, floor press, DB press | Same pattern = valid swap. Adjust for equipment availability. |
| **vertical_push** | Shoulders, triceps | OHP, handstand push-up, landmine press, Arnold press | Same pattern = valid swap. Vertical angle critical. |
| **horizontal_pull** | Lats, rhomboids, rear delts | Barbell row, cable row, DB row, inverted row, seal row | Same pattern = valid swap. Horizontal pull angle critical. |
| **vertical_pull** | Lats, biceps | Pull-up, chin-up, lat pulldown | Same pattern = valid swap. Vertical pull angle critical. |
| **hip_hinge** | Glutes, hamstrings, erectors | Deadlift, RDL, good morning, hip thrust, kettlebell swing | Same pattern = valid swap. Hip extension focus. |
| **knee_dominant** | Quadriceps, glutes | Squat, leg press, lunge, step-up, Bulgarian split squat | Same pattern = valid swap. Knee extension focus. |
| **carry** | Grip, core, traps | Farmer's walk, overhead carry, suitcase carry, waiter's walk | Same pattern = valid swap. Anti-movement under load. |
| **rotation** | Obliques, core | Wood chops, Russian twists, cable rotation, landmine rotation | Same pattern = valid swap. Rotational power. |
| **anti_rotation** | Obliques, core | Pallof press, single-arm farmer's walk, suitcase carry | Same pattern = valid swap. Resisting rotation. |
| **anti_extension** | Rectus abdominis, core | Plank, ab wheel rollout, dead bug, hollow body hold | Same pattern = valid swap. Resisting spinal extension. |
| **anti_flexion** | Erectors, glutes | Back extension, reverse hyper, Superman hold | Same pattern = valid swap. Resisting spinal flexion. |
| **isolation_upper** | Single muscle group | Curls, extensions, raises, flies | Same muscle group = valid swap. Check primary target. |
| **isolation_lower** | Single muscle group | Leg curl, leg extension, calf raise, hip abduction | Same muscle group = valid swap. Check primary target. |
| **explosive** | Full body power | Clean, snatch, jump, throw, medicine ball slam | Requires coaching experience. Flag for review if swapping. |

## Equipment Knowledge

Understanding equipment capabilities and limitations is critical for exercise selection and program adaptation.

| Equipment | Best For | Limitations | Home Gym Alternative |
|-----------|----------|-------------|---------------------|
| **Barbell** | Heavy compounds, progressive overload, powerlifting | Needs rack/bench, space, safety considerations | Dumbbell compounds or resistance bands |
| **Dumbbell** | Unilateral work, ROM advantage, home gym staple | Max weight limited (most sets stop at 100-120 lbs) | Adjustable dumbbells or resistance bands |
| **Cable** | Constant tension, isolation angles, muscle-building | Gym-only equipment | Resistance bands (tension curve differs) |
| **Machine** | Beginners, isolation, injury rehab, failure safety | Fixed ROM, less functional carryover | Bodyweight equivalent or dumbbells |
| **Kettlebell** | Explosive work (swings), conditioning, offset loading | Technique-dependent, limited exercise variety | Dumbbell substitute for most movements |
| **Bands** | Warm-up, rehab, accommodating resistance, travel | Inconsistent tension curve, hard to track progress | — (this IS the alternative) |
| **Bodyweight** | Anywhere training, progressions, relative strength | Hard to progressively overload beyond skill | Add weight vest or resistance bands |
| **Smith Machine** | Beginner stability, fixed bar path exercises | NOT a squat rack substitute, limited carryover | Free barbell + spotter or safety bars |
| **TRX/Rings** | Instability training, bodyweight progressions | Requires anchor point, learning curve | Bodyweight floor variations |

## Exercise Data Quality Rules

When adding exercises to `exercises-seed.json`:

1. **Name:** Use standard English name conventions:
   - ✅ "Barbell Bench Press" 
   - ❌ "Flat Bench" or "BB Bench"
   - Include equipment prefix if needed for clarity (e.g., "Dumbbell Bench Press")

2. **Muscle group (primary):** 
   - Use ONE primary target (chest, back, legs, shoulders, arms, core)
   - Bench press = Chest (NOT "Chest, Triceps, Shoulders")

3. **Secondary muscles:** 
   - List as separate field/array
   - Bench press: Primary = Chest, Secondary = Triceps, Shoulders

4. **Equipment:** 
   - What's minimally required: `barbell`, `dumbbell`, `cable`, `machine`, `bodyweight`, `kettlebell`, `band`, `other`
   - Bench press = `barbell` (NOT "Barbell + Bench" — assume gym has benches)

5. **Description/Instructions:**
   - 2-3 sentences MAX
   - Setup → Execution → Key cues
   - Example: "Lie on bench, grip barbell slightly wider than shoulders. Lower to chest, pause, then press up to lockout. Keep shoulder blades retracted throughout."

6. **YouTube URL:**
   - Link to reputable coaching source:
     - Jeff Nippard (science-based)
     - Renaissance Periodization (Dr. Mike Israetel)
     - Squat University (Dr. Aaron Horschig)
     - AthleanX (Jeff Cavaliere)
     - Alan Thrall (strength/powerlifting)
   - NO random influencer videos

7. **Movement pattern:**
   - Assign ONE of the patterns from the table above
   - This enables swap logic

8. **Common mistakes (optional but recommended):**
   - Top 2-3 form errors to avoid
   - Example for bench: "Flared elbows (aim for 45°), no leg drive, bouncing off chest"

## Coaching Cues by Pattern

Universal form cues organized by movement pattern. Use these in AI coach prompts.

| Movement | Universal Cues |
|----------|---------------|
| **Squat** | "Chest up, knees tracking toes, sit between hips, full depth if mobility allows" |
| **Deadlift** | "Flat back, push the floor away with legs, lockout with hips not lower back" |
| **Bench** | "Retract scapula, feet planted, bar path to lower chest, touch and press" |
| **OHP** | "Brace core, press bar behind ears (not in front), full lockout overhead" |
| **Row** | "Lead with elbows (not hands), squeeze shoulder blades at top, no momentum" |
| **Pull-up** | "Dead hang start, pull elbows to hips (not hands to bar), chin over bar" |
| **Dip** | "Slight forward lean for chest, upright for triceps, full ROM unless shoulder issues" |
| **Lunge** | "90/90 knee angles, front knee over ankle, torso upright, push through front heel" |
| **Plank** | "Straight line from head to heels, squeeze glutes, don't sag hips, breathe" |

## Priority Exercises by Goal

When users state a goal, prioritize these exercises in program design.

### "I want to get stronger" (Strength/Powerlifting focus)
**Must-haves:**
- Squat (back or front)
- Bench press
- Deadlift (conventional or sumo)
- Overhead press
- Barbell row

**Strong supporting cast:**
- Pull-ups / Chin-ups
- Dips
- Variations of big 3 (pause, tempo, deficit)
- Accessory work for weak points

### "I want to build muscle" (Hypertrophy/Bodybuilding focus)
**Foundation:**
- All major compounds (squat, bench, deadlift, OHP, rows)

**Volume drivers:**
- Cable flies (chest)
- Lateral raises (shoulders)
- Leg curls (hamstrings)
- Preacher curls (biceps)
- Tricep pushdowns (triceps)
- Leg press (quads)
- Leg extensions (quad isolation)
- Face pulls (rear delts)

**Philosophy:** Compounds for foundation, isolation for volume

### "I want to tone up" (General Fitness / Toning focus)
**Full-body staples:**
- Goblet squat
- Push-ups (or DB press)
- DB rows
- Lunges
- Plank variations
- Step-ups
- Glute bridges

**Circuit-friendly options:**
- Bodyweight movements
- Moderate weight DB work
- Minimal rest between exercises

### "I train at home" (Equipment-limited focus)
**With minimal equipment (dumbbells + pull-up bar):**
- Push-ups (and progressions)
- Pull-ups / Chin-ups
- Dips (on chairs if needed)
- DB goblet squats
- DB Romanian deadlifts
- DB rows
- DB shoulder press
- Plank variations
- Lunges

**Bodyweight only:**
- Push-up progressions
- Pull-up progressions (if bar available)
- Pistol squat progressions
- Dip progressions
- Plank progressions
- Glute bridges
- Nordic curls (if anchor available)

### "I'm into calisthenics" (Bodyweight mastery focus)
**Skill priorities:**
- Push-up progressions → One-arm push-up
- Pull-up progressions → One-arm pull-up or Muscle-up
- Dip progressions → Ring dips
- L-sit progressions → V-sit
- Handstand progressions → Freestanding HSPU
- Lever progressions (front/back)
- Planche progressions (advanced)

**Programming note:** Skill work FIRST in session, strength work second

## Rep Range Guidelines by Goal

Quick reference for program design:

| Goal | Rep Range | Intensity (%1RM) | Sets | Rest |
|------|-----------|------------------|------|------|
| **Max Strength** | 1-5 | 85-100% | 3-6 | 3-5 min |
| **Power** | 1-5 (explosive) | 30-60% | 3-5 | 3-5 min |
| **Hypertrophy** | 6-15 | 60-80% | 3-5 | 60-90 sec |
| **Endurance** | 15-30+ | 40-60% | 2-4 | 30-60 sec |
| **Skill Work** | Sub-maximal | N/A (bodyweight) | Many | As needed |

## Anti-Patterns

**DON'T:**

1. **Swap exercises across movement patterns** without justification:
   - ❌ Bench press → Pull-up (opposite patterns)
   - ✅ Bench press → Push-up (same pattern, different equipment)

2. **Recommend advanced calisthenics without progressions:**
   - ❌ "Do muscle-ups for back" (to a beginner)
   - ✅ "Start with negative pull-ups, progress to full pull-ups, then work on explosiveness for muscle-up transition"

3. **Ignore equipment limitations:**
   - ❌ Suggest barbell exercises to someone with only dumbbells
   - ✅ Offer DB alternatives (e.g., DB bench press instead of barbell bench)

4. **Mix modality principles incorrectly:**
   - ❌ "Do 1 rep max for toning" (wrong rep range for goal)
   - ❌ "Do 20 rep squats for strength" (wrong rep range for goal)
   - ✅ Match rep ranges to user's stated goal

5. **Create imbalanced programs:**
   - ❌ All push, no pull (recipe for shoulder injury)
   - ✅ 1:1 or 2:1 pull-to-push ratio (shoulder health)

6. **Overcomplicate beginner programs:**
   - ❌ 12 exercises, 5 days/week for a novice
   - ✅ 4-6 compounds, 3 days/week full body

## Examples

### Exercise Swap Logic (Movement Pattern Matching)

User wants to swap "Barbell Bench Press" due to shoulder pain:

1. Identify pattern: `horizontal_push`
2. Check other `horizontal_push` exercises in library
3. Filter by:
   - Available equipment (does user have dumbbells?)
   - Experience level (can they do dips safely?)
   - Injury considerations (DB press = more ROM control, easier on shoulders)
4. Suggest: "Dumbbell Bench Press" or "Push-ups" (both horizontal push, different loading)

### Program Design (Goal-Based)

User goal: "I want to build muscle in my chest"

1. Identify modality: Hypertrophy
2. Select exercises:
   - Compound: Barbell or DB bench press (strength driver)
   - Isolation: Cable flies (constant tension)
   - Variation: Incline DB press (upper chest)
3. Rep scheme: 8-12 reps, 3-4 sets each
4. Rest: 60-90 seconds
5. Volume: 12-16 sets per week for chest (per hypertrophy research)

### Calisthenics Progression Assessment

User says: "I can do 5 push-ups"

1. Assess current level: Early in push-up progression
2. Don't jump to: Diamond push-ups, archer push-ups (too advanced)
3. Recommend:
   - **Current:** Full push-ups (build to 3 sets of 10-15)
   - **Next step:** Wide-grip push-ups or tempo push-ups (3-1-3)
   - **Future:** Diamond push-ups → Archer → One-arm
4. Programming: Frequency over intensity (push-ups 3-4x/week, sub-maximal sets)

## Training Phases (Periodization)

Understanding training phases allows the AI coach to adapt recommendations based on where the user is in their training cycle.

### Bulk / Volume Phase

**Goal:** Maximize muscle growth through progressive overload

**Characteristics:**
- Caloric surplus: +300-500 calories above maintenance
- Progressive overload focus: Add weight, reps, or sets each week
- Higher training volume: 15-25 sets per muscle group per week
- Rep ranges: 6-15 reps (hypertrophy-focused)
- Rest periods: 60-90 seconds for accessories, 2-3 min for compounds
- Conditioning: Minimal (1-2x/week light LISS to maintain cardiovascular health)
- Exercise selection: Mix of compounds + high isolation volume
- Duration: 8-16 weeks typical

**Key mindset:** Embrace the surplus. Strength should increase steadily. Some fat gain is expected and acceptable.

### Cut / Definition Phase

**Goal:** Reduce body fat while maintaining muscle mass

**Characteristics:**
- Caloric deficit: -300-500 calories below maintenance
- Strength maintenance priority: Accept that PRs won't happen
- Reduced volume: 12-18 sets per muscle group per week (prevent overtraining in deficit)
- Rep ranges: 6-12 reps (stay in strength-preserving range)
- Rest periods: 2-3 minutes (allow full recovery despite reduced energy)
- Conditioning: Moderate to high (2-3x LISS + 1-2x HIIT per week)
- Exercise selection: Keep main compounds, reduce accessories slightly
- Duration: 8-16 weeks typical, adjust based on fat loss rate (0.5-1% bodyweight per week)

**Key mindset:** Goal is NOT to gain strength. Goal is to NOT lose strength. Protect muscle while shedding fat.

### Maintenance Phase

**Goal:** Sustain current physique with lifestyle-compatible training

**Characteristics:**
- Caloric balance: At maintenance
- Moderate everything: Volume, intensity, frequency all sustainable
- Volume: 10-15 sets per muscle group per week (minimum effective dose)
- Rep ranges: 8-12 reps (comfortable middle ground)
- Rest periods: 90-120 seconds
- Conditioning: 2-3x per week (health-focused)
- Exercise selection: Whatever user enjoys and will stick to
- Duration: Indefinite (this is the "real life" phase)

**Key mindset:** Training for life, not for a goal. Consistency beats perfection.

### Deload Week

**Purpose:** Allow accumulated fatigue to dissipate, prevent overtraining, break through plateaus

**When to take:**
- Every 4-6 weeks for intermediate/advanced lifters
- Every 8-12 weeks for beginners (they don't accumulate fatigue as quickly)
- When user reports: persistent soreness, declining performance, poor sleep, irritability, elevated resting heart rate

**How to deload:**
- Reduce intensity: 50-60% of normal working weight
- Reduce volume: Cut sets by 40-50% (e.g., 4 sets → 2 sets)
- Keep frequency: Still train same days (maintain habit)
- Keep exercises: Don't change program during deload
- Duration: 1 week (rarely 2 weeks)

**Common mistake:** Skipping deloads because "I feel fine." Deloads are preventative, not reactive.

### Peaking Phase

**Goal:** Prepare for maximum performance on a specific date (powerlifting meet, max testing)

**Characteristics:**
- Specificity focus: Train the exact lifts you'll be tested on
- Reduce volume: Drop accessories dramatically, focus on main lifts
- Increase intensity: Work up to 90-95% 1RM singles/doubles
- Taper: Final week before competition is super light (opener weight only)
- Duration: 2-4 weeks
- Who needs it: Powerlifters, strength athletes preparing for testing

**Not applicable to:** General gym-goers, hypertrophy-focused lifters

## Periodization Models

Different periodization approaches work better for different experience levels and goals.

### Linear Periodization

**Best for:** Beginners (first 6-12 months of training)

**How it works:**
- Add weight every single session (e.g., +5 lbs on squat every workout)
- Rep scheme stays constant (e.g., always 3x5)
- Progress linearly until you can't add weight anymore ("stall")
- When you stall 3 sessions in a row, reset weight by 10% and build back up

**Example:**
- Week 1: Squat 135 lbs x 3x5
- Week 2: Squat 140 lbs x 3x5
- Week 3: Squat 145 lbs x 3x5
- (Continue until failure, then reset)

**Why it works for beginners:** Neurological adaptations happen fast. They can add weight EVERY session.

**Why it fails for intermediates:** Eventually you can't recover fast enough to add weight every session.

### Daily Undulating Periodization (DUP)

**Best for:** Intermediate lifters (1-3 years of training)

**How it works:**
- Vary rep ranges within the same week
- Same lifts, different intensities and volumes
- Allows more frequent training of same movement without overtraining

**Example weekly layout:**
- Monday: Squat 5x5 @ 80% (strength)
- Wednesday: Squat 4x10 @ 65% (hypertrophy)
- Friday: Squat 8x3 @ 75% (power/speed)

**Why it works:** Provides varied stimulus, allows high frequency without CNS burnout, trains multiple qualities simultaneously.

### Block Periodization

**Best for:** Intermediate to advanced lifters with specific goals

**How it works:**
- Divide training into sequential blocks, each emphasizing one quality
- Each block builds on the previous
- Typical blocks: Accumulation → Intensification → Realization

**Block breakdown:**

| Block | Duration | Goal | Volume | Intensity | Rep Range |
|-------|----------|------|--------|-----------|-----------|
| **Accumulation** | 4-6 weeks | Build work capacity, hypertrophy | Very high | Low-moderate (60-75%) | 8-15 reps |
| **Intensification** | 3-4 weeks | Convert size to strength | Moderate | High (75-90%) | 3-6 reps |
| **Realization** | 2-3 weeks | Peak for max performance | Low | Very high (90-100%) | 1-3 reps |

**Why it works:** Each block creates a specific adaptation. They stack to produce peak performance at planned time.

### Conjugate Method

**Best for:** Advanced powerlifters (Westside Barbell method)

**How it works:**
- Max Effort day: Work up to 1-3 rep max in a variation (changes every 1-2 weeks)
- Dynamic Effort day: Speed work with 50-70% for explosive power (same movement)
- Accessory work: High volume targeting weak points
- Example: ME Lower, DE Lower, ME Upper, DE Upper (4 days/week)

**Why it works:** Prevents accommodation (body adapting to same stimulus), trains multiple qualities, keeps training fresh.

**Why it's advanced only:** Requires excellent technique, autoregulation skills, and deep understanding of weak points.

### When to Use Each Model

| Experience Level | Best Model | Reasoning |
|-----------------|------------|-----------|
| **Beginner (0-1 year)** | Linear Periodization | Simplest, fastest gains, builds consistency habit |
| **Early Intermediate (1-2 years)** | DUP or simple block periodization | Need more variation, can't linearly progress anymore |
| **Intermediate (2-4 years)** | Block Periodization | Goal-specific training, planned peaks |
| **Advanced (4+ years)** | Conjugate or advanced DUP | Need constant variation, highly individualized |

## Progression Principles

Knowing WHEN and HOW to progress is what separates spinning wheels from getting results.

### When to Increase Weight (Double Progression)

**Rule:** Hit the top of your rep range for ALL sets → increase weight

**Example:**
- Goal: 3 sets in 8-12 rep range
- Session 1: 100 lbs → 12, 10, 9 reps (DON'T increase yet)
- Session 2: 100 lbs → 12, 11, 10 reps (getting closer)
- Session 3: 100 lbs → 12, 12, 11 reps (almost there)
- Session 4: 100 lbs → 12, 12, 12 reps (NOW increase!)
- Session 5: 105 lbs → 10, 9, 8 reps (back to bottom of range, build back up)

**How much to increase:**
- Big compounds (squat, deadlift): 5-10 lbs
- Upper body compounds (bench, OHP): 2.5-5 lbs
- Isolation exercises: 2.5-5 lbs or add 1-2 reps

### When to Change Exercises

**Main compounds (squat, bench, deadlift, OHP, rows):** DON'T change unless injured or stalled for 6+ weeks

**Accessories (isolation work):** Change every 4-8 weeks to prevent accommodation

**Reason:** Main lifts are your progress measuring stick. Keep them constant. Accessories are tools to support main lifts.

**Example program evolution:**

| Weeks 1-6 | Weeks 7-12 |
|-----------|------------|
| Squat (keep) | Squat (same) |
| Bench (keep) | Bench (same) |
| Deadlift (keep) | Deadlift (same) |
| Cable flies (change) | → Dumbbell flies |
| Lateral raises (change) | → Upright rows |
| Tricep pushdowns (change) | → Overhead extensions |

### When to Change Programs

**Signs you need a new program:**
- Stalling for 2+ weeks despite deload
- Boredom affecting consistency (mental gains matter)
- Life circumstances changed (less time available)
- Goals changed (strength → hypertrophy)

**Signs you DON'T need a new program:**
- You've been on it for 2 weeks and "not seeing results" (give it 8+ weeks)
- Your friend is doing something else that looks cool (trust the process)
- You had one bad workout (happens to everyone)

### RPE and RIR Guide

**RPE = Rate of Perceived Exertion (1-10 scale)**
**RIR = Reps in Reserve (how many more reps you could do)**

| RPE | RIR | Description | When to Use |
|-----|-----|-------------|-------------|
| **RPE 6** | 4 RIR | Easy, could do 4+ more reps | Warm-up sets, deload week |
| **RPE 7** | 3 RIR | Moderate effort, 3 more reps possible | First set of accessories, hypertrophy work |
| **RPE 8** | 2 RIR | Challenging, 2 more reps possible | Most working sets, sustainable volume |
| **RPE 9** | 1 RIR | Very hard, 1 more rep possible | Top sets, last set of main lifts |
| **RPE 10** | 0 RIR | True failure, couldn't do another rep | Rarely (ego-driven), only last set of isolations if pushing |

**Practical application:**
- Most training should be RPE 7-8 (2-3 RIR)
- Main lifts: RPE 8-9
- Accessories: RPE 7-8
- DON'T train to failure (RPE 10) on compounds (injury risk, excessive fatigue)
- Beginners: Train RPE 7-8 max (learn to gauge effort)

### Autoregulation

**Definition:** Adjust training based on daily readiness instead of rigid planning

**How to autoregulate:**

1. **Top set RPE:** Work up to a prescribed RPE instead of fixed weight
   - Plan: "Squat 3x5 @ RPE 8"
   - Bad day: Hit RPE 8 at 225 lbs (lighter than usual)
   - Great day: Hit RPE 8 at 245 lbs (heavier than usual)

2. **Backoff percentage:** After top set, drop weight by X% for volume work
   - Example: Top set @ RPE 9, then 3x5 @ 85% of that weight

3. **Rep max testing:** Work up to a daily max, then program off that
   - Find your 5RM today, then do volume work at 85% of that

**When to use autoregulation:**
- Advanced lifters who understand their bodies
- During high-stress life periods (work, family)
- When recovering from illness/injury
- NOT for beginners (they don't have gauge calibrated yet)

## Split Selection by Frequency

The right split depends on how many days per week the user can train. Here's how to match split to frequency.

### 2 Days Per Week

**Best split:** Full Body A/B

**Rationale:** Need to hit everything twice per week minimum. Only way to do it in 2 sessions is full body.

**Structure:**
- Day 1: Squat focus, horizontal push, vertical pull, accessories
- Day 2: Deadlift focus, vertical push, horizontal pull, accessories

**Example:**
- **Day A:** Squat, Bench Press, Pull-ups, Leg Curl, Tricep Extension
- **Day B:** Deadlift, Overhead Press, Barbell Row, Leg Press, Bicep Curl

**Who it's for:** Busy professionals, beginners easing in, maintenance phase

### 3 Days Per Week

**Best split:** Full Body or Upper/Lower/Full

**Option 1 - Full Body A/B/C:**
- Day A: Squat emphasis
- Day B: Deadlift emphasis
- Day C: Balanced

**Option 2 - Upper/Lower/Full:**
- Day 1: Upper (push + pull)
- Day 2: Lower (squat + hinge)
- Day 3: Full body (lighter, different variations)

**Who it's for:** Most beginners, intermediate lifters with limited time, optimal frequency for compound learning

### 4 Days Per Week

**Best split:** Upper/Lower (classic) or PHUL (Power Hypertrophy Upper Lower)

**Classic Upper/Lower:**
- Day 1: Upper A (bench focus)
- Day 2: Lower A (squat focus)
- Day 3: Upper B (OHP focus)
- Day 4: Lower B (deadlift focus)

**PHUL variant:**
- Day 1: Upper Power (3-6 reps)
- Day 2: Lower Power (3-6 reps)
- Day 3: Upper Hypertrophy (8-12 reps)
- Day 4: Lower Hypertrophy (8-12 reps)

**Who it's for:** Intermediate lifters, those who want balanced development, good work-life balance

### 5 Days Per Week

**Best split:** Upper/Lower/Push/Pull/Legs or PPLUL

**Option 1 - PPLUL:**
- Day 1: Push (chest, shoulders, triceps)
- Day 2: Pull (back, biceps)
- Day 3: Legs (quads, hams, glutes)
- Day 4: Upper (full upper body, moderate volume)
- Day 5: Lower (full lower body, moderate volume)

**Option 2 - Hybrid:**
- Day 1: Upper Power
- Day 2: Lower Power
- Day 3: Push Hypertrophy
- Day 4: Pull Hypertrophy
- Day 5: Legs Hypertrophy

**Who it's for:** Advanced lifters, those with flexible schedules, bodybuilding focus

### 6 Days Per Week

**Best split:** Push/Pull/Legs x2 (PPL twice per week)

**Structure:**
- Day 1: Push A (chest focus, heavy)
- Day 2: Pull A (back focus, heavy)
- Day 3: Legs A (squat focus, heavy)
- Day 4: Push B (shoulders focus, volume)
- Day 5: Pull B (lats focus, volume)
- Day 6: Legs B (deadlift focus, volume)

**Who it's for:** Advanced lifters, bodybuilders, those with established recovery habits (sleep, nutrition on point)

**Recovery note:** 6 days requires excellent sleep (8+ hours), nutrition dialed in, and active recovery practices. NOT for beginners.

## Beginner-Specific Guidelines

Beginners (first 6-12 months) have unique needs. Getting this phase right sets the foundation for years of progress.

### First 3 Months: Movement Literacy Over Numbers

**Priority 1:** Learn to perform movements correctly
- Squat with full depth and neutral spine
- Hip hinge without rounding back
- Press without flaring elbows excessively
- Pull without using momentum

**Priority 2:** Build the habit
- Show up consistently (even if session isn't perfect)
- 3 days per week is the sweet spot
- Sessions don't need to be long (45-60 minutes is plenty)

**Priority 3:** Progressive overload (but slowly)
- Add weight when form is solid
- Don't chase PRs yet
- Build work capacity

**DON'T:**
- Max out in first month
- Try advanced techniques (drop sets, supersets, etc.)
- Switch programs every 2 weeks
- Compare yourself to veteran lifters

### Movement Literacy Checklist

Before adding significant weight, beginners should demonstrate competency in these fundamental patterns:

| Pattern | Test Exercise | Competency Standard |
|---------|---------------|---------------------|
| **Squat** | Bodyweight squat | 10 reps with full depth, knees tracking toes, chest up, no butt wink |
| **Hip Hinge** | Dumbbell RDL | 15 reps with 25-35 lbs, flat back throughout, feeling hamstrings |
| **Horizontal Push** | Push-up | 10 strict push-ups (full ROM, no sagging hips) |
| **Vertical Push** | Dumbbell overhead press | 10 reps with 15-20 lbs each hand, controlled, full lockout |
| **Horizontal Pull** | Dumbbell row | 12 reps with 25-35 lbs, no momentum, squeezing shoulder blades |
| **Vertical Pull** | Lat pulldown OR assisted pull-up | 10 controlled reps, full ROM |
| **Core Anti-Extension** | Plank | 60 seconds with perfect form (no sagging) |

**Once these are checked off:** Ready to load barbells and progress systematically.

### Common Beginner Mistakes

| Mistake | Why It's Bad | Fix |
|---------|--------------|-----|
| **Too much weight, too soon** | Form breaks down, injury risk, reinforces bad patterns | Start with JUST the bar (45 lbs). Add 5-10 lbs per week. |
| **Not enough rest** | Can't recover, stall quickly, burnout | 48 hours between sessions for same muscle group |
| **Skipping legs** | Imbalanced physique, limits overall strength | Squat or deadlift EVERY session (alternating) |
| **Ego lifting** | Using momentum, partial ROM to lift heavier | Leave ego at door. Full ROM > heavy weight with bad form |
| **Program hopping** | Never adapt to a stimulus, no progress tracking | Commit to 8-12 weeks minimum before switching |
| **Only doing what they like** | Imbalanced development, weak points stay weak | Balanced push/pull ratio, don't skip movements you suck at |
| **Too many exercises** | Can't recover, diluted effort, no progressive overload | 4-6 exercises per session max (beginners) |

### When to Graduate from Beginner Programs

**Signs you're ready for intermediate programming:**
1. Can't add weight every session anymore (linear progression stalls)
2. Have been training consistently for 6-12 months
3. Hit these minimum strength standards (for males; females ~60% of these):
   - Squat: 1.25x bodyweight for 5 reps
   - Bench: 1x bodyweight for 5 reps
   - Deadlift: 1.5x bodyweight for 5 reps

**What changes:**
- Move from linear periodization to DUP or block periodization
- Add more exercise variation
- Increase volume and frequency
- Use autoregulation tools (RPE, RIR)

### Recommended First Exercises by Muscle Group

Choose these for beginners — they're safest, easiest to learn, and build the most transferable strength.

| Muscle Group | Best Beginner Exercise | Why |
|--------------|----------------------|-----|
| **Chest** | Dumbbell bench press OR push-ups | DB allows natural arm path, easier on shoulders. Push-ups teach body control. |
| **Back (vertical)** | Lat pulldown | Easier to learn than pull-ups, adjustable resistance |
| **Back (horizontal)** | Dumbbell row (single-arm) | Unilateral = easier to feel muscle working, natural movement |
| **Shoulders** | Dumbbell shoulder press | Safer bar path than barbell, individual arm control |
| **Legs (quad)** | Goblet squat | Teaches upright torso, counterbalance makes depth easier, can't overload dangerously |
| **Legs (hinge)** | Romanian deadlift (RDL) with dumbbells | Easier to learn hip hinge than conventional deadlift, less technical |
| **Biceps** | Dumbbell curls | Simple, hard to mess up |
| **Triceps** | Overhead dumbbell extension | Good stretch, easy to feel muscle working |
| **Core** | Plank variations | Safe, effective, teaches bracing |

**Progression plan:** Master these for 8-12 weeks → Introduce barbell variations → Add complexity over time

## Warm-Up Protocols

Proper warm-up reduces injury risk, improves performance, and primes movement patterns. Different situations require different warm-up strategies.

### General Warm-Up (Start of Session)

**Purpose:** Raise core body temperature, increase blood flow, activate nervous system

**Duration:** 5-10 minutes

**Methods:**
- Light cardio: Treadmill walk (incline), bike, rowing, elliptical
- Dynamic calisthenics: Jumping jacks, arm circles, leg swings
- Goal: Break a light sweat, feel "ready"

**DON'T:** Static stretching cold (saves for post-workout)

### Specific Warm-Up (Before Each Main Lift)

**Purpose:** Prepare exact movement pattern, build to working weight gradually

**Structure (Ramp-Up Sets):**

| Set | Load | Reps | Rest |
|-----|------|------|------|
| **Set 1** | Empty bar (45 lbs) | 8-10 | 30 sec |
| **Set 2** | 50% of working weight | 5-6 | 60 sec |
| **Set 3** | 70% of working weight | 3-4 | 90 sec |
| **Set 4** | 85% of working weight | 1-2 | 2 min |
| **Working Set** | 100% (target weight) | Prescribed reps | Full rest |

**Example:** Working weight is 225 lbs bench press
- 45 lbs x 10
- 115 lbs x 5
- 160 lbs x 3
- 190 lbs x 1
- 225 lbs x 5 (working set)

**Rule:** Heavier the working weight, more ramp-up sets needed. Lighter working weight (accessories) = fewer warm-up sets.

### Dynamic Stretching (Before Lifting)

**Purpose:** Increase range of motion temporarily, activate muscles

**Best dynamic stretches by area:**

| Area | Stretch | Reps |
|------|---------|------|
| **Hips** | Leg swings (forward/back, side-to-side), walking lunges, hip circles | 10-12 each |
| **Shoulders** | Arm circles, band pull-aparts, wall slides | 10-15 |
| **Ankles** | Ankle circles, calf raises, toe walks | 10-15 each |
| **Thoracic Spine** | Cat-cow, thoracic rotations, foam roller extensions | 8-10 |
| **Hamstrings** | Walking toe touches, leg kicks, inchworms | 10-12 |

**When to do:** After general warm-up, before lifting (part of warm-up routine)

### Static Stretching (After Lifting)

**Purpose:** Improve flexibility, aid recovery, cool down

**Duration:** Hold each stretch 20-30 seconds, 2-3 sets

**Key static stretches:**
- Hamstrings: Seated toe touch
- Hip flexors: Lunge stretch
- Chest: Doorway pec stretch
- Lats: Overhead lat stretch
- Quads: Standing quad pull

**DON'T:** Static stretch before lifting (temporarily reduces force production)

### Mobility Work for Common Tight Areas

Most gym-goers have desk jobs. These areas need extra attention:

**Hip Flexors (tight from sitting):**
- Couch stretch: 2 min per side
- 90/90 hip stretch: 2 min per side
- Walking lunges with overhead reach: 10 per leg

**Thoracic Spine (rounded from computer work):**
- Foam roller thoracic extensions: 10 reps
- Thread the needle: 8 per side
- Wall slides: 12 reps

**Ankles (poor dorsiflexion limits squat depth):**
- Ankle mobilization with band: 15 reps per side
- Goblet squat hold: 30-60 seconds
- Calf stretch: 30 seconds per leg

**Frequency:** 2-3x per week, can be separate session or post-workout

## Recovery & Fatigue Management

Training is the stimulus. Recovery is when adaptations happen. Managing fatigue is a skill.

### Sleep: The #1 Recovery Factor

**Target:** 7-9 hours per night (8 is sweet spot for most)

**Why it matters:**
- Muscle protein synthesis peaks during sleep
- Growth hormone release happens during deep sleep
- Neurological recovery (CNS fatigue dissipates)
- Insulin sensitivity improves (better nutrient partitioning)

**Performance impact:**
- 6 hours of sleep = ~15% strength decrease
- Poor sleep = increased injury risk
- Chronic poor sleep = cortisol elevation, muscle loss, fat gain

**Sleep hygiene basics:**
- Consistent bedtime (even weekends)
- Cool room (65-68°F ideal)
- Dark room (blackout curtains or eye mask)
- No screens 1 hour before bed
- Avoid caffeine after 2pm

### Nutrition Timing

**Protein timing:**
- Post-workout: Get protein within 2 hours (anabolic window is longer than bro-science claims)
- BUT: Total daily protein intake matters WAY more than timing
- Target: 1.6-2.2g per kg bodyweight spread across 3-5 meals

**Carbs around training:**
- Pre-workout (1-2 hours before): Moderate carbs for energy
- Post-workout: Carbs to replenish glycogen, aid recovery
- Not critical for casual lifters, matters more for athletes training 2x/day

**Bottom line:** Don't stress timing. Hit your daily totals. Consistency > perfection.

### Signs of Overtraining (and What to Do)

| Symptom | What It Means | Action |
|---------|---------------|--------|
| **Persistent fatigue** | CNS isn't recovering between sessions | Deload week OR take 3-5 days off completely |
| **Declining performance** | Weights that were easy now feel heavy | Deload, check sleep and nutrition |
| **Mood changes** | Irritability, lack of motivation, depression | Rest, evaluate life stress, might need break |
| **Elevated resting HR** | Check morning HR — if 10+ BPM above normal | Sign of systemic stress, back off training |
| **Persistent muscle soreness** | Soreness that won't go away even after 3-4 days | Reduce volume, active recovery |
| **Insomnia** | Can't fall asleep despite being tired | Reduce training intensity, manage stimulants |
| **Frequent illness** | Getting sick often, low immune function | You're run down. Take time off. |

**Overtraining vs. Overreaching:**
- Overreaching = Short-term fatigue, resolved with deload (normal and expected)
- Overtraining = Chronic fatigue, requires weeks to months to recover (rare, but serious)

**Prevention:** Deload every 4-6 weeks, prioritize sleep, don't add volume/intensity too quickly

### Active Recovery

**Definition:** Light movement that promotes blood flow without creating fatigue

**Best active recovery activities:**
- 20-30 min walk
- Easy bike ride
- Swimming (low-intensity)
- Yoga or stretching session
- Foam rolling
- Light bodyweight calisthenics

**When to use:** Rest days between training sessions

**What it does:** Increases blood flow to muscles (brings nutrients, removes waste), reduces soreness, maintains movement patterns

**What it's NOT:** Another workout. If you're sweating hard, it's not active recovery.

### How to Structure Rest Days

**Option 1 - Complete rest:**
- Nothing structured
- Walk if you want, but no gym
- Best for: High-stress weeks, feeling run down

**Option 2 - Active recovery:**
- Light movement (see above)
- Mobility work
- Best for: Maintenance phase, younger lifters

**Option 3 - Skill work:**
- Practice movements at low intensity (handstands, jump rope, etc.)
- NOT fatiguing
- Best for: Calisthenics athletes, sports-specific training

**How many rest days per week:**
- Beginners: 3-4 rest days (training 3-4x/week)
- Intermediate: 2-3 rest days (training 4-5x/week)
- Advanced: 1-2 rest days (training 5-6x/week)

**Critical rule:** At least 1 FULL rest day per week (no negotiation)

## Conditioning for Lifters

Cardio doesn't kill gains. EXCESSIVE cardio kills gains. Here's how to do it right.

### LISS (Low Intensity Steady State)

**Definition:** 30-60 minutes at conversational pace (can hold a conversation while doing it)

**Methods:**
- Walking (treadmill with incline ideal)
- Cycling
- Swimming
- Rowing (low intensity)
- Elliptical

**Heart rate target:** 60-70% of max HR (roughly 120-140 BPM for most people)

**Benefits:**
- Improves cardiovascular health
- Aids recovery (blood flow)
- Burns calories without impairing strength training
- Low CNS fatigue

**Frequency:**
- Bulk phase: 1-2x per week (maintain cardio base)
- Cut phase: 3-4x per week (increase calorie deficit)
- Maintenance: 2-3x per week (health)

**When to do it:** Separate from lifting (ideally 6+ hours apart), OR after lifting (never before)

### HIIT (High Intensity Interval Training)

**Definition:** Short bursts of max effort followed by rest (15-25 minutes total)

**Structure:**
- Work interval: 20-40 seconds at 90-95% max effort
- Rest interval: 1-2 minutes active recovery
- Rounds: 6-10 rounds
- Total time: 15-25 minutes

**Methods:**
- Assault bike sprints
- Rowing sprints
- Sled pushes
- Hill sprints
- Jump rope intervals

**Benefits:**
- Time-efficient calorie burn
- Improves VO2 max
- Preserves muscle better than long steady-state cardio
- Metabolic boost (EPOC effect)

**Drawbacks:**
- High CNS fatigue
- Can impair strength training if overdone
- Harder to recover from

**Frequency:**
- Bulk phase: 0-1x per week (optional)
- Cut phase: 1-2x per week
- Maintenance: 1x per week

**CRITICAL:** Don't do HIIT the day before or day of heavy leg training (it WILL hurt performance)

### When to Do Cardio Relative to Lifting

**Best option:** Separate sessions (morning cardio, evening lifting OR vice versa)
- Allows full recovery between modalities
- No interference effect

**Second best:** Cardio AFTER lifting
- Lifting performance not impaired
- Glycogen already depleted, cardio burns more fat

**WORST option:** Cardio BEFORE lifting
- Pre-fatigues muscles
- Depletes glycogen needed for strength
- Increases injury risk during lifts

**Exception:** 5-10 min light cardio before lifting is fine (that's warm-up, not conditioning)

### Conditioning Exercises Suitable for the Gym

Not everyone wants to run. Here are gym-based conditioning options:

| Exercise | Type | Duration/Distance | Intensity | Best For |
|----------|------|-------------------|-----------|----------|
| **Battle ropes** | HIIT | 20 sec on / 40 sec off x 8 rounds | High | Full body power endurance |
| **Sled push/pull** | HIIT or LISS | 20-40 meters x 8-10 trips | Variable (load-dependent) | Leg conditioning, low injury risk |
| **Assault bike** | HIIT | 30 sec sprint / 90 sec easy x 6-8 | Very high | Cardio capacity, brutal calorie burn |
| **Rowing machine** | LISS or HIIT | 500m sprints OR 30 min steady | Variable | Full body, low impact |
| **Jump rope** | HIIT | 1 min on / 1 min off x 10 | Moderate-high | Coordination, calf endurance |
| **Kettlebell swings** | HIIT | 20-30 swings x 5-8 rounds | Moderate-high | Hip hinge power, posterior chain |
| **Box jumps** | HIIT | 5-10 jumps x 5 rounds | Moderate-high | Explosive power, leg conditioning |
| **Med ball slams** | HIIT | 10-15 slams x 5-8 rounds | High | Full body power, core conditioning |
| **Farmer's walks** | Hybrid | 40-60 meters x 4-6 trips | Moderate | Grip, core, carries over to strength |
| **Stair climber** | LISS | 20-30 min steady | Moderate | Glute/leg endurance, low skill |

### How Much Cardio by Training Phase

| Phase | LISS Frequency | HIIT Frequency | Total Weekly Cardio | Notes |
|-------|----------------|----------------|---------------------|-------|
| **Bulk** | 1-2x per week | 0-1x per week | 30-90 min | Minimal — focus on lifting. Just enough for heart health. |
| **Cut** | 3-4x per week | 1-2x per week | 150-250 min | Moderate — increase calorie deficit. Preserve muscle with resistance training. |
| **Maintenance** | 2-3x per week | 1x per week | 90-150 min | Health-focused. Balanced approach. |

**Rule of thumb:** Start with minimal cardio. Add 1 session per week if fat loss stalls. Don't jump straight to 6 days of cardio.

## Nutrition Basics (Principles Only)

GymBro isn't a nutrition app. But the AI coach needs to understand nutrition principles to give context-aware advice.

### Caloric Surplus for Muscle Gain (Bulking)

**Target:** +300-500 calories above maintenance

**Why this range:**
- Too small (<200): Might not be enough to support muscle growth
- Too large (>700): Excessive fat gain, diminishing returns

**How to find maintenance:** Track current intake for 1 week while weight is stable. That's your baseline.

**Rate of gain:** 0.5-1% bodyweight per month (0.5-1 lb per week for 150 lb person)

**Macros:**
- Protein: 1.6-2.2g per kg (0.8-1g per lb)
- Fat: 0.8-1g per kg (0.4-0.5g per lb) — needed for hormone production
- Carbs: Fill the rest (fuel for training)

**AI coach scope:** Inform user of principles, recommend they track intake, but DON'T prescribe specific meal plans.

### Caloric Deficit for Fat Loss (Cutting)

**Target:** -300-500 calories below maintenance

**Why this range:**
- Too small (<200): Fat loss too slow, user gets discouraged
- Too large (>700): Muscle loss risk, metabolic adaptation, unsustainable

**Rate of loss:** 0.5-1% bodyweight per week (faster for beginners with more fat, slower for lean individuals)

**Macros:**
- Protein: INCREASE to 2.0-2.4g per kg (muscle preservation in deficit)
- Fat: Minimum 0.6-0.8g per kg (don't crash hormones)
- Carbs: Reduce to create deficit (carbs are most flexible macro)

**Adjustments:** If fat loss stalls for 2+ weeks, reduce calories by 100-200 OR add 1 cardio session. Don't slash calories dramatically.

### Protein Target: The One Non-Negotiable

**Target:** 1.6-2.2g per kg bodyweight (0.8-1g per lb)

**Why it matters:**
- Muscle protein synthesis (building muscle)
- Muscle protein breakdown prevention (keeping muscle)
- Satiety (protein is most filling macro)

**Practical:**
- 180 lb person = 145-180g protein per day
- Spread across 3-5 meals (20-40g per meal)

**Sources (for user education, not prescription):**
- Chicken, turkey, lean beef, fish
- Eggs, Greek yogurt, cottage cheese
- Protein powder (whey, casein, plant-based)
- Legumes, tofu (for vegetarians)

**AI coach role:** If user asks "Am I eating enough protein?" → Guide them to calculate based on bodyweight. DON'T create meal plans.

### Why Nutrition Matters More Than Training for Body Composition

**Truth:** You can't out-train a bad diet.

**Example:**
- 1 hour of hard lifting = ~200-400 calories burned
- 1 donut = ~300 calories
- It takes 5 minutes to eat what takes 1 hour to burn

**For fat loss:** 80% nutrition, 20% training
**For muscle gain:** 60% nutrition, 40% training (training provides stimulus, food provides building blocks)

**AI coach message:** "Your training is on point. If you're not seeing results, look at your nutrition first."

### GymBro Scope: Inform, Don't Prescribe

**What the AI coach CAN do:**
✅ Explain calorie surplus/deficit principles
✅ Provide protein targets based on bodyweight
✅ Suggest tracking food for 1 week to establish baseline
✅ Explain why nutrition matters
✅ Recommend consulting a nutritionist for specific plans

**What the AI coach CANNOT do:**
❌ Create meal plans
❌ Recommend specific foods (that's prescribing)
❌ Calculate exact macros without user input
❌ Diagnose eating disorders
❌ Replace professional nutritionist advice

**Template response:** "Based on your goals, a caloric surplus of 300-500 calories would support muscle growth. I recommend tracking your current intake to find your baseline, then adding 300-500 to that. For specific meal planning, consider consulting a registered dietitian."

## Injury Prevention & Common Issues

Smart training avoids injuries. But when issues arise, the AI coach should guide users to safe alternatives and flag when to seek professional help.

### Shoulder Impingement

**Symptoms:** Pain in front/top of shoulder during pressing or overhead movements

**Common causes:**
- Behind-the-neck pressing (presses bar behind head)
- Excessive internal rotation (elbows flared too wide on bench)
- Weak rotator cuff (stabilizers can't support load)
- Poor scapular control

**Fixes:**
- STOP: Behind-the-neck press, wide-grip bench press
- REDUCE: Overhead pressing volume temporarily
- SWITCH TO: Neutral grip pressing (dumbbell press, Swiss bar), floor press (limits ROM)
- STRENGTHEN: Face pulls, band pull-aparts, external rotations (rotator cuff work)

**Alternative exercises:**
- Overhead press → Landmine press (safer shoulder angle)
- Barbell bench → Dumbbell bench (natural arm path)
- Add: Face pulls 3x15 EVERY upper day

**When to see a professional:** Pain lasts >2 weeks despite modifications, pain at rest, loss of ROM

### Lower Back Strain

**Symptoms:** Pain in lumbar spine (lower back), worse after deadlifts/squats

**Common causes:**
- Rounded back during hinges (flexion under load)
- Weak core (can't brace properly)
- Hyperextension at lockout (overarching lower back)
- Too much volume too soon

**Fixes:**
- STOP: Heavy deadlifts temporarily
- REDUCE: Squat/deadlift volume by 40-50%
- FOCUS ON: Core bracing (learn to brace BEFORE lifting)
- USE: Lifting belt for heavy sets (gives feedback for bracing)
- STRENGTHEN: Planks, dead bugs, bird dogs (anti-extension core)

**Alternative exercises:**
- Conventional deadlift → Trap bar deadlift (more upright torso, easier to maintain neutral spine)
- Barbell squat → Goblet squat (counterbalance keeps torso upright)
- Add: McGill Big 3 (curl-up, side plank, bird dog) daily

**Form cue:** "Brace like someone's about to punch you in the stomach" before every rep

**When to see a professional:** Shooting pain down legs (sciatica), pain at rest, loss of bowel/bladder control (emergency)

### Knee Pain

**Symptoms:** Pain around kneecap or inside knee during squats/lunges

**Common causes:**
- Knees caving in (valgus collapse — poor glute activation)
- Squatting too deep for current mobility (forcing depth)
- Weak VMO (inner quad muscle)
- Quadriceps tendinitis (overuse)

**Fixes:**
- CHECK: Squat stance (feet shoulder-width, toes slightly out)
- CUE: "Knees out" or "spread the floor" (activate glutes)
- REDUCE: Squat depth to pain-free range (partial squats okay temporarily)
- STRENGTHEN: Terminal knee extensions, Spanish squats, Peterson step-ups (VMO focus)

**Alternative exercises:**
- Barbell squat → Box squat (controls depth, reduces knee flexion)
- Lunges → Split squats (more stable, easier to control knee position)
- Add: Glute activation (clamshells, glute bridges) before leg days

**When to see a professional:** Swelling, locking/catching sensation, pain walking up stairs, pain lasts >3 weeks

### Wrist Pain

**Symptoms:** Pain in wrist during pressing movements (bench, OHP)

**Common causes:**
- Excessive wrist extension (bar sitting in palm instead of base of hand)
- Weak wrist stabilizers
- Overuse (too much pressing volume)

**Fixes:**
- CHECK: Bar position (should sit over forearm bones, not palm)
- USE: Wrist wraps for heavy pressing (provides stability)
- SWITCH TO: Neutral grip pressing (Swiss bar, dumbbells)
- STRENGTHEN: Wrist curls, reverse wrist curls

**Alternative exercises:**
- Barbell bench → Dumbbell bench (neutral grip option)
- Barbell OHP → Dumbbell OHP (easier to find comfortable wrist angle)
- Push-ups → Use push-up handles (keeps wrist neutral)

**When to see a professional:** Persistent pain despite modifications, numbness/tingling (could be carpal tunnel)

### When to Stop Immediately

**Sharp pain = STOP** (not soreness, SHARP stabbing pain)

**Rule of thumb:**
- Soreness (delayed onset muscle soreness / DOMS): Normal, peaks 24-48 hours after workout
- Discomfort during lift: Manageable, part of hard training
- Sharp pain during lift: STOP immediately, assess
- Pain that doesn't go away with rest: See professional

**AI coach protocol:**
1. User reports pain → Ask: "Is it sharp pain or muscle soreness?"
2. If sharp → "Stop that exercise immediately. Try [alternative exercise]. If pain persists, see a doctor."
3. If soreness → "That's normal DOMS. Active recovery and light movement can help."

### Alternative Exercises by Injury Type

Quick reference for common exercise swaps when dealing with injuries:

| Injury/Issue | Avoid | Try Instead |
|--------------|-------|-------------|
| **Shoulder pain** | Overhead press, behind-neck press, wide-grip bench | Landmine press, neutral grip DB press, floor press |
| **Lower back pain** | Deadlift, back squat, good morning | Trap bar deadlift, goblet squat, leg press, belt squats |
| **Knee pain** | Deep squats, lunges, leg extensions | Box squats (controlled depth), split squats, leg press (partial ROM) |
| **Wrist pain** | Barbell bench, barbell curls | Dumbbell bench (neutral grip), cable curls, hammer curls |
| **Elbow pain** | Barbell curls, close-grip bench, skull crushers | Hammer curls, neutral grip press, overhead cable extensions |
| **Hip pain** | Deep squats, conventional deadlift | Box squats, sumo deadlift, goblet squats |

**Movement pattern reference:** Use the movement pattern table earlier in this skill to find same-pattern alternatives.

## User Profile Adaptation Matrix

This decision table guides workout recommendations based on user characteristics. AI coach and plan generator should reference this.

| User Profile | Frequency | Phase | Recommended Split | Volume/Session | Conditioning | Notes |
|-------------|-----------|-------|-------------------|---------------|-------------|-------|
| **Beginner, 2x/week, general fitness** | 2x | Maintenance | Full Body A/B | 12-16 sets | 1x LISS | Focus on compound movements, learn form |
| **Beginner, 3x/week, muscle gain** | 3x | Bulk | Full Body A/B/C | 15-20 sets | Minimal | Linear progression, same lifts each week |
| **Beginner, 3x/week, fat loss** | 3x | Cut | Full Body A/B/C | 12-16 sets | 2x LISS | Maintain strength, add cardio for deficit |
| **Intermediate, 4x/week, strength** | 4x | Bulk | Upper/Lower | 16-20 sets | 1x LISS | DUP or block periodization, main lifts focus |
| **Intermediate, 4x/week, muscle gain** | 4x | Bulk | Upper/Lower or PHUL | 18-24 sets | 1x LISS | Higher volume, mix compounds + isolation |
| **Intermediate, 4x/week, fat loss** | 4x | Cut | Upper/Lower | 14-18 sets | 2x LISS + 1x HIIT | Preserve strength, increase conditioning |
| **Intermediate, 5x/week, muscle gain** | 5x | Bulk | PPLUL | 20-25 sets | 1x LISS | High volume, bodybuilding style |
| **Advanced, 6x/week, hypertrophy** | 6x | Bulk | PPL x2 | 20-28 sets | 1x LISS | Max volume, needs excellent recovery |
| **Advanced, 5x/week, strength** | 5x | Bulk | Conjugate or DUP | 16-22 sets | 1x LISS | Periodization critical, autoregulation |
| **Advanced, 5x/week, fat loss** | 5x | Cut | PPL + UL OR Upper/Lower/Upper/Lower/Full | 16-20 sets | 3x LISS + 1x HIIT | Strength maintenance, high conditioning |
| **Beginner, 2x/week, fat loss** | 2x | Cut | Full Body A/B | 12-16 sets | 2x LISS | Build habit, add cardio separate from lifting |
| **Intermediate, 3x/week, strength** | 3x | Bulk | Full Body or Upper/Lower/Full | 16-20 sets | Minimal | Main lifts focus, compounds prioritized |
| **Advanced, 4x/week, strength (peak)** | 4x | Peaking | Upper Power/Lower Power/Upper/Lower | 10-14 sets | Minimal | Specificity phase, low volume, high intensity |
| **Any level, 6x/week, cut** | 6x | Cut | PPL x2 | 14-18 sets | 3-4x LISS + 1-2x HIIT | High frequency, reduced volume per session |

**How to use this matrix:**
1. Identify user profile from onboarding (experience level, weekly frequency, primary goal)
2. Find matching row in table
3. Use recommended split, volume, and conditioning as starting point
4. Adjust based on individual feedback and progress

## AI Coach Response Templates

How the AI coach should respond based on different user contexts and questions.

### User Plateauing (No Progress for 2+ Weeks)

**Diagnosis checklist (ask user these questions):**

1. **Nutrition check:** "Are you eating enough to support your goals?"
   - If bulking: Ensure caloric surplus
   - If cutting: Plateau is expected, adjust expectations
   - If maintaining: Might need slight surplus to progress

2. **Sleep check:** "How's your sleep been? Getting 7-9 hours consistently?"
   - Poor sleep = primary recovery limiter
   - If sleep is poor, address that BEFORE changing program

3. **Deload check:** "When was your last deload week?"
   - If 6+ weeks ago → "Time for a deload. Drop intensity to 60%, reduce volume by half, take a week."
   - If recent deload didn't help → Move to next check

4. **Program duration check:** "How long have you been on this program?"
   - If <8 weeks → "Stick with it. Progress isn't always linear."
   - If 8-16 weeks → "Might be time to change rep ranges or add variation."
   - If >16 weeks → "Probably time for a new program stimulus."

**Response template:**
```
"It sounds like you've stalled on [exercise]. Let's troubleshoot:

1. Are you eating enough? If you're in a caloric deficit, strength gains are harder. Consider eating at maintenance or slight surplus.
2. How's your sleep? Aim for 7-9 hours per night — recovery happens during sleep.
3. When did you last take a deload week? If it's been 6+ weeks, take a deload (50-60% intensity, half the volume).
4. If nutrition, sleep, and recovery are dialed in, it might be time to change your program or rep scheme.

Let me know what you find, and we can adjust from there."
```

### User Asking About Supplements

**Evidence-based only approach:**

**Tier 1 (Proven, recommend):**
- **Creatine monohydrate:** 5g per day, improves strength and power output, most researched supplement
- **Protein powder:** Convenience tool to hit daily protein target (1.6-2.2g per kg bodyweight)
- **Caffeine:** 3-6 mg per kg bodyweight pre-workout, improves focus and performance

**Tier 2 (Marginal benefit, user's choice):**
- **Beta-alanine:** May help with muscular endurance (15+ rep sets)
- **Citrulline malate:** May improve pump and blood flow
- **Fish oil:** General health, anti-inflammatory (if not eating fatty fish)

**Tier 3 (Weak evidence or unnecessary):**
- BCAAs (unnecessary if eating enough protein)
- Testosterone boosters (don't work unless you have clinical deficiency)
- Fat burners (mostly caffeine + marketing)

**Response template:**
```
"For supplements, focus on the basics that actually have evidence:

1. **Creatine monohydrate** (5g/day) — most researched, proven to work for strength and power.
2. **Protein powder** — just a convenient way to hit your daily protein target. Not magic, just food.
3. **Caffeine** (pre-workout) — improves focus and performance. Coffee works just as well as expensive pre-workout.

Everything else is marginal at best. Don't waste money on supplements if your nutrition, sleep, and training aren't dialed in first. Those are the foundations."
```

### User Asking About Form

**Protocol:**
1. Provide 3 key cues (not overwhelming)
2. Suggest video resource (YouTube link if available in exercise database)
3. Recommend starting light to practice

**Response template:**
```
"For [exercise], focus on these 3 cues:

1. [Primary cue — e.g., "Chest up, core braced"]
2. [Secondary cue — e.g., "Bar path straight down to lower chest"]
3. [Tertiary cue — e.g., "Push through heels, full lockout at top"]

I recommend watching this video: [YouTube link from exercise database]

Start with light weight (50-60% of your max) and practice these cues for 2-3 sessions. Once form is solid, add weight progressively. Better to build good habits now than reinforce bad ones."
```

### User in Cut Phase Asking About Strength Loss

**Set realistic expectations:**

**Response template:**
```
"It's totally normal for strength to plateau or even decline slightly during a cut. You're in a caloric deficit — your body doesn't have surplus energy to build new strength.

**Your goal during a cut:** MAINTAIN strength, not gain it.

Here's what to do:
1. Keep lifting heavy (80-85% of your max) — this signals your body to keep muscle.
2. Don't drop volume too much (reduce by 10-20% max).
3. Prioritize protein (2.0-2.4g per kg bodyweight) — protects muscle in a deficit.
4. Accept that progress will be slower. The wins are: losing fat, keeping muscle, maintaining strength.

Once you return to maintenance or a surplus, your strength will come back quickly. You're not losing gains — you're revealing them."
```

### User Just Starting (First Workout)

**Encourage consistency over intensity:**

**Response template:**
```
"Welcome to your fitness journey! Here's what matters most in your first 3 months:

1. **Show up consistently.** 3 days per week is the sweet spot. Even if a session isn't perfect, showing up builds the habit.
2. **Focus on form over weight.** Start light (just the bar if needed). Learn the movements correctly. You'll have YEARS to add weight.
3. **Don't compare yourself to others.** Everyone started somewhere. The person squatting 315 lbs started with the empty bar too.
4. **Soreness is normal.** You'll be sore after your first few sessions (DOMS — delayed onset muscle soreness). It goes away as your body adapts.

Your first goal: Complete 12 workouts in the next 4 weeks. Build the habit. We'll worry about PRs later.

You've got this. Let's get started."
```

### User Reporting Injury or Pain

**Safety-first protocol:**

**Response template (for sharp pain):**
```
"Sharp pain is a red flag. STOP doing [exercise] immediately.

Here's what to do:
1. Rest that movement for 3-5 days.
2. Try this alternative: [suggest movement-pattern equivalent that avoids injured area]
3. If pain persists for more than 2 weeks, or if you have swelling/loss of range of motion, see a doctor or physical therapist.

Your health is more important than any single workout. Let's find a way to train around this while it heals."
```

**Response template (for DOMS soreness):**
```
"That sounds like DOMS (delayed onset muscle soreness) — totally normal, especially if you're new or just increased intensity.

DOMS peaks 24-48 hours after a workout and goes away on its own. To help:
1. **Active recovery:** Light walking, easy movement (increases blood flow)
2. **Stay hydrated**
3. **Keep training** (yes, really — movement helps flush out soreness)

It gets better as your body adapts. Within 2-3 weeks of consistent training, DOMS will be much less intense."
```

## References

- **Programming:** Stronger by Science, Renaissance Periodization, Starting Strength
- **Calisthenics:** Overcoming Gravity (Steven Low), Convict Conditioning progressions
- **Movement patterns:** Functional Movement Systems (FMS), Athletic Body in Balance
- **Exercise library standards:** ExRx.net, ACE Exercise Library, NASM Exercise Database
- **Periodization:** Practical Programming for Strength Training (Rippetoe & Baker), The Science and Practice of Strength Training (Zatsiorsky)
- **Nutrition for lifters:** Renaissance Periodization Diet Templates, Stronger by Science Nutrition Guide
- **Injury prevention:** Becoming a Supple Leopard (Kelly Starrett), Clinical Sports Medicine (Brukner & Khan)
