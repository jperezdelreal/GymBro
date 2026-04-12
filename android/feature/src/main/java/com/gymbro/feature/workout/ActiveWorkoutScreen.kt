package com.gymbro.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.abs
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.ui.localizedName
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.R
import com.gymbro.core.voice.VoiceRecognitionService
import com.gymbro.feature.common.AnimatedProgressCircle
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.GradientButton
import com.gymbro.feature.common.TooltipOverlay
import com.gymbro.feature.common.TooltipPosition
import kotlinx.coroutines.launch

private fun getMuscleGroupColor(muscleGroup: MuscleGroup): Color {
    return when (muscleGroup) {
        MuscleGroup.CHEST -> AccentGreenStart
        MuscleGroup.BACK -> AccentCyanStart
        MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES -> AccentAmberStart
        MuscleGroup.SHOULDERS -> Color(0xFFFF6B9D)
        MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS -> Color(0xFF9D4EDD)
        MuscleGroup.CORE -> Color(0xFFFFBE0B)
        MuscleGroup.FULL_BODY -> AccentGreenStart
    }
}

@Composable
fun ActiveWorkoutRoute(
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
    onNavigateToExercisePicker: () -> Unit = {},
    onNavigateToSummary: (Long, Double, Int, Int, List<com.gymbro.core.model.PersonalRecord>) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    pickedExercise: Exercise? = null,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCompleteSetTooltip by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showCompleteSetTooltip = viewModel.tooltipManager.shouldShow("active_workout_complete_set")
    }
    
    // Get services from Hilt through entry point
    val voiceRecognitionService = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            VoiceServiceEntryPoint::class.java
        ).voiceRecognitionService()
    }
    
    val userPreferences = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            PreferencesEntryPoint::class.java
        ).userPreferences()
    }
    
    val weightUnit = userPreferences.weightUnit.collectAsStateWithLifecycle(initialValue = UserPreferences.WeightUnit.KG)

    // Handle picked exercise
    LaunchedEffect(pickedExercise) {
        if (pickedExercise != null) {
            viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(pickedExercise))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ActiveWorkoutEffect.ShowExercisePicker -> onNavigateToExercisePicker()
                is ActiveWorkoutEffect.NavigateToSummary -> onNavigateToSummary(
                    effect.durationSeconds,
                    effect.totalVolume,
                    effect.totalSets,
                    effect.exerciseCount,
                    effect.personalRecords,
                )
                is ActiveWorkoutEffect.NavigateBack -> onNavigateBack()
                is ActiveWorkoutEffect.RestTimerFinished -> {
                    // Vibrate with a pattern (short-long-short-long-long)
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 200, 100, 200, 100, 300),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 300), -1)
                    }
                    
                    // Play notification sound
                    try {
                        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(context, notification)
                        ringtone.play()
                    } catch (e: Exception) {
                        // Silently fail if sound cannot be played
                    }
                }
            }
        }
    }

    ActiveWorkoutScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
        voiceRecognitionService = voiceRecognitionService,
        defaultWeightUnit = weightUnit.value,
        showCompleteSetTooltip = showCompleteSetTooltip,
        onTooltipDismissed = {
            showCompleteSetTooltip = false
            viewModel.viewModelScope.launch {
                viewModel.tooltipManager.markShown("active_workout_complete_set")
            }
        },
        onNavigateToCoach = onNavigateToCoach,
        onDiscardWorkout = { viewModel.onEvent(ActiveWorkoutEvent.DiscardWorkout) },
    )
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface VoiceServiceEntryPoint {
    fun voiceRecognitionService(): com.gymbro.core.voice.VoiceRecognitionService
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PreferencesEntryPoint {
    fun userPreferences(): com.gymbro.core.preferences.UserPreferences
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutState,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    voiceRecognitionService: VoiceRecognitionService,
    defaultWeightUnit: UserPreferences.WeightUnit,
    showCompleteSetTooltip: Boolean = false,
    onTooltipDismissed: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onDiscardWorkout: () -> Unit = {},
) {
    if (state.isLoading) {
        FullScreenLoading(message = stringResource(R.string.active_workout_starting))
        return
    }

    Box {
        val haptic = LocalHapticFeedback.current
        var showDiscardDialog by remember { mutableStateOf(false) }

        BackHandler { showDiscardDialog = true }
        
        if (showDiscardDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(stringResource(R.string.discard_workout_title)) },
                text = { Text(stringResource(R.string.discard_workout_message)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onEvent(ActiveWorkoutEvent.DiscardWorkout)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = com.gymbro.core.ui.theme.AccentRed,
                        ),
                    ) {
                        Text(stringResource(R.string.discard_workout_confirm))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showDiscardDialog = false },
                    ) {
                        Text(stringResource(R.string.discard_workout_cancel))
                    }
                },
            )
        }
        
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.active_workout_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = formatDuration(state.elapsedSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyanStart,
                            modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDiscardDialog = true 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.workout_discard))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            if (!state.isRestTimerActive) {
                FinishWorkoutButton(
                    enabled = state.totalSets > 0 && !state.isCompleting,
                    completedExercises = state.exercises.count { ex -> ex.sets.any { it.isCompleted } },
                    onClick = { onEvent(ActiveWorkoutEvent.CompleteWorkout) },
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = if (!state.isRestTimerActive) 16.dp else 0.dp)
            ) {
                // AI Coach FAB
                FloatingActionButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToCoach()
                    },
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = Color(0xFF00FF87),
                ) {
                    Icon(
                        Icons.Default.SmartToy, 
                        contentDescription = stringResource(R.string.active_workout_coach_fab)
                    )
                }
                
                // Add Exercise FAB
                FloatingActionButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(ActiveWorkoutEvent.AddExerciseClicked) 
                    },
                    containerColor = Color.Transparent,
                    modifier = Modifier.drawBehind {
                        val gradient = Brush.horizontalGradient(
                            colors = listOf(AccentGreenStart, AccentGreenEnd)
                        )
                        drawCircle(gradient)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.active_workout_add_exercise), tint = Color.White)
                }
            }
        },
        containerColor = Background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Hero Rest Timer at top when active
                if (state.isRestTimerActive) {
                    item {
                        HeroRestTimer(
                            remainingSeconds = state.restTimerSeconds,
                            totalSeconds = state.restTimerTotal,
                            onSkip = { onEvent(ActiveWorkoutEvent.SkipRestTimer) },
                            onAdjust = { delta -> onEvent(ActiveWorkoutEvent.AdjustRestTimer(delta)) },
                        )
                    }
                }

                // Stats card
                item {
                    GlassmorphicCard {
                        WorkoutStatsContent(
                            elapsedSeconds = state.elapsedSeconds,
                            totalVolume = state.totalVolume,
                            totalSets = state.totalSets,
                            weightUnit = defaultWeightUnit,
                        )
                    }
                }

                // Empty workout guidance
                if (state.exercises.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.active_workout_empty_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                // Fatigue warnings banner (#418)
                if (state.fatigueWarnings.isNotEmpty()) {
                    item {
                        FatigueWarningBanner(
                            warnings = state.fatigueWarnings,
                            onDismiss = { onEvent(ActiveWorkoutEvent.DismissFatigueWarnings) },
                        )
                    }
                }

                // Exercise cards
                itemsIndexed(
                    items = state.exercises,
                    key = { index, ex -> "${ex.exercise.id}_$index" },
                ) { exerciseIndex, exerciseUi ->
                    GlassmorphicCard(
                        accentColor = getMuscleGroupColor(exerciseUi.exercise.muscleGroup),
                    ) {
                        ExerciseCardContent(
                            exerciseUi = exerciseUi,
                            exerciseIndex = exerciseIndex,
                            onEvent = onEvent,
                            voiceRecognitionService = voiceRecognitionService,
                            defaultWeightUnit = defaultWeightUnit,
                        )
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

        if (showCompleteSetTooltip && state.exercises.isNotEmpty() && state.exercises.any { it.sets.isNotEmpty() }) {
            TooltipOverlay(
                message = stringResource(R.string.active_workout_tap_to_complete),
                position = TooltipPosition.CENTER,
                offsetY = 100,
                onDismiss = onTooltipDismissed
            )
        }
    }
}

@Composable
private fun WorkoutStatsContent(
    elapsedSeconds: Long,
    totalVolume: Double,
    totalSets: Int,
    weightUnit: UserPreferences.WeightUnit = UserPreferences.WeightUnit.KG,
) {
    val unitLabel = if (weightUnit == UserPreferences.WeightUnit.LBS) "lb" else "kg"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(label = stringResource(R.string.common_duration), value = formatDuration(elapsedSeconds), color = AccentCyanStart)
        StatItem(label = stringResource(R.string.common_volume), value = "${totalVolume.toInt()} $unitLabel", color = AccentGreenStart)
        StatItem(label = stringResource(R.string.workout_sets), value = "$totalSets", color = AccentAmberStart)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) { }) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun FatigueWarningBanner(
    warnings: List<FatigueWarningUi>,
    onDismiss: () -> Unit,
) {
    val warningColor = Color(0xFFFF9800)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(warningColor.copy(alpha = 0.15f))
            .border(1.dp, warningColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.fatigue_warning_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = warningColor,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.fatigue_warning_dismiss),
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        warnings.forEach { warning ->
            val deltaText = warning.rpeDelta?.let { d ->
                " (+${String.format("%.1f", d)})"
            } ?: ""
            Text(
                text = stringResource(
                    R.string.fatigue_warning_detail,
                    warning.exerciseName,
                    String.format("%.1f", warning.currentAvgRpe),
                    deltaText,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun HeroRestTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val subtractTimeDescription = stringResource(R.string.rest_timer_cd_subtract_time)
    val addTimeDescription = stringResource(R.string.rest_timer_cd_add_time)
    val progress = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero circular timer with AnimatedProgressCircle
        AnimatedProgressCircle(
            progress = progress,
            size = 160.dp,
            strokeWidth = 14.dp,
            gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${remainingSeconds}s",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = glowAlpha),
                )
                Text(
                    text = stringResource(R.string.active_workout_rest),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controls with GradientButton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAdjust(-15) 
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.height(48.dp),
            ) { 
                Text(
                    text = "-15s",
                    modifier = Modifier.semantics { contentDescription = subtractTimeDescription },
                )
            }
            GradientButton(text = stringResource(R.string.active_workout_skip), onClick = onSkip)
            OutlinedButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAdjust(15) 
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.height(48.dp),
            ) { 
                Text(
                    text = "+15s",
                    modifier = Modifier.semantics { contentDescription = addTimeDescription },
                )
            }
        }
    }
}

@Composable
private fun FinishWorkoutButton(
    enabled: Boolean,
    completedExercises: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val shouldGlow = completedExercises >= 3
    
    val infiniteTransition = rememberInfiniteTransition(label = "finish_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (shouldGlow) 0.3f else 0f,
        targetValue = if (shouldGlow) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finish_glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .drawBehind {
                if (shouldGlow) {
                    val gradient = Brush.radialGradient(
                        colors = listOf(
                            AccentGreenStart.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                    drawCircle(gradient, radius = size.width)
                }
            }
    ) {
        GradientButton(
            text = stringResource(R.string.workout_finish),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExerciseCardContent(
    exerciseUi: WorkoutExerciseUi,
    exerciseIndex: Int,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    voiceRecognitionService: com.gymbro.core.voice.VoiceRecognitionService,
    defaultWeightUnit: com.gymbro.core.preferences.UserPreferences.WeightUnit,
) {
    val haptic = LocalHapticFeedback.current
    var voiceToast by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss voice feedback after a delay
    LaunchedEffect(voiceToast) {
        if (voiceToast != null) {
            kotlinx.coroutines.delay(2500)
            voiceToast = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Exercise header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = getMuscleGroupColor(exerciseUi.exercise.muscleGroup),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = exerciseUi.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // Voice input — auto-fills the first incomplete set
            VoiceInputButton(
                voiceRecognitionService = voiceRecognitionService,
                defaultWeightUnit = defaultWeightUnit,
                onVoiceResult = { parsed ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val targetSetIndex = exerciseUi.sets.indexOfFirst { !it.isCompleted }
                    if (targetSetIndex >= 0) {
                        val parser = com.gymbro.core.voice.VoiceInputParser()
                        onEvent(
                            ActiveWorkoutEvent.VoiceInput(
                                exerciseIndex = exerciseIndex,
                                setIndex = targetSetIndex,
                                weight = parsed.weight.let { w ->
                                    if (w == w.toLong().toDouble()) w.toLong().toString() else w.toString()
                                },
                                reps = parsed.reps.toString(),
                                rpe = parsed.rpe?.let { r ->
                                    if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
                                } ?: "",
                            )
                        )
                        voiceToast = parser.formatConfirmation(parsed)
                    }
                },
                onError = { errorMsg ->
                    voiceToast = errorMsg
                },
            )
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(ActiveWorkoutEvent.RemoveExercise(exerciseIndex)) 
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.active_workout_remove_exercise),
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Voice feedback toast
        AnimatedVisibility(
            visible = voiceToast != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            voiceToast?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyanStart,
                    modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = exerciseUi.exercise.muscleGroup.localizedName(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            Text(stringResource(R.string.active_workout_header_set), style = setHeaderStyle(), modifier = Modifier.width(40.dp))
            Text(stringResource(R.string.active_workout_header_weight), style = setHeaderStyle(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.active_workout_header_reps), style = setHeaderStyle(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(56.dp)) // for complete button
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Set rows
        exerciseUi.sets.forEachIndexed { setIndex, setUi ->
            SetRow(
                setUi = setUi,
                exerciseIndex = exerciseIndex,
                setIndex = setIndex,
                onEvent = onEvent,
            )
            if (setIndex < exerciseUi.sets.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add set button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(ActiveWorkoutEvent.AddSet(exerciseIndex)) 
                }
                .border(1.dp, AccentGreenStart.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.active_workout_cd_add_set), tint = AccentGreenStart, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.workout_add_set), color = AccentGreenStart, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SetRow(
    setUi: WorkoutSetUi,
    exerciseIndex: Int,
    setIndex: Int,
    onEvent: (ActiveWorkoutEvent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val completedDescription = stringResource(R.string.active_workout_set_completed, setUi.setNumber)
    val completeDescription = stringResource(R.string.active_workout_complete_set, setUi.setNumber)
    val warmupToggleDescription = stringResource(R.string.active_workout_cd_warmup_toggle)
    val rowBackground = when {
        setUi.isCompleted -> AccentGreenStart.copy(alpha = 0.08f)
        setUi.isWarmup -> AccentAmberStart.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .clickable(enabled = !setUi.isCompleted && setUi.weight.isNotEmpty() && setUi.reps.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(ActiveWorkoutEvent.QuickCompleteSet(exerciseIndex, setIndex))
            }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Set number + warmup indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(ActiveWorkoutEvent.ToggleWarmup(exerciseIndex, setIndex)) 
                }
                .semantics { contentDescription = warmupToggleDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (setUi.isWarmup) "W" else "${setUi.setNumber}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (setUi.isWarmup) AccentAmberStart else Color.White,
            )
        }

        // Weight with gesture support
        GestureNumberField(
            value = setUi.weight,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetWeight(exerciseIndex, setIndex, it)) },
            enabled = !setUi.isCompleted,
            modifier = Modifier.weight(1f).testTag("weight_input"),
            stepSize = 2.5,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Reps with gesture support
        GestureNumberField(
            value = setUi.reps,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetReps(exerciseIndex, setIndex, it)) },
            enabled = !setUi.isCompleted,
            modifier = Modifier.weight(1f).testTag("reps_input"),
            stepSize = 1.0,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // RPE quick picker — compact tap-to-cycle selector
        RpeQuickPicker(
            selectedRpe = setUi.rpe,
            onRpeSelected = { onEvent(ActiveWorkoutEvent.UpdateSetRpe(exerciseIndex, setIndex, it)) },
            enabled = !setUi.isCompleted,
            modifier = Modifier.width(48.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Complete button
        if (setUi.isCompleted) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(AccentGreenStart, AccentGreenEnd)))
                    .semantics { contentDescription = completedDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(ActiveWorkoutEvent.CompleteSet(exerciseIndex, setIndex))
                    }
                    .background(Brush.horizontalGradient(listOf(AccentGreenStart, AccentGreenEnd)))
                    .alpha(if (setUi.weight.isNotEmpty() && setUi.reps.isNotEmpty()) 1f else 0.4f)
                    .semantics { contentDescription = completeDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun GestureNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    stepSize: Double = 1.0,
) {
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember { mutableStateOf(0f) }
    val dragThreshold = 40f

    TextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(input)
            }
        },
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                        val threshold = dragThreshold
                        
                        if (abs(dragOffset) >= threshold) {
                            val currentValue = value.toDoubleOrNull() ?: 0.0
                            val delta = if (dragOffset < 0) stepSize else -stepSize
                            val newValue = (currentValue + delta).coerceAtLeast(0.0)
                            
                            val formatted = if (stepSize >= 1.0) {
                                newValue.toInt().toString()
                            } else {
                                String.format("%.1f", newValue)
                            }
                            onValueChange(formatted)
                            
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            dragOffset = 0f
                        }
                    },
                    onDragEnd = { dragOffset = 0f }
                )
            },
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = AccentGreenStart,
        ),
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun setHeaderStyle() = MaterialTheme.typography.labelSmall.copy(
    color = Color.White.copy(alpha = 0.4f),
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
)

/**
 * Compact RPE quick picker — tap to cycle through RPE values (6-10).
 * Speed matters during a workout, so this is a single-tap cycle, not a dropdown.
 * Displays current RPE with color coding: green (6-7), amber (8), red (9-10).
 */
@Composable
private fun RpeQuickPicker(
    selectedRpe: String,
    onRpeSelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val rpeValues = listOf("", "6", "7", "8", "9", "10")
    val currentIndex = rpeValues.indexOf(selectedRpe).coerceAtLeast(0)

    val displayText = if (selectedRpe.isEmpty()) "—" else selectedRpe
    val targetRpeColor = when (selectedRpe.toIntOrNull()) {
        in 6..7 -> AccentGreenStart
        8 -> AccentAmberStart
        in 9..10 -> AccentRed
        else -> Color.White.copy(alpha = 0.4f)
    }
    val rpeColor by animateColorAsState(
        targetValue = targetRpeColor,
        animationSpec = tween(durationMillis = 300),
        label = "rpeColor",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selectedRpe.isNotEmpty()) rpeColor.copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val nextIndex = (currentIndex + 1) % rpeValues.size
                onRpeSelected(rpeValues[nextIndex])
            }
            .testTag("rpe_picker"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) rpeColor else rpeColor.copy(alpha = 0.4f),
        )
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
