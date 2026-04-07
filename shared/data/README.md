# Shared Data

Platform-agnostic seed data and shared resources used by both iOS and Android.

## Files

### exercises-seed.json
Complete exercise library with metadata used across the app.

**Schema:**
```json
[
  {
    "name": "Exercise Name",
    "category": "compound|isolation",
    "equipment": "barbell|dumbbell|cable|machine|bodyweight",
    "instructions": "Detailed form guide",
    "muscleGroups": [
      {
        "name": "Primary Muscle",
        "isPrimary": true
      }
    ],
    "videoURL": "https://youtube.com/watch?v=...",
    "imageURL": "image url or null",
    "muscleImageURL": "muscle diagram url or null"
  }
]
```

### programs-seed.json
Pre-built workout programs with structured exercises and progressions.

**Schema:**
```json
[
  {
    "name": "Program Name",
    "programDescription": "Overview of program",
    "durationWeeks": 4,
    "frequencyPerWeek": 4,
    "periodizationType": "linear|block|undulating",
    "targetAudience": "Beginner|Intermediate|Advanced",
    "expectedOutcome": "Expected strength/physique gains",
    "progressionScheme": "How to progress",
    "days": [
      {
        "dayNumber": 1,
        "name": "Day 1 - Squat",
        "dayDescription": "Workout overview",
        "weekVariations": [
          {
            "weekNumber": 1,
            "exercises": [
              {
                "order": 1,
                "exerciseName": "Exercise name (must match exercises-seed.json)",
                "targetSets": 3,
                "targetReps": "5x5+",
                "targetRPE": 8.5,
                "notes": "Any additional notes"
              }
            ]
          }
        ]
      }
    ]
  }
]
```

## Usage

Both iOS and Android platforms load these JSON files at app startup to populate:
- Exercise library and search
- Pre-built program templates
- Workout exercise references and form guidance

The data is immutable at runtime and serves as the canonical reference for exercise metadata across platforms.
