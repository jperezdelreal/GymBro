import Foundation

// Core models
public typealias Workout = Workout
public typealias Exercise = Exercise
public typealias ExerciseSet = ExerciseSet
public typealias Program = Program
public typealias ProgramDay = ProgramDay
public typealias UserProfile = UserProfile
public typealias ChatMessage = ChatMessage

// Supporting models
public typealias MuscleGroup = MuscleGroup
public typealias BodyweightEntry = BodyweightEntry
public typealias PlannedExercise = PlannedExercise

// Enums
public typealias UnitSystem = UnitSystem
public typealias ExperienceLevel = ExperienceLevel
public typealias ExerciseCategory = ExerciseCategory
public typealias Equipment = Equipment
public typealias SetType = SetType
public typealias PeriodizationType = PeriodizationType
public typealias MessageRole = MessageRole

// Services
public typealias RestTimerService = RestTimerService
public typealias NotificationService = NotificationService
public typealias NextSetInfo = NextSetInfo
public typealias PersonalRecord = PersonalRecord
public typealias PersonalRecordService = PersonalRecordService
public typealias ExerciseDataSeeder = ExerciseDataSeeder

// AI Coach Services
public typealias AICoachService = AICoachService
public typealias AzureOpenAICoachService = AzureOpenAICoachService
public typealias DeterministicCoachFallback = DeterministicCoachFallback
public typealias PromptBuilder = PromptBuilder
public typealias SafetyFilter = SafetyFilter
public typealias AICoachConfiguration = AICoachConfiguration
public typealias UsageLimiter = UsageLimiter
public typealias CoachContext = CoachContext
public typealias AICoachError = AICoachError
// Progress Tracking (Issue #8)
public typealias E1RMCalculator = E1RMCalculator
public typealias E1RMFormula = E1RMFormula
public typealias ProgressTrackingService = ProgressTrackingService
public typealias E1RMDataPoint = E1RMDataPoint
public typealias VolumeDataPoint = VolumeDataPoint
public typealias FrequencyDataPoint = FrequencyDataPoint
public typealias MuscleGroupBalance = MuscleGroupBalance
public typealias PREvent = PREvent
public typealias TimeWindow = TimeWindow

// Plateau Detection (Issue #9)
public typealias PlateauDetectionService = PlateauDetectionService
public typealias PlateauAnalysis = PlateauAnalysis
public typealias ProgressState = ProgressState
public typealias RollingAverageAnalyzer = RollingAverageAnalyzer
public typealias ChangePointDetector = ChangePointDetector
public typealias TrendForecaster = TrendForecaster