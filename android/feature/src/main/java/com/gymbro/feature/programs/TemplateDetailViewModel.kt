package com.gymbro.feature.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.WorkoutTemplateLibrary
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    private val activePlanStore: ActivePlanStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val templateName: String = savedStateHandle.get<String>("templateName") ?: ""
    private val template: WorkoutTemplateLibrary.CuratedTemplate = 
        WorkoutTemplateLibrary.templates.first { it.name == templateName }

    private val _state = MutableStateFlow(TemplateDetailState(template))
    val state: StateFlow<TemplateDetailState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<TemplateDetailEffect>()
    val effect: SharedFlow<TemplateDetailEffect> = _effect.asSharedFlow()

    fun onEvent(event: TemplateDetailEvent) {
        when (event) {
            is TemplateDetailEvent.BackClicked -> {
                viewModelScope.launch {
                    _effect.emit(TemplateDetailEffect.NavigateBack)
                }
            }
            is TemplateDetailEvent.StartProgram -> {
                viewModelScope.launch {
                    _state.update { it.copy(isStarting = true) }
                    
                    // Convert CuratedTemplate to WorkoutPlan
                    val workoutPlan = convertTemplateToWorkoutPlan(template)
                    
                    // Save as active plan
                    activePlanStore.setPlan(workoutPlan)
                    
                    _state.update { it.copy(isStarting = false) }
                    _effect.emit(TemplateDetailEffect.ProgramStarted(template))
                }
            }
        }
    }

    private fun convertTemplateToWorkoutPlan(
        template: WorkoutTemplateLibrary.CuratedTemplate
    ): WorkoutPlan {
        val goal = when (template.splitType) {
            "Strength" -> com.gymbro.core.preferences.UserPreferences.TrainingGoal.STRENGTH
            "Hypertrophy" -> com.gymbro.core.preferences.UserPreferences.TrainingGoal.HYPERTROPHY
            "Powerbuilding" -> com.gymbro.core.preferences.UserPreferences.TrainingGoal.POWERLIFTING
            else -> com.gymbro.core.preferences.UserPreferences.TrainingGoal.GENERAL_FITNESS
        }

        val experienceLevel = when (template.targetAudience) {
            WorkoutTemplateLibrary.TargetAudience.BEGINNER -> 
                com.gymbro.core.preferences.UserPreferences.ExperienceLevel.BEGINNER
            WorkoutTemplateLibrary.TargetAudience.INTERMEDIATE -> 
                com.gymbro.core.preferences.UserPreferences.ExperienceLevel.INTERMEDIATE
            WorkoutTemplateLibrary.TargetAudience.ADVANCED -> 
                com.gymbro.core.preferences.UserPreferences.ExperienceLevel.ADVANCED
        }

        val workoutDays = template.days.mapIndexed { index, day ->
            WorkoutDay(
                dayNumber = index + 1,
                name = day.dayName,
                exercises = day.exercises.map { exercise ->
                    PlannedExercise(
                        exerciseName = exercise.name,
                        sets = parseSets(exercise.setsAndReps),
                        repsRange = parseRepsRange(exercise.setsAndReps),
                        restSeconds = 90
                    )
                }
            )
        }

        return WorkoutPlan(
            name = template.name,
            description = template.description,
            goal = goal,
            experienceLevel = experienceLevel,
            daysPerWeek = template.daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays
        )
    }

    private fun parseSets(setsAndReps: String): Int {
        // Parse formats like "3x5", "4x8-10", "5x3 (T1)", etc.
        val parts = setsAndReps.split("x")
        return parts.firstOrNull()?.toIntOrNull() ?: 3
    }

    private fun parseRepsRange(setsAndReps: String): String {
        // Parse formats like "3x5", "4x8-10", "5x3 (T1)", etc.
        val parts = setsAndReps.split("x")
        if (parts.size < 2) return "8-12"
        
        val repsPart = parts[1].trim()
        // Remove any text in parentheses like "(T1)"
        val cleanReps = repsPart.replace(Regex("\\s*\\(.*\\)"), "").trim()
        return cleanReps.ifEmpty { "8-12" }
    }
}
