# GymBro AI/ML Technical Approach
**Version:** 1.0  
**Last Updated:** 2026  
**Owner:** Neo (AI/ML Engineer)

---

## Executive Summary

GymBro's AI/ML architecture is designed for **2026-2027 iOS deployment** with a privacy-first, on-device approach leveraging Apple's Core AI framework and Apple Intelligence capabilities. The system combines heuristic-driven training logic with targeted machine learning for progress analytics and recovery modeling. Our philosophy: **the best AI is invisible**—it makes the right call at the right time without over-engineering.

**Key Design Principles:**
- **Privacy-first:** All sensitive data stays on-device using Core ML/Core AI
- **Explainability:** Every AI decision must be transparent to users
- **Adaptive intelligence:** Real-time adjustments based on performance, recovery, and context
- **Simplicity over complexity:** Well-tuned heuristics before complex ML when appropriate

---

## 1. AI Coach Chat Design

### 1.1 Natural Language Workout Planning

The AI Coach is a conversational interface that helps users plan workouts, adjust training variables, and understand their progress. It operates as a **domain-specific assistant** with deep knowledge of:
- Exercise science and periodization principles
- User's training history, current program, and goals
- Recovery status and readiness indicators
- Form cues and technique guidance

**Example Interactions:**

```
User: "My lower back felt sketchy on deadlifts today"
Coach: "Got it. I see you hit 405x5 last session and attempted 415 today. 
       Let's drop to 385 next time and focus on bracing. I'm also noticing 
       your HRV is 15% below baseline—consider an extra rest day this week."

User: "Should I add more chest volume?"
Coach: "Your bench is progressing well (e1RM up 8% this month). Current 
       volume: 12 sets/week. Research suggests 12-20 sets optimal for 
       hypertrophy. Let's add one set to incline and see how you recover."

User: "Generate a hypertrophy block for the next 6 weeks"
Coach: "Based on your strength foundation (bench 285, squat 405, deadlift 455):
       - 4-day upper/lower split
       - 15-20 sets per muscle group per week
       - Progressive overload via sets/reps (keep intensity at 70-80% 1RM)
       - Deload week 4 to manage fatigue. Want me to detail day 1?"
```

### 1.2 On-Device vs Cloud LLM Trade-offs (2026-2027)

**Recommendation: Hybrid approach with on-device primary**

| Aspect | On-Device (Core AI) | Cloud (Private Cloud Compute) |
|--------|---------------------|-------------------------------|
| **Use Cases** | - Quick workout adjustments<br>- Form cues<br>- Program modifications<br>- Progress summaries | - Complex program generation<br>- Long-form periodization plans<br>- Multi-variable optimization |
| **Latency** | <100ms response time | 300-800ms typical |
| **Privacy** | 100% local, no data leaves device | Ephemeral, auditable, no logging |
| **Context Window** | ~8K tokens (3B param model) | ~32K+ tokens (larger models) |
| **Cost** | Free, battery efficient | Pay-per-use (minimal cost) |
| **Availability** | Works offline | Requires internet |

**Implementation Strategy:**
1. **Default to on-device** for 90% of interactions (quick Q&A, adjustments, insights)
2. **Escalate to PCC** for:
   - Full 8-12 week program generation
   - Multi-exercise periodization with complex constraints
   - Detailed biomechanical analysis requests
3. **Seamless handoff:** User never knows where processing happens

**Apple Intelligence Integration:**
- Use Apple's Foundation Models framework for conversational chat
- Leverage system-wide context (HealthKit data, calendar for scheduling)
- Siri integration: "Hey Siri, ask GymBro if I should train today"
- On-device summarization for workout history and trends

### 1.3 Context Management: How the Coach "Knows" You

**Context Architecture:**

```
┌─────────────────────────────────────────┐
│         Context Window (8K tokens)       │
├─────────────────────────────────────────┤
│ 1. User Profile (200 tokens)            │
│    - Goals, experience level, injuries  │
│    - Current program type & phase       │
├─────────────────────────────────────────┤
│ 2. Recent Training (3K tokens)          │
│    - Last 7 days of workouts            │
│    - PR history (last 30 days)          │
│    - Volume trends (weekly tonnage)     │
├─────────────────────────────────────────┤
│ 3. Recovery Status (500 tokens)         │
│    - HRV, sleep, resting HR (7-day avg) │
│    - Readiness score & trend            │
│    - Subjective fatigue signals         │
├─────────────────────────────────────────┤
│ 4. Current Conversation (3K tokens)     │
│    - Last 5-10 message exchanges        │
│    - Active workout session (if any)    │
├─────────────────────────────────────────┤
│ 5. Domain Knowledge (1.3K tokens)       │
│    - Relevant exercise science snippets │
│    - Periodization phase rules          │
└─────────────────────────────────────────┘
```

**Smart Context Retrieval:**
- **Semantic search** over workout history (Core ML embeddings)
- **Priority scoring:** Recent workouts > old PRs, current injuries > past issues
- **Adaptive compression:** Older data summarized (e.g., "August: +12lb bench, deload week 3")

**Context Storage:**
- SQLite database (on-device) with vector embeddings for semantic retrieval
- Core Data for structured workout logs
- HealthKit as source of truth for biometric data

### 1.4 Technical Stack

**LLM Layer:**
- **Primary:** Apple Foundation Models (on-device 3B param)
- **Secondary:** Private Cloud Compute for complex tasks
- **Fallback:** Deterministic rule engine if LLM unavailable

**Prompt Engineering:**
- System prompt defines coach personality: knowledgeable, direct, skeptical of bro-science
- Few-shot examples for workout modifications and exercise substitutions
- Structured output format for program generation (JSON schema)

**Safety & Validation:**
- Output parsing to validate exercise names, set/rep schemes
- Range checking on weights/reps (flag unrealistic suggestions)
- Injury risk warnings triggered by context (e.g., high-frequency deadlifts + low HRV)

---

## 2. Adaptive Training Engine

### 2.1 Auto-Periodization Approach

**Model: Hybrid Block + Daily Undulating Periodization (DUP)**

Modern strength training research supports flexible periodization that adapts to individual response. GymBro implements:

**Block Periodization Structure (Macro Level):**
```
Accumulation (4-6 weeks)
├─ Focus: Volume, hypertrophy, work capacity
├─ Intensity: 65-75% 1RM
├─ Volume: High (15-25 sets per muscle group/week)
└─ Goal: Build base, muscle growth

Intensification (3-4 weeks)
├─ Focus: Strength, neural adaptation
├─ Intensity: 80-90% 1RM
├─ Volume: Moderate (10-15 sets per muscle group/week)
└─ Goal: Convert size to strength

Realization (2-3 weeks)
├─ Focus: Peak strength, specificity
├─ Intensity: 90-97% 1RM
├─ Volume: Low (6-10 sets per muscle group/week)
└─ Goal: Achieve PRs, test maxes

Deload (1 week)
├─ Volume: -40-50%
├─ Intensity: Maintained or -10%
└─ Goal: Recovery, supercompensation
```

**Daily Undulating Variation (Micro Level):**
- Within each block, vary rep ranges and intensity across sessions
- Example accumulation week:
  - Day 1: 3x8-10 @ 70%
  - Day 2: 4x6-8 @ 75%
  - Day 3: 3x12-15 @ 65%

**Why Hybrid?**
- Block periodization: Proven for long-term strength gains
- DUP: Reduces monotony, allows higher frequency, matches varied recovery
- Flexibility: Each block can extend/shorten based on progress metrics

### 2.2 Load Progression Algorithms

**Decision Logic: Multi-Factor Progression Model**

```python
def decide_weight_progression(exercise, last_session, user_context):
    """
    Determines next session's weight using multi-factor analysis
    """
    factors = {
        'performance': analyze_performance(last_session),
        'recovery': user_context.readiness_score,
        'volume_accumulation': calculate_fatigue_index(exercise, 7),
        'phase': user_context.current_training_phase
    }
    
    # Performance scoring
    if last_session.rpe <= 7 and last_session.completed_all_reps:
        performance_signal = 'EASY'  # Add weight
    elif last_session.rpe >= 9 or last_session.failed_reps > 0:
        performance_signal = 'HARD'  # Maintain or reduce
    else:
        performance_signal = 'APPROPRIATE'  # Maintain or small increase
    
    # Recovery gating
    if factors['recovery'] < 60:  # Low readiness
        return 'MAINTAIN_OR_REDUCE'
    
    # Progression rules by phase
    if factors['phase'] == 'accumulation':
        if performance_signal == 'EASY':
            return increment_weight(exercise, 'SMALL')  # 2.5-5lb
        elif factors['volume_accumulation'] > 0.7:  # High fatigue
            return 'MAINTAIN'
    
    elif factors['phase'] == 'intensification':
        if performance_signal == 'EASY' and factors['recovery'] > 70:
            return increment_weight(exercise, 'MODERATE')  # 5-10lb
    
    return 'MAINTAIN'  # Conservative default
```

**Progression Increments:**
- **Upper body:** 2.5-5 lbs per jump
- **Lower body:** 5-10 lbs per jump
- **Accessories:** Reps first, then weight (double progression)

**Adaptive Mechanisms:**
- **Successful session (RPE 7-8, all reps):** Progress next session
- **Failed set:** Reduce weight 10% and rebuild
- **Two consecutive easy sessions:** Accelerate progression
- **Plateau (3 weeks no progress):** Trigger phase transition or deload

### 2.3 Deload Detection and Triggers

**Automated Deload Triggers:**

1. **Volume Fatigue Index > 0.75**
   - Rolling 7-day volume significantly exceeds 14-day average
   - Formula: `acute_load / chronic_load ratio > 1.3`

2. **Performance Plateau:**
   - No e1RM improvement for 3+ weeks
   - AND subjective fatigue reported

3. **Recovery Decline:**
   - Readiness score < 50 for 3+ consecutive days
   - OR HRV < 20% below 30-day baseline

4. **Manual User Signal:**
   - User reports persistent soreness, joint pain, or motivation drop

5. **Scheduled Deload:**
   - Every 4-6 weeks regardless (proactive fatigue management)

**Deload Implementation:**
- Reduce volume by 40-50% (fewer sets)
- Maintain intensity (same weights) OR reduce 10% and maintain sets
- Focus on technique, mobility, weak points
- Duration: 4-7 days depending on severity

### 2.4 Training Goal Adaptations

**Goal-Specific Modifications:**

| Goal | Primary Focus | Volume | Intensity | Rep Ranges | Frequency |
|------|--------------|--------|-----------|------------|-----------|
| **Strength** | Neural efficiency, max force | Moderate | High (80-95%) | 1-6 | 3-5x/week per lift |
| **Hypertrophy** | Mechanical tension, metabolic stress | High | Moderate (65-85%) | 6-15 | 2-3x/week per muscle |
| **Endurance** | Work capacity, lactate threshold | Very High | Low-Moderate (55-75%) | 12-20+ | 2-3x/week |
| **General Fitness** | Balanced development | Moderate | Moderate (70-80%) | 6-12 | 2-4x/week |

**Algorithm adjusts:**
- Set/rep prescriptions
- Rest intervals (2-5min strength, 1-2min hypertrophy, 30-90s endurance)
- Exercise selection (compounds vs accessories ratio)
- Periodization emphasis (longer accumulation for hypertrophy, frequent peaks for strength)

### 2.5 State Machine Design

```
┌─────────────────────────────────────────────────────┐
│              TRAINING STATE MACHINE                 │
└─────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  ONBOARDING  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │ PROGRESSION  │◄────────────┐
    │ (Accumulation│             │
    │      or      │             │
    │Intensification)│           │
    └──────┬───────┘             │
           │                     │
           │ Trigger:            │
           │ - Fatigue high      │
           │ - Plateau           │
           │ - Scheduled         │
           │                     │
           ▼                     │
    ┌──────────────┐             │
    │   DELOAD     │             │
    └──────┬───────┘             │
           │                     │
           │ After 4-7 days      │
           │                     │
           ▼                     │
    ┌──────────────┐             │
    │  RECOVERY    │             │
    │  (Testing    │             │
    │   Readiness) │             │
    └──────┬───────┘             │
           │                     │
           │ Readiness > 70      │
           │                     │
           └─────────────────────┘

           ┌──────────────┐
    ┌─────▶│  MAINTENANCE │◄─────┐
    │      │ (User paused │      │
    │      │  or traveling)│     │
    │      └──────┬───────┘      │
    │             │              │
    │    Resume   │   Continue   │
    │             │   maintaining│
    └─────────────┴──────────────┘
```

**State Transitions:**
- **PROGRESSION → DELOAD:** Auto-triggered by fatigue/plateau indicators
- **DELOAD → RECOVERY:** Fixed duration (4-7 days)
- **RECOVERY → PROGRESSION:** When readiness restored (score > 70)
- **ANY → MAINTENANCE:** User-initiated (vacation, injury, life chaos)
- **MAINTENANCE → PROGRESSION:** User-initiated return (with potential re-onboarding)

**State Persistence:**
- Current state stored in Core Data
- Transition history logged for analysis
- Coach chat aware of state for contextual guidance

---

## 3. Progress Analytics & Plateau Detection

### 3.1 Key Metrics for Serious Lifters

**Primary Metrics (Real-time calculated):**

1. **Estimated 1RM (e1RM):**
   - Formula: Brzycki for <10 reps, Epley for <6 reps
   - Brzycki: `e1RM = weight / (1.0278 - 0.0278 × reps)`
   - Tracked per compound lift (squat, bench, deadlift, OHP)
   - 7-day, 30-day, 90-day trends displayed

2. **Volume Load (Tonnage):**
   - `Total tonnage = Σ(weight × reps × sets)` per session, week, month
   - Broken down by muscle group and movement pattern
   - Used for fatigue monitoring and progression validation

3. **Strength Curve (Load-Velocity Profile):**
   - For users with velocity tracking (future feature)
   - Predict 1RM from bar speed at submaximal loads
   - More accurate than rep-based e1RM

4. **Relative Strength:**
   - `Wilks Score = Total (Squat + Bench + Deadlift) × Wilks Coefficient`
   - Allows comparison across bodyweights
   - Tracked over time to assess true strength gain vs body mass change

5. **Volume Landmarks:**
   - Total reps per muscle group per week
   - Total sets per muscle group per week
   - Compared to evidence-based ranges (e.g., 10-20 sets per muscle group)

6. **Frequency Metrics:**
   - Sessions per week per lift
   - Days between training same muscle group

**Secondary Metrics (Insight generation):**
- **Time Under Tension (TUT):** Estimated from tempo prescriptions
- **Intensity Distribution:** % of sets at <70%, 70-85%, >85% 1RM
- **Movement Balance:** Push/pull ratio, squat/deadlift balance
- **Weak Point Analysis:** Sticking points in lifts (requires video analysis - future)

### 3.2 Plateau Detection Algorithms

**Multi-Method Approach:**

**Method 1: Time Series Forecasting (Prophet-inspired)**
```python
def detect_plateau_via_forecast(exercise_history, lookahead_days=30):
    """
    Uses local time series model to predict e1RM trajectory
    Plateau = predicted slope near zero
    """
    df = prepare_timeseries(exercise_history)  # Date, e1RM
    
    # Fit lightweight local model (polynomial regression + trend decomposition)
    model = fit_trend_model(df)
    future = model.predict(lookahead_days)
    
    # Calculate slope of predicted trend
    predicted_slope = (future[-1] - future[0]) / lookahead_days
    
    # Normalize by current e1RM
    relative_slope = predicted_slope / df['e1RM'].iloc[-1]
    
    if relative_slope < 0.001:  # <0.1% gain per day = plateau
        return {
            'status': 'PLATEAU_DETECTED',
            'confidence': model.confidence_interval,
            'recommendation': 'Consider phase change or deload'
        }
    return {'status': 'PROGRESSING'}
```

**Method 2: Statistical Change Point Detection**
```python
def detect_plateau_via_changepoint(exercise_history, window=21):
    """
    Identifies significant decrease in rate of progress
    """
    e1rm_series = [session.e1rm for session in exercise_history]
    
    # Calculate rolling rate of change (slope over window)
    rates = calculate_rolling_slope(e1rm_series, window)
    
    # Detect change point where slope drops below threshold
    recent_rate = rates[-7:]  # Last 7 sessions
    historical_rate = rates[-28:-7]  # Previous 21 sessions
    
    if mean(recent_rate) < 0.2 * mean(historical_rate):
        return 'SIGNIFICANT_SLOWDOWN'
    return 'NORMAL_VARIATION'
```

**Method 3: Rolling Average Stagnation**
```python
def detect_plateau_via_rolling_avg(exercise_history, window=10):
    """
    Simple but effective: Has e1RM increased in last 10 sessions?
    """
    recent = exercise_history[-window:]
    max_recent = max(session.e1rm for session in recent)
    max_historical = max(session.e1rm for session in exercise_history[:-window])
    
    if max_recent <= max_historical:
        sessions_since_pr = days_since_last_pr(exercise_history)
        if sessions_since_pr > 21:  # 3 weeks
            return 'PLATEAU'
    return 'PROGRESSING'
```

**Composite Plateau Score:**
- Combine all three methods with weights
- Prophet forecast: 40% (predictive)
- Change point: 35% (statistical rigor)
- Rolling average: 25% (intuitive, interpretable)
- Plateau declared if composite score > 0.65

**User Notification:**
```
"Your bench press hasn't increased in 3 weeks. This is normal—strength 
gains aren't linear. Options:
1. Deload this week (-40% volume) then return
2. Switch to hypertrophy focus for 4 weeks (build base)
3. Add variation (close-grip, paused reps) to break pattern
What feels right?"
```

### 3.3 Strength Curve Analysis

**Concept:**
- Strength doesn't increase linearly—it follows S-curves (rapid growth → plateau → breakthrough → new plateau)
- Analyze user's personal curve to set realistic expectations

**Implementation:**
```
┌─────────────────────────────────────────┐
│         Strength Progression Phases     │
├─────────────────────────────────────────┤
│ Novice Phase (0-6 months)               │
│ - Linear progression (~5-10lb/week)     │
│ - High sensitivity to training          │
├─────────────────────────────────────────┤
│ Intermediate Phase (6 months - 3 years) │
│ - Block periodization needed            │
│ - Progress slows (~5-10lb/month)        │
├─────────────────────────────────────────┤
│ Advanced Phase (3+ years)               │
│ - Micro-periodization critical          │
│ - Progress measured annually            │
└─────────────────────────────────────────┘
```

**Adaptive Expectations:**
- Calculate user's training age from history
- Adjust plateau thresholds accordingly:
  - Novice: Plateau if no progress in 2 weeks → likely form/recovery issue
  - Advanced: Plateau if no progress in 8 weeks → normal, adjust programming

### 3.4 Visual Insights & Recommendations

**Dashboard Visualizations:**

1. **e1RM Trend Chart:**
   - Line graph with 7-day moving average
   - Color-coded by training phase
   - Plateau zones highlighted in yellow
   - PR markers (stars)

2. **Volume Heatmap:**
   - Weekly volume per muscle group
   - Color intensity = tonnage
   - Identifies imbalances (e.g., high chest, low back volume)

3. **Intensity Distribution Pie Chart:**
   - % of sets in light/moderate/heavy zones
   - Target distributions shown based on current goal

4. **Recovery vs Performance Scatter:**
   - X-axis: Readiness score
   - Y-axis: Session performance (RPE-adjusted tonnage)
   - Shows correlation between recovery and output

**AI-Generated Insights (weekly):**
```
"This week's analysis:
✓ Squat e1RM up 3% to 385 lbs—great progress
⚠ Deadlift volume down 20%—is your lower back fatigued?
✓ Bench volume at 14 sets (optimal range)
→ Recommendation: Add one RDL variation to support deadlift"
```

**Insight Generation Engine:**
- Rule-based system with 50+ insight templates
- Triggered by thresholds (e.g., "volume drop >15%")
- Prioritized by relevance (recent data > old trends)
- Max 3-5 insights per week (avoid overload)

---

## 4. Recovery Modeling

### 4.1 HealthKit Signal Integration

**Data Sources:**

| HealthKit Type | Metric | Sampling | Use Case |
|----------------|--------|----------|----------|
| **HKCategoryTypeIdentifierSleepAnalysis** | Sleep duration, stages (deep, REM, core) | Nightly | Primary recovery indicator |
| **HKQuantityTypeIdentifierHeartRateVariabilitySDNN** | HRV (SDNN in ms) | Morning measurement | Autonomic nervous system balance |
| **HKQuantityTypeIdentifierRestingHeartRate** | Resting HR (bpm) | Morning or daily average | Stress/fatigue indicator |
| **HKQuantityTypeIdentifierActiveEnergyBurned** | Daily activity (kcal) | Continuous | Training load + NEAT |
| **HKWorkoutTypeStrengthTraining** | Workout sessions | Per session | Acute training load |
| **HKQuantityTypeIdentifierBodyMass** | Bodyweight (kg) | Daily or weekly | Trend analysis (cut/bulk) |

**Privacy & Permissions:**
- Request minimal necessary permissions on first launch
- Explain exactly how each metric is used
- All data processed on-device, never uploaded

**Data Pipeline:**
```
HealthKit → Core Data Cache → Feature Engineering → Recovery Model → Readiness Score
                                                                        ↓
                                                                User Dashboard
```

### 4.2 Readiness Score Calculation

**Algorithm: Weighted Multi-Factor Model**

```python
def calculate_readiness_score(user_data):
    """
    Returns 0-100 readiness score
    Higher = better recovery, ready to train hard
    """
    
    # 1. Sleep Score (35% weight)
    sleep_score = calculate_sleep_score(
        duration=user_data.sleep_duration,
        efficiency=user_data.sleep_efficiency,
        deep_sleep_pct=user_data.deep_sleep_percentage
    )
    
    # 2. HRV Score (30% weight)
    hrv_score = calculate_hrv_score(
        current_hrv=user_data.today_hrv,
        baseline_hrv=user_data.hrv_30day_avg,
        std_dev=user_data.hrv_std_dev
    )
    
    # 3. Resting HR Score (15% weight)
    rhr_score = calculate_rhr_score(
        current_rhr=user_data.today_rhr,
        baseline_rhr=user_data.rhr_30day_avg
    )
    
    # 4. Training Load Score (15% weight)
    load_score = calculate_load_score(
        acute_load=user_data.last_7day_load,
        chronic_load=user_data.last_28day_load
    )
    
    # 5. Subjective Input (5% weight, if provided)
    subjective_score = user_data.subjective_readiness or 70  # Default neutral
    
    # Weighted combination
    readiness = (
        sleep_score * 0.35 +
        hrv_score * 0.30 +
        rhr_score * 0.15 +
        load_score * 0.15 +
        subjective_score * 0.05
    )
    
    return clip(readiness, 0, 100)

def calculate_sleep_score(duration, efficiency, deep_sleep_pct):
    """Sleep sub-component (0-100)"""
    # Optimal: 7-9 hours, >85% efficiency, >20% deep sleep
    duration_score = gaussian_score(duration, optimal=8.0, sigma=1.0) * 50
    efficiency_score = linear_score(efficiency, min=70, optimal=90) * 30
    deep_score = linear_score(deep_sleep_pct, min=15, optimal=25) * 20
    return duration_score + efficiency_score + deep_score

def calculate_hrv_score(current_hrv, baseline_hrv, std_dev):
    """HRV sub-component (0-100)"""
    # HRV > baseline = good recovery
    # HRV < baseline - 1 SD = poor recovery
    z_score = (current_hrv - baseline_hrv) / std_dev
    return 50 + (z_score * 20)  # Maps -1.5 SD to 20, +1.5 SD to 80

def calculate_rhr_score(current_rhr, baseline_rhr):
    """Resting HR sub-component (0-100)"""
    # Lower RHR = better recovery
    delta = current_rhr - baseline_rhr
    if delta <= 0:
        return 90  # Below baseline is great
    elif delta <= 5:
        return 70  # Slightly elevated, acceptable
    elif delta <= 10:
        return 50  # Elevated, caution
    else:
        return 30  # Significantly elevated, poor recovery

def calculate_load_score(acute_load, chronic_load):
    """Training load ratio (0-100)"""
    # Acute:Chronic Workload Ratio (ACWR)
    # Optimal: 0.8-1.3
    # >1.5 = high injury risk, fatigue
    acwr = acute_load / chronic_load if chronic_load > 0 else 1.0
    
    if 0.8 <= acwr <= 1.3:
        return 90  # Optimal training stress
    elif acwr < 0.8:
        return 70  # Undertraining or deload
    elif acwr <= 1.5:
        return 60  # Moderate overreach
    else:
        return 30  # Excessive fatigue
```

**Score Interpretation:**
- **90-100:** Excellent recovery, ready for PR attempts or high-intensity work
- **70-89:** Good recovery, normal training intensity
- **50-69:** Moderate recovery, consider reducing volume or intensity
- **30-49:** Poor recovery, light session or rest recommended
- **0-29:** Critical fatigue, rest day mandatory

### 4.3 Recovery-Based Training Adjustments

**Adaptive Rules Engine:**

```python
def adjust_training_for_recovery(planned_session, readiness_score):
    """
    Modifies planned workout based on recovery status
    """
    if readiness_score >= 80:
        # Green light: proceed as planned or even push harder
        if planned_session.type == 'strength':
            return planned_session.with_note("Great recovery—go for a PR if you feel it!")
        return planned_session
    
    elif readiness_score >= 60:
        # Yellow: train but manage intensity
        if planned_session.intensity > 85:
            return planned_session.reduce_intensity(to=80).add_note(
                "Readiness is moderate. Dropped intensity 5% to protect recovery."
            )
        return planned_session
    
    elif readiness_score >= 40:
        # Orange: significant adjustment needed
        if planned_session.type == 'strength':
            return planned_session.reduce_volume(by=0.3).reduce_intensity(to=75).add_note(
                "Low readiness detected. Lighter session to avoid overtraining."
            )
        return planned_session.reduce_volume(by=0.5)
    
    else:
        # Red: rest or active recovery only
        return RestDay(
            suggestion="Your body needs rest. HRV and sleep indicate high fatigue. "
                      "Consider light walking, stretching, or full rest."
        )
```

**Real-World Example:**
```
Planned: Squat 5x3 @ 365 lbs (88% 1RM)
Readiness: 52 (low HRV, poor sleep)

Adjusted: Squat 4x3 @ 345 lbs (83% 1RM)
Reason: "Your HRV is 20% below baseline and you slept only 5.5 hours. 
         Let's hit quality reps at a lighter load to stay fresh."
```

### 4.4 Fatigue Accumulation Models

**Acute vs Chronic Training Load (ATL/CTL):**

Borrowed from endurance sports (TrainingPeaks model):

```
Acute Training Load (ATL) = 7-day exponentially weighted moving average of daily load
Chronic Training Load (CTL) = 28-day exponentially weighted moving average of daily load
Training Stress Balance (TSB) = CTL - ATL

TSB Interpretation:
- TSB > +10: Well-rested, potentially under-trained
- TSB -10 to +10: Optimal training stress
- TSB < -20: Accumulating fatigue, deload soon
- TSB < -30: Overreaching, high injury risk
```

**Daily Load Calculation for Strength Training:**

```python
def calculate_session_load(session):
    """
    Quantifies training stress from a single session
    Returns: Load score (arbitrary units)
    """
    total_volume = session.total_tonnage  # Sum of weight × reps × sets
    intensity_factor = session.avg_intensity / 70  # Normalize to 70% baseline
    rpe_factor = session.avg_rpe / 7  # Normalize to RPE 7 baseline
    
    # Base load from volume
    base_load = total_volume / 1000  # Convert to manageable scale
    
    # Adjust for intensity and RPE
    session_load = base_load * intensity_factor * rpe_factor
    
    return session_load
```

**Example:**
- Session: Squat 315x5x3, Bench 225x5x3, Accessories
- Total tonnage: 6,000 lbs
- Avg intensity: 80% 1RM
- Avg RPE: 8
- Session load = (6000 / 1000) × (80/70) × (8/7) ≈ 8.2 units

Track daily loads and calculate ATL/CTL to visualize fatigue:

```
Week 1: ATL=7, CTL=6, TSB=-1 (fresh, ramping up)
Week 3: ATL=10, CTL=9, TSB=-1 (optimal)
Week 5: ATL=12, CTL=10, TSB=-2 (slight accumulation)
Week 6: ATL=6, CTL=9, TSB=+3 (deload week, recovering)
```

**Visualization:**
- Line chart with ATL (blue), CTL (red), TSB (green/red zones)
- Deload automatically suggested when TSB < -25

---

## 5. Technical Stack for AI/ML

### 5.1 Core ML Model Types Needed

| Model Type | Use Case | Deployment | Training Data | Update Frequency |
|------------|----------|------------|---------------|------------------|
| **Transformer LLM (3B param)** | AI Coach chat, NL understanding | On-device (Core AI) | Apple Foundation Models (pre-trained) | N/A (using Apple's) |
| **Time Series Forecasting (Prophet-style)** | Plateau detection, e1RM prediction | On-device (Core ML) | User's workout history | Weekly per user |
| **Logistic Regression** | Readiness score calculation | On-device (Core ML) | Aggregated HealthKit data | Daily per user |
| **Embeddings Model (BERT-tiny)** | Semantic search over workout history | On-device (Core ML) | Exercise descriptions, user notes | One-time (pre-trained) |
| **Anomaly Detection (Isolation Forest)** | Injury risk signals, abnormal fatigue | On-device (Core ML) | Load, HRV, performance patterns | Monthly per user |

**Why These Models?**
- **LLM:** Handles conversational AI, no training needed (leverage Apple's)
- **Time series:** Lightweight, interpretable, perfect for trend analysis
- **Logistic regression:** Explainable, fast inference, low compute
- **Embeddings:** Enable semantic search without heavy compute
- **Anomaly detection:** Catches edge cases (sudden performance drop, injury precursors)

### 5.2 On-Device vs Server Split

**On-Device (95% of compute):**
- ✅ AI Coach chat (standard queries)
- ✅ Workout adjustments
- ✅ Readiness score calculation
- ✅ Progress analytics and visualizations
- ✅ Plateau detection
- ✅ Load progression decisions

**Server (Private Cloud Compute, 5% of compute):**
- ⚠️ Full program generation (8-12 weeks, complex constraints)
- ⚠️ Aggregated insights (comparing to anonymized population data)
- ⚠️ Advanced biomechanical analysis (if video integration added)

**Rationale:**
- On-device: Privacy, speed, offline capability, no recurring cost
- Server: Only when complexity exceeds on-device limits
- Seamless handoff: User never knows unless they check settings

### 5.3 Data Pipeline

```
┌─────────────────────────────────────────────────────────┐
│                   DATA FLOW ARCHITECTURE                │
└─────────────────────────────────────────────────────────┘

USER INPUT                          HEALTHKIT DATA
(Workouts, RPE, Notes)              (Sleep, HRV, RHR)
        │                                   │
        ▼                                   ▼
┌───────────────────────────────────────────────────────┐
│            Core Data (Local SQLite Database)          │
│  - WorkoutSession (sets, reps, weight, RPE)           │
│  - Exercise (name, type, muscle groups)               │
│  - UserProfile (goals, experience, injuries)          │
│  - RecoveryData (daily HRV, sleep, readiness)         │
└───────────────┬───────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────────────┐
│               FEATURE ENGINEERING                     │
│  - Calculate e1RM, volume, intensity                  │
│  - Aggregate weekly/monthly metrics                   │
│  - Compute ATL/CTL/TSB                                │
│  - Normalize HRV, sleep scores                        │
└───────────────┬───────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────────────┐
│                 CORE ML MODELS                        │
│  - Readiness Model → Readiness Score                  │
│  - Time Series Model → e1RM Forecast                  │
│  - Anomaly Detector → Risk Flags                      │
│  - Embeddings → Semantic Search Results              │
└───────────────┬───────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────────────┐
│              TRAINING ENGINE (Rules + ML)             │
│  - Progression logic                                  │
│  - Deload triggers                                    │
│  - Recovery adjustments                               │
└───────────────┬───────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────────────┐
│                  USER INTERFACE                       │
│  - Dashboard (charts, insights)                       │
│  - AI Coach Chat                                      │
│  - Workout Log (input)                                │
└───────────────────────────────────────────────────────┘
```

**Key Design Decisions:**
- **Core Data as single source of truth:** All workout and recovery data stored locally
- **Batch feature engineering:** Nightly compute (background tasks) to pre-calculate metrics
- **Model inference on-demand:** When user opens dashboard or chats with coach
- **No external API calls** for core functionality (except PCC for complex tasks)

### 5.4 Privacy Considerations

**Privacy-First Architecture:**

1. **All sensitive data on-device:**
   - Workout logs, body metrics, health data never leave the device
   - No analytics tracking of user behavior
   - No data sold or shared with third parties

2. **Private Cloud Compute (when needed):**
   - Ephemeral processing (data not stored)
   - End-to-end encrypted requests
   - Auditable by third-party security researchers
   - User can opt-out and use 100% on-device mode

3. **No user identification:**
   - App uses local-only user ID (UUID)
   - If optional cloud backup used (iCloud), encrypted and user-controlled

4. **Transparency:**
   - Settings show exactly what data is used for what purpose
   - "Data Dashboard" lets users export all their data (JSON or CSV)
   - Clear indicators when PCC is used vs on-device

5. **Compliance:**
   - GDPR compliant (data export, deletion requests)
   - HIPAA considerations (HealthKit data handled per Apple guidelines)
   - No ads, no trackers, no third-party SDKs (except Apple frameworks)

**User Control:**
- Toggle: "Strict On-Device Mode" (disables PCC entirely)
- Toggle: "Share anonymized insights" (opt-in for population benchmarks)
- Full data export and account deletion available in settings

---

## 6. Implementation Roadmap

### Phase 1: Foundation (Months 1-3)
- [x] Core Data schema for workouts, exercises, user profile
- [x] HealthKit integration (sleep, HRV, RHR)
- [ ] Basic readiness score calculation (rule-based)
- [ ] Manual workout logging UI
- [ ] Simple progression algorithm (linear for novices)

### Phase 2: Intelligence (Months 4-6)
- [ ] Core ML models: time series forecasting, embeddings
- [ ] Plateau detection algorithm
- [ ] Adaptive training engine (state machine)
- [ ] ATL/CTL fatigue modeling
- [ ] Progress analytics dashboard

### Phase 3: AI Coach (Months 7-9)
- [ ] Apple Foundation Models integration
- [ ] Context management system
- [ ] Conversational UI for workout planning
- [ ] Recovery-based training adjustments
- [ ] Insight generation engine

### Phase 4: Refinement (Months 10-12)
- [ ] Private Cloud Compute for complex program generation
- [ ] Advanced anomaly detection (injury risk)
- [ ] A/B testing of progression algorithms
- [ ] User feedback loop for model improvement
- [ ] iOS 27 Core AI migration (if released)

---

## 7. Success Metrics

**User Outcomes (Primary):**
- **Strength gains:** Average e1RM increase per 12 weeks
- **Adherence:** % of planned workouts completed
- **Injury rate:** % of users reporting injuries (target: <5% per year)
- **Plateau avoidance:** Time to plateau vs industry baseline (target: +30% longer)

**AI Performance (Secondary):**
- **Readiness accuracy:** Correlation between readiness score and actual session performance (target: r > 0.6)
- **Plateau prediction:** Detection rate 2-4 weeks in advance (target: >70% accuracy)
- **Coach satisfaction:** User rating of AI Coach helpfulness (target: >4.5/5)
- **Adjustment effectiveness:** % of recovery-adjusted sessions that avoid overtraining (target: >85%)

**Technical (Operational):**
- **Inference latency:** Coach response time <200ms (on-device)
- **Battery impact:** <5% daily battery drain
- **Model size:** Total ML models <100MB installed
- **Crash rate:** <0.1% of sessions

---

## 8. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Users distrust AI recommendations** | Medium | High | Show rationale for every decision; allow manual overrides |
| **HealthKit data insufficient (users don't wear watch)** | Medium | Medium | Degrade gracefully to RPE-based readiness; encourage wearable use |
| **Plateau detection false positives** | Medium | Low | Require multiple methods to agree; user can dismiss alerts |
| **Apple model API changes (Core AI transition)** | High | Medium | Abstract LLM layer; maintain Core ML fallback |
| **Privacy concerns about health data** | Low | High | Transparent privacy policy; pass App Store review; annual audits |
| **Overfitting to advanced users (bias in design)** | Medium | Medium | User testing with beginners; adaptive difficulty in UI |
| **Injury from poor AI advice** | Low | Critical | Conservative recommendations; liability waiver; always defer to "listen to your body" |

---

## 9. References and Further Reading

**Exercise Science:**
- Zourdos et al. (2016). "Advanced Resistance Training Strategies"
- Helms et al. (2018). "The Muscle and Strength Pyramid: Training"
- NSCA "Essentials of Strength Training and Conditioning" (4th Ed)

**AI/ML in Fitness:**
- Apex Training, Pelaris, Tsunami Barbell case studies
- "Convergence of AI and Wearables in Strength Training" (MDPI, 2026)
- "Velocity-Based Training for 1RM Estimation" (Applied Sciences, 2025)

**Apple ML Docs:**
- "Introducing Apple Foundation Models" (Apple ML Research)
- Core AI Framework Guide (WWDC 2026)
- HealthKit Programming Guide

**Periodization:**
- Bompa & Buzzichelli. "Periodization: Theory and Methodology of Training"
- DeLorme "Progressive Resistance Exercise"
- Issurin "Block Periodization" (Sports Med, 2010)

---

## Appendix: Example Code Snippets

### A. Readiness Score Calculation (Swift)

```swift
import HealthKit

struct ReadinessCalculator {
    func calculateReadinessScore(
        sleepDuration: TimeInterval,
        sleepEfficiency: Double,
        deepSleepPercentage: Double,
        currentHRV: Double,
        baselineHRV: Double,
        hrvStdDev: Double,
        currentRHR: Double,
        baselineRHR: Double,
        acuteLoad: Double,
        chronicLoad: Double,
        subjectiveReadiness: Double?
    ) -> Double {
        
        // 1. Sleep Score (35%)
        let sleepScore = calculateSleepScore(
            duration: sleepDuration / 3600, // Convert to hours
            efficiency: sleepEfficiency,
            deepSleepPct: deepSleepPercentage
        )
        
        // 2. HRV Score (30%)
        let hrvScore = calculateHRVScore(
            current: currentHRV,
            baseline: baselineHRV,
            stdDev: hrvStdDev
        )
        
        // 3. RHR Score (15%)
        let rhrScore = calculateRHRScore(
            current: currentRHR,
            baseline: baselineRHR
        )
        
        // 4. Training Load Score (15%)
        let loadScore = calculateLoadScore(
            acute: acuteLoad,
            chronic: chronicLoad
        )
        
        // 5. Subjective (5%)
        let subjectiveScore = subjectiveReadiness ?? 70.0
        
        // Weighted combination
        let readiness = (
            sleepScore * 0.35 +
            hrvScore * 0.30 +
            rhrScore * 0.15 +
            loadScore * 0.15 +
            subjectiveScore * 0.05
        )
        
        return max(0, min(100, readiness))
    }
    
    private func calculateSleepScore(
        duration: Double,
        efficiency: Double,
        deepSleepPct: Double
    ) -> Double {
        let durationScore = gaussianScore(duration, optimal: 8.0, sigma: 1.0) * 50
        let efficiencyScore = linearScore(efficiency, min: 70, optimal: 90) * 30
        let deepScore = linearScore(deepSleepPct, min: 15, optimal: 25) * 20
        return durationScore + efficiencyScore + deepScore
    }
    
    private func calculateHRVScore(
        current: Double,
        baseline: Double,
        stdDev: Double
    ) -> Double {
        let zScore = (current - baseline) / stdDev
        return 50 + (zScore * 20)
    }
    
    private func calculateRHRScore(current: Double, baseline: Double) -> Double {
        let delta = current - baseline
        if delta <= 0 { return 90 }
        else if delta <= 5 { return 70 }
        else if delta <= 10 { return 50 }
        else { return 30 }
    }
    
    private func calculateLoadScore(acute: Double, chronic: Double) -> Double {
        guard chronic > 0 else { return 70 }
        let acwr = acute / chronic
        
        if acwr >= 0.8 && acwr <= 1.3 { return 90 }
        else if acwr < 0.8 { return 70 }
        else if acwr <= 1.5 { return 60 }
        else { return 30 }
    }
    
    // Helper functions
    private func gaussianScore(_ value: Double, optimal: Double, sigma: Double) -> Double {
        let exponent = -pow(value - optimal, 2) / (2 * pow(sigma, 2))
        return 100 * exp(exponent)
    }
    
    private func linearScore(_ value: Double, min: Double, optimal: Double) -> Double {
        if value >= optimal { return 100 }
        if value <= min { return 0 }
        return 100 * (value - min) / (optimal - min)
    }
}
```

### B. Plateau Detection (Python pseudocode for prototyping)

```python
import numpy as np
from scipy.stats import linregress

def detect_plateau(e1rm_history, threshold_days=21):
    """
    Simple plateau detector using linear regression on recent trend
    
    Args:
        e1rm_history: List of (date, e1rm) tuples
        threshold_days: Minimum days without progress to declare plateau
    
    Returns:
        bool: True if plateau detected
    """
    if len(e1rm_history) < 10:
        return False  # Not enough data
    
    # Extract recent data (last 30 days)
    recent = e1rm_history[-30:]
    dates = np.array([(d - recent[0][0]).days for d, _ in recent])
    e1rms = np.array([e for _, e in recent])
    
    # Linear regression
    slope, intercept, r_value, p_value, std_err = linregress(dates, e1rms)
    
    # Check if slope is essentially flat
    # (< 0.1 lb per week = 0.014 lb per day)
    if abs(slope) < 0.014 and p_value < 0.05:
        # Verify no PRs in last N days
        max_recent = max(e1rms)
        max_historical = max(e for _, e in e1rm_history[:-threshold_days])
        
        if max_recent <= max_historical:
            return True
    
    return False
```

### C. AI Coach Context Prompt (Pseudocode)

```python
def build_coach_context(user, recent_workouts, recovery_data):
    """
    Constructs the context window for AI Coach LLM
    """
    context = f"""
You are an expert strength training coach with 20 years of experience. You provide 
evidence-based, concise advice. You are skeptical of bro-science and always explain 
your reasoning.

USER PROFILE:
- Name: {user.name}
- Goal: {user.goal}  # e.g., "Gain strength on big 3"
- Experience: {user.training_age} months
- Current Program: {user.current_program}  # e.g., "Accumulation Phase, Week 3"
- Injuries/Limitations: {user.injuries or "None"}

RECENT TRAINING (Last 7 days):
{format_recent_workouts(recent_workouts)}

RECOVERY STATUS:
- Readiness Score: {recovery_data.readiness_score}/100
- HRV: {recovery_data.hrv_status}  # e.g., "18% below baseline"
- Sleep: {recovery_data.sleep_summary}  # e.g., "7.2 hrs avg, 83% efficiency"

CURRENT CONVERSATION:
{conversation_history[-5:]}  # Last 5 exchanges

USER QUERY:
{user_message}

Respond conversationally, use their training data to personalize advice, and always 
prioritize safety and long-term progress over short-term PRs.
"""
    return context
```

---

**Document Version:** 1.0  
**Next Review:** After Phase 1 completion  
**Questions/Feedback:** Contact Neo via `.squad/agents/neo/`
