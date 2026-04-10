package com.gymbro.core.ai

import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.RecordType
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.RpeTrendService
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiCoachService @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val personalRecordService: PersonalRecordService,
    private val rpeTrendService: RpeTrendService,
) {
    private val chatHistory = mutableListOf<ChatMessage>()
    
    private val systemPrompt = """
        You are GymBro AI Coach, an expert strength training advisor.
        
        Be concise, actionable, and evidence-based. Focus on:
        - Form tips and technique corrections
        - Programming advice (sets, reps, frequency, split selection)
        - Plateau-breaking strategies
        - Recovery and deload guidance
        
        When discussing training splits, understand that:
        - Full Body (2-3x/week): Best for beginners or limited time, hits all muscles each session
        - Upper/Lower (4x/week): Balanced split for intermediate lifters, 2x frequency for each half
        - PPL (Push/Pull/Legs, 6x/week): High volume bodybuilding split, 2x per muscle per week
        - PPLUL (5x/week): Hybrid of PPL + Upper/Lower for advanced lifters
        
        Never give medical advice — recommend seeing a doctor for injuries.
        Keep responses under 150 words.
        Use a friendly but professional tone.
    """.trimIndent()

    private val isFirebaseEnabled: Boolean
        get() = try {
            Class.forName("com.google.firebase.FirebaseApp")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    private fun createModel(): GenerativeModel? {
        if (!isFirebaseEnabled) return null
        
        return try {
            Firebase.vertexAI.generativeModel(
                modelName = "gemini-2.0-flash-exp",
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 300
                },
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendMessage(userMessage: String): Result<ChatMessage> {
        val model = createModel() ?: return Result.failure(
            Exception("Firebase AI is not configured. Add google-services.json to enable AI Coach.")
        )

        try {
            val userChatMessage = ChatMessage(
                role = MessageRole.USER,
                content = userMessage,
            )
            chatHistory.add(userChatMessage)

            val context = buildContext()
            val fullPrompt = buildString {
                append(systemPrompt)
                append("\n\n")
                append(context)
                append("\n\nUser: $userMessage")
            }

            val response = model.generateContent(fullPrompt)
            val responseText = response.text ?: "I'm having trouble generating a response. Please try again."

            val assistantMessage = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = responseText,
            )
            chatHistory.add(assistantMessage)

            return Result.success(assistantMessage)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun buildContext(): String {
        val recentWorkouts = workoutRepository.getRecentWorkouts(limit = 5).first()
        val daysSinceLastWorkout = workoutRepository.getDaysSinceLastWorkout()
        
        val exerciseIds = recentWorkouts.flatMap { workout ->
            workout.sets.map { it.exerciseId }
        }.distinct()

        val records = mutableListOf<PersonalRecord>()
        exerciseIds.take(3).forEach { exerciseId ->
            try {
                val exerciseRecords = personalRecordService.getPersonalRecords(
                    exerciseId = exerciseId.toString(),
                    exerciseName = "Exercise"
                )
                records.addAll(exerciseRecords.filter { it.type == RecordType.MAX_E1RM })
            } catch (e: Exception) {
                // Skip if error
            }
        }

        // Gather RPE trends and fatigue warnings
        val rpeTrends = exerciseIds.take(5).mapNotNull { exerciseId ->
            try {
                rpeTrendService.getTrend(exerciseId.toString())
            } catch (e: Exception) {
                null
            }
        }
        val fatigueWarnings = rpeTrends.filter { it.isFatigueWarning }

        // Compute overall average RPE from recent sets
        val recentRpeSets = recentWorkouts.flatMap { it.sets }
            .filter { !it.isWarmup && it.rpe != null }
        val overallAvgRpe = if (recentRpeSets.isNotEmpty()) {
            recentRpeSets.mapNotNull { it.rpe }.average()
        } else null

        return buildString {
            append("USER CONTEXT:\n")
            
            if (daysSinceLastWorkout != null) {
                append("- Days since last workout: $daysSinceLastWorkout\n")
            }
            
            if (recentWorkouts.isNotEmpty()) {
                append("- Recent workout count (last 5): ${recentWorkouts.size}\n")
                try {
                    var totalVolume = 0.0
                    for (workout in recentWorkouts) {
                        for (exerciseSet in workout.sets.filter { !it.isWarmup }) {
                            totalVolume += exerciseSet.weightKg * exerciseSet.reps
                        }
                    }
                    append("- Total volume (recent): ${String.format("%.0f", totalVolume)} kg\n")
                } catch (e: Exception) {
                    // Skip volume calculation if error
                }
            }

            // RPE trends
            if (overallAvgRpe != null) {
                append("- Recent average RPE: ${String.format("%.1f", overallAvgRpe)}\n")
            }
            if (fatigueWarnings.isNotEmpty()) {
                append("- ⚠ Fatigue warnings (RPE trending up):\n")
                fatigueWarnings.forEach { trend ->
                    val prev = trend.previousAvgRpe?.let { String.format("%.1f", it) } ?: "N/A"
                    append("  * Exercise ${trend.exerciseId}: avg RPE ${String.format("%.1f", trend.currentAvgRpe)} (was $prev)\n")
                }
            }
            if (rpeTrends.isNotEmpty()) {
                val risingCount = rpeTrends.count { it.trend == RpeTrendService.TrendDirection.RISING }
                val fallingCount = rpeTrends.count { it.trend == RpeTrendService.TrendDirection.FALLING }
                if (risingCount > 0 || fallingCount > 0) {
                    append("- RPE trends: $risingCount exercises rising, $fallingCount falling\n")
                }
            }
            
            if (records.isNotEmpty()) {
                append("- Top personal records:\n")
                records.take(3).forEach { record ->
                    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
                    val dateStr = record.date.atZone(ZoneId.systemDefault()).format(dateFormatter)
                    append("  * ${record.exerciseName}: ${String.format("%.1f", record.value)} kg e1RM ($dateStr)\n")
                }
            }
            
            if (recentWorkouts.isEmpty()) {
                append("- No recent workout history\n")
            }
        }
    }

    fun getChatHistory(): List<ChatMessage> = chatHistory.toList()

    fun clearHistory() {
        chatHistory.clear()
    }
}
