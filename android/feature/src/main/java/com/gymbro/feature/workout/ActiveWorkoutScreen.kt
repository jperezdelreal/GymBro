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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
                    val supersetGroup = state.supersetGroups.entries.firstOrNull { exerciseIndex in it.value }
                    val supersetLabel = if (supersetGroup != null) {
                        val position = supersetGroup.value.indexOf(exerciseIndex)
                        val letter = 'A' + (supersetGroup.value.first() / 26)
                        "$letter${position + 1}"
                    } else null
                    val isSelected = state.selectedExercises.contains(exerciseIndex)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (supersetGroup != null) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(120.dp)
                                    .background(
                                        AccentGreenStart,
                                        shape = if (exerciseIndex == supersetGroup.value.first() && exerciseIndex == supersetGroup.value.last()) {
                                            RoundedCornerShape(4.dp)
                                        } else if (exerciseIndex == supersetGroup.value.first()) {
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        } else if (exerciseIndex == supersetGroup.value.last()) {
                                            RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                        } else {
                                            RoundedCornerShape(0.dp)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        GlassmorphicCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEvent(ActiveWorkoutEvent.ToggleExerciseSelection(exerciseIndex)) }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) AccentGreenStart else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            accentColor = getMuscleGroupColor(exerciseUi.exercise.muscleGroup),
                        ) {
                            Column {
                                if (supersetLabel != null) {
                                    Text(
                                        text = supersetLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreenStart,
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                    )
                                }
                                ExerciseCardContent(
                                    exerciseUi = exerciseUi,
                                    exerciseIndex = exerciseIndex,
                                    onEvent = onEvent,
                                    voiceRecognitionService = voiceRecognitionService,
                                    defaultWeightUnit = defaultWeightUnit,
                                )
                            }
                        }
                    }
                }
                
                if (state.selectedExercises.size >= 2) {
                    item {
                        GradientButton(
                            text = stringResource(R.string.active_workout_create_superset),
                            onClick = { onEvent(ActiveWorkoutEvent.CreateSuperset) },
                            modifier = Modifier.fillMaxWidth()
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

        // Exercise Detail Bottom Sheet
        state.exerciseDetailSheet?.let { detailState ->
            ExerciseDetailBottomSheet(
                detailState = detailState,
                onDismiss = { onEvent(ActiveWorkoutEvent.DismissExerciseDetail) },
                defaultWeightUnit = defaultWeightUnit,
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
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        onClick = { onEvent(ActiveWorkoutEvent.ShowExerciseDetail(exerciseUi.exercise)) }
                    )
                    .semantics { heading() },
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
        
        // Progression suggestion chip
        exerciseUi.progressionSuggestion?.let { suggestion ->
            Spacer(modifier = Modifier.height(8.dp))
            ProgressionSuggestionChip(
                suggestion = suggestion,
                defaultWeightUnit = defaultWeightUnit,
            )
        }

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
    
    var showWeightDialog by remember { mutableStateOf(false) }
    var showRepsDialog by remember { mutableStateOf(false) }
    var showWarmupTooltip by remember { mutableStateOf(false) }
    var showRpeTooltip by remember { mutableStateOf(false) }
    
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
        // Set number + warmup indicator with tooltip
        Box(
            modifier = Modifier
                .size(56.dp)
                .clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(ActiveWorkoutEvent.ToggleWarmup(exerciseIndex, setIndex)) 
                }
                .semantics { contentDescription = warmupToggleDescription },
            contentAlignment = Alignment.Center,
        ) {
            if (setUi.isWarmup) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.warmup_badge_label),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmberStart,
                    )
                    IconButton(
                        onClick = { showWarmupTooltip = true },
                        modifier = Modifier.size(16.dp),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Warmup info",
                            tint = AccentAmberStart.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = "${setUi.setNumber}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // Weight input field - tap to open dialog
        ClickableNumberField(
            value = setUi.weight,
            enabled = !setUi.isCompleted,
            onClick = { if (!setUi.isCompleted) showWeightDialog = true },
            modifier = Modifier.weight(1f).testTag("weight_input"),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Reps input field - tap to open dialog
        ClickableNumberField(
            value = setUi.reps,
            enabled = !setUi.isCompleted,
            onClick = { if (!setUi.isCompleted) showRepsDialog = true },
            modifier = Modifier.weight(1f).testTag("reps_input"),
        )

        Spacer(modifier = Modifier.width(4.dp))

        // RPE with label and info icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.rpe_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                )
                IconButton(
                    onClick = { showRpeTooltip = true },
                    modifier = Modifier.size(12.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "RPE info",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
            RpeQuickPicker(
                selectedRpe = setUi.rpe,
                onRpeSelected = { onEvent(ActiveWorkoutEvent.UpdateSetRpe(exerciseIndex, setIndex, it)) },
                enabled = !setUi.isCompleted,
                modifier = Modifier.width(48.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Complete button
        if (setUi.isCompleted) {
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                    .size(56.dp)
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
    
    // Dialogs
    if (showWeightDialog) {
        NumberInputDialog(
            title = stringResource(R.string.number_input_weight_title),
            value = setUi.weight,
            stepSize = 2.5,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetWeight(exerciseIndex, setIndex, it)) },
            onDismiss = { showWeightDialog = false },
            onConfirm = { showWeightDialog = false },
        )
    }
    
    if (showRepsDialog) {
        NumberInputDialog(
            title = stringResource(R.string.number_input_reps_title),
            value = setUi.reps,
            stepSize = 1.0,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetReps(exerciseIndex, setIndex, it)) },
            onDismiss = { showRepsDialog = false },
            onConfirm = { showRepsDialog = false },
        )
    }
    
    if (showWarmupTooltip) {
        InfoTooltipDialog(
            title = stringResource(R.string.warmup_tooltip_title),
            message = stringResource(R.string.warmup_tooltip_message),
            onDismiss = { showWarmupTooltip = false },
        )
    }
    
    if (showRpeTooltip) {
        InfoTooltipDialog(
            title = stringResource(R.string.rpe_tooltip_title),
            message = stringResource(R.string.rpe_tooltip_message),
            onDismiss = { showRpeTooltip = false },
        )
    }
}

/**
 * Clickable number field that opens a dialog when tapped.
 * Replaces the gesture-based input for better UX.
 */
@Composable
private fun ClickableNumberField(
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) Color.White.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.03f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (value.isEmpty()) "—" else value,
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            ),
        )
    }
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

@Composable
private fun ProgressionSuggestionChip(
    suggestion: ProgressionSuggestionUi,
    defaultWeightUnit: com.gymbro.core.preferences.UserPreferences.WeightUnit,
) {
    val unitLabel = when (defaultWeightUnit) {
        com.gymbro.core.preferences.UserPreferences.WeightUnit.KG -> stringResource(R.string.common_kg)
        com.gymbro.core.preferences.UserPreferences.WeightUnit.LBS -> stringResource(R.string.common_lbs)
    }
    
    val (chipColor, suggestionText, reasonText) = when (suggestion.reason) {
        com.gymbro.core.service.ProgressionEngine.ProgressionReason.PROGRESS -> Triple(
            AccentGreenStart,
            stringResource(R.string.progression_suggested, "${suggestion.suggestedWeight} $unitLabel"),
            stringResource(R.string.progression_reason_progress),
        )
        com.gymbro.core.service.ProgressionEngine.ProgressionReason.REGRESS -> Triple(
            com.gymbro.core.ui.theme.AccentRed,
            stringResource(R.string.progression_reduce, "${suggestion.suggestedWeight} $unitLabel"),
            stringResource(R.string.progression_reason_regress),
        )
        com.gymbro.core.service.ProgressionEngine.ProgressionReason.MAINTAIN -> Triple(
            AccentAmberStart,
            stringResource(R.string.progression_maintain, "${suggestion.lastWeight} $unitLabel"),
            stringResource(R.string.progression_reason_maintain),
        )
        com.gymbro.core.service.ProgressionEngine.ProgressionReason.NO_DATA -> Triple(
            Color.White.copy(alpha = 0.6f),
            stringResource(R.string.progression_maintain, "${suggestion.lastWeight} $unitLabel"),
            stringResource(R.string.progression_reason_no_data),
        )
    }
    
    val lastWorkoutText = if (suggestion.lastRpe != null) {
        stringResource(
            R.string.progression_last_with_rpe,
            "${suggestion.lastWeight} $unitLabel",
            suggestion.lastReps,
            String.format("%.1f", suggestion.lastRpe)
        )
    } else {
        stringResource(
            R.string.progression_last,
            "${suggestion.lastWeight} $unitLabel",
            suggestion.lastReps
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.1f))
            .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = lastWorkoutText,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = suggestionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = chipColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * Number input dialog with +/- buttons for quick adjustments and a number keyboard.
 * Industry standard pattern for gym apps - large touch targets, clear visual feedback.
 */
@Composable
private fun NumberInputDialog(
    title: String,
    value: String,
    stepSize: Double,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var currentValue by remember(value) { mutableStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Large number display
                Text(
                    text = if (currentValue.isEmpty()) "0" else currentValue,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreenStart,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                // +/- adjustment buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    // Minus button
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val current = currentValue.toDoubleOrNull() ?: 0.0
                            val newValue = (current - stepSize).coerceAtLeast(0.0)
                            currentValue = if (stepSize >= 1.0) {
                                newValue.toInt().toString()
                            } else {
                                String.format("%.1f", newValue)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    // Manual input field
                    TextField(
                        value = currentValue,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                currentValue = input
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AccentGreenStart,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Plus button
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val current = currentValue.toDoubleOrNull() ?: 0.0
                            val newValue = current + stepSize
                            currentValue = if (stepSize >= 1.0) {
                                newValue.toInt().toString()
                            } else {
                                String.format("%.1f", newValue)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentGreenStart,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValueChange(currentValue)
                    onConfirm()
                },
            ) {
                Text(
                    stringResource(R.string.number_input_done),
                    color = AccentGreenStart,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.number_input_cancel))
            }
        },
        containerColor = Color(0xFF1C1C1E),
    )
}

/**
 * Info tooltip dialog for explaining warmup sets and RPE.
 */
@Composable
private fun InfoTooltipDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Got it",
                    color = AccentGreenStart,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = Color(0xFF1C1C1E),
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailBottomSheet(
    detailState: ExerciseDetailSheetState,
    onDismiss: () -> Unit,
    defaultWeightUnit: UserPreferences.WeightUnit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Exercise Name Header
            Text(
                text = detailState.exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            
            // Exercise Metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExerciseMetadataChip(
                    label = stringResource(R.string.exercise_detail_equipment),
                    value = detailState.exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() },
                )
                ExerciseMetadataChip(
                    label = stringResource(R.string.exercise_detail_muscle_group),
                    value = detailState.exercise.muscleGroup.displayName,
                )
            }
            
            // Description with structured sections
            if (detailState.exercise.description.isNotBlank()) {
                Text(
                    text = stringResource(R.string.exercise_detail_description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                
                val sections = parseExerciseDescription(detailState.exercise.description)
                sections.forEach { (sectionName, content) ->
                    if (content.isNotBlank()) {
                        Text(
                            text = when (sectionName) {
                                "SETUP" -> stringResource(R.string.exercise_detail_setup)
                                "EXECUTION" -> stringResource(R.string.exercise_detail_execution)
                                "TIPS" -> stringResource(R.string.exercise_detail_tips)
                                else -> sectionName
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentCyanStart,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        Text(
                            text = content.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // YouTube Video Link
            detailState.exercise.youtubeUrl?.let { url ->
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        // Open YouTube URL
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentCyanStart,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.exercise_detail_watch_video))
                }
            }
            
            // History Section
            Text(
                text = stringResource(R.string.exercise_detail_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            
            when {
                detailState.isLoadingHistory -> {
                    Text(
                        text = stringResource(R.string.exercise_detail_history_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                detailState.history.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.exercise_detail_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                else -> {
                    detailState.history.forEach { session ->
                        HistorySessionCard(
                            session = session,
                            weightUnit = defaultWeightUnit,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExerciseMetadataChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
        )
    }
}

@Composable
private fun HistorySessionCard(
    session: ExerciseHistorySessionUi,
    weightUnit: UserPreferences.WeightUnit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = session.date,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AccentGreenStart,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        
        session.sets.forEachIndexed { index, set ->
            val weight = if (weightUnit == UserPreferences.WeightUnit.LBS) {
                set.weight * 2.20462
            } else {
                set.weight
            }
            
            val setInfo = if (set.rpe != null) {
                stringResource(
                    R.string.exercise_detail_set_format_with_rpe,
                    set.reps,
                    weight,
                    set.rpe
                )
            } else {
                stringResource(
                    R.string.exercise_detail_set_format,
                    set.reps,
                    weight
                )
            }
            
            Text(
                text = "Set ${index + 1}: $setInfo",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

private fun parseExerciseDescription(description: String): List<Pair<String, String>> {
    val sections = mutableListOf<Pair<String, String>>()
    val sectionPattern = "##(\\w+)##".toRegex()
    
    val matches = sectionPattern.findAll(description).toList()
    
    if (matches.isEmpty()) {
        // No structured format, return the whole description as-is
        return listOf("" to description)
    }
    
    matches.forEachIndexed { index, match ->
        val sectionName = match.groupValues[1]
        val startIndex = match.range.last + 1
        val endIndex = if (index < matches.size - 1) {
            matches[index + 1].range.first
        } else {
            description.length
        }
        
        val content = description.substring(startIndex, endIndex).trim()
        sections.add(sectionName to content)
    }
    
    return sections
}

