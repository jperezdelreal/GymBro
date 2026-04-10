package com.gymbro.feature.exerciselibrary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentCyanEnd
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentAmberEnd
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.ObserveErrors
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.icon
import com.gymbro.core.R

// Backwards compatibility
private val AccentGreen = AccentGreenStart
private val AccentCyan = AccentCyanStart
private val AccentAmber = AccentAmberStart

@Composable
fun ExerciseLibraryRoute(
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCreateExercise: () -> Unit = {},
    onExercisePicked: ((Exercise) -> Unit)? = null,
    isPickerMode: Boolean = false,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(
        errorFlow = viewModel.errorEvents,
        snackbarHostState = snackbarHostState
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExerciseLibraryEffect.NavigateToDetail -> {
                    onNavigateToDetail(effect.exerciseId)
                }
            }
        }
    }

    ExerciseLibraryScreen(
        state = state.value,
        onEvent = { event ->
            if (isPickerMode && event is ExerciseLibraryEvent.ExerciseClicked) {
                onExercisePicked?.invoke(event.exercise)
            } else {
                viewModel.onEvent(event)
            }
        },
        onNavigateToCreateExercise = onNavigateToCreateExercise,
        isPickerMode = isPickerMode,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    state: ExerciseLibraryState,
    onEvent: (ExerciseLibraryEvent) -> Unit,
    onNavigateToCreateExercise: () -> Unit = {},
    isPickerMode: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Title Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(if (isPickerMode) R.string.exercise_library_pick_title else R.string.exercise_library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .semantics { heading() }
                )
                if (!isPickerMode) {
                    IconButton(
                        onClick = onNavigateToCreateExercise,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.exercise_library_create),
                            tint = AccentGreen,
                        )
                    }
                }
            }
            // Search bar - Glassmorphic style
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = GlassOverlay,
                border = BorderStroke(1.dp, GlassBorder),
            ) {
                TextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(ExerciseLibraryEvent.SearchQueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth().testTag("search_bar"),
                    placeholder = {
                        Text(stringResource(R.string.exercise_library_search), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentGreenStart,
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AccentGreenStart,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            // Muscle group filter chips with gradient when selected
            val filterGroups = listOf(
                MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS,
                MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.CORE,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                filterGroups.forEach { group ->
                    val isSelected = state.selectedMuscleGroup == group
                    GradientFilterChip(
                        selected = isSelected,
                        onClick = {
                            onEvent(
                                ExerciseLibraryEvent.MuscleGroupSelected(
                                    if (isSelected) null else group,
                                ),
                            )
                        },
                        label = group.displayName,
                        icon = group.icon(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            when {
                state.isLoading -> {
                    FullScreenLoading(message = stringResource(R.string.exercise_library_loading))
                }
                state.exercises.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.exercise_library_empty_title),
                        subtitle = stringResource(R.string.exercise_library_empty_subtitle),
                        actionText = stringResource(R.string.exercise_library_empty_cta),
                        onActionClick = onNavigateToCreateExercise,
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.exercises,
                            key = { it.id.toString() },
                        ) { exercise ->
                            ExerciseCard(
                                exercise = exercise,
                                onClick = { onEvent(ExerciseLibraryEvent.ExerciseClicked(exercise)) },
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun GradientFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else if (isPressed) 0.95f else 1.0f,
        label = "chip_scale"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.Transparent else GlassOverlay,
        border = if (selected) null else BorderStroke(1.dp, GlassBorder),
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (selected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(AccentGreenStart, AccentGreenEnd)
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        label = "card_scale"
    )

    // Accent color based on muscle group
    val accentColor = when (exercise.muscleGroup) {
        MuscleGroup.CHEST, MuscleGroup.BICEPS, MuscleGroup.TRICEPS -> AccentGreenStart
        MuscleGroup.BACK -> AccentCyanStart
        MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES -> AccentAmberStart
        MuscleGroup.SHOULDERS -> AccentGreenEnd
        MuscleGroup.CORE -> AccentCyanEnd
        else -> AccentGreenStart
    }

    GlassmorphicCard(
        modifier = Modifier
            .scale(scale)
            .clickable {
                isPressed = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .testTag("exercise_card"),
        accentColor = accentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBadge(category = exercise.category)
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.exercise_library_view_details),
                tint = accentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    // Reset pressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun CategoryBadge(category: ExerciseCategory) {
    val gradientColors = when (category) {
        ExerciseCategory.COMPOUND -> listOf(AccentGreenStart, AccentGreenEnd)
        ExerciseCategory.ISOLATION -> listOf(AccentAmberStart, AccentAmberEnd)
        ExerciseCategory.ACCESSORY -> listOf(AccentCyanStart, AccentCyanEnd)
        ExerciseCategory.CARDIO -> listOf(AccentRed, AccentRed.copy(alpha = 0.8f))
    }

    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = category.displayName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}


