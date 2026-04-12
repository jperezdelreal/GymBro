package com.gymbro.feature.exerciselibrary

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.model.Equipment
import com.gymbro.core.ui.localizedName
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.icon

private val AccentGreen = AccentGreenStart

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ExerciseDetailRoute(
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    ExerciseDetailScreen(
        state = state.value,
        onNavigateBack = onNavigateBack,
        onRetry = { viewModel.onEvent(ExerciseDetailEvent.RetryClicked) },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ExerciseDetailScreen(
    state: ExerciseDetailState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.exercise?.name
                            ?: stringResource(R.string.exercise_detail_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            state.isLoading -> {
                FullScreenLoading(
                    message = stringResource(R.string.exercise_detail_loading),
                    modifier = Modifier.padding(paddingValues),
                )
            }
            state.error != null -> {
                EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.exercise_detail_error_title),
                    subtitle = state.error,
                    actionText = stringResource(R.string.common_retry),
                    onActionClick = onRetry,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            state.exercise != null -> {
                ExerciseDetailContent(
                    exercise = state.exercise,
                    modifier = Modifier.padding(paddingValues),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Exercise Name (heading)
        val nameModifier = if (sharedTransitionScope != null && animatedContentScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "exercise-name-${exercise.id}"),
                    animatedVisibilityScope = animatedContentScope,
                )
            }
        } else {
            Modifier
        }
        
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = AccentGreen,
            modifier = nameModifier.semantics { heading() },
        )

        // Muscle group with icon
        val muscleIcon = exercise.muscleGroup.icon()
        GlassmorphicCard(
            accentColor = muscleGroupColor(exercise.muscleGroup),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = muscleIcon,
                    contentDescription = null,
                    tint = muscleGroupColor(exercise.muscleGroup),
                    modifier = Modifier.size(32.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.exercise_detail_muscle_group),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = exercise.muscleGroup.localizedName(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // Category and Equipment row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Category badge
            GlassmorphicCard(
                modifier = Modifier.weight(1f),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.exercise_detail_category),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CategoryBadge(category = exercise.category)
                }
            }

            // Equipment
            GlassmorphicCard(
                modifier = Modifier.weight(1f),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.exercise_detail_equipment),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = exercise.equipment.localizedName(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }

        // Description
        if (exercise.description.isNotBlank()) {
            val sections = parseExerciseSections(exercise.description)
            sections.forEach { section ->
                ExerciseSectionCard(section = section)
            }
        }

        // YouTube link button
        if (!exercise.youtubeUrl.isNullOrBlank()) {
            val videoContentDesc = stringResource(R.string.cd_watch_exercise_video)
            GlassmorphicCard(
                accentColor = AccentRed,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exercise.youtubeUrl))
                    context.startActivity(intent)
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = videoContentDesc
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.exercise_detail_watch_video),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = AccentRed,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryBadge(category: ExerciseCategory) {
    val gradientColors = when (category) {
        ExerciseCategory.COMPOUND -> listOf(AccentGreenStart, AccentGreenEnd)
        ExerciseCategory.ISOLATION -> listOf(AccentAmberStart, AccentAmberStart.copy(alpha = 0.8f))
        ExerciseCategory.ACCESSORY -> listOf(AccentCyanStart, AccentCyanStart.copy(alpha = 0.8f))
        ExerciseCategory.CARDIO -> listOf(AccentRed, AccentRed.copy(alpha = 0.8f))
    }

    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = category.localizedName().uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

private fun muscleGroupColor(muscleGroup: MuscleGroup): Color = when (muscleGroup) {
    MuscleGroup.CHEST, MuscleGroup.BICEPS, MuscleGroup.TRICEPS -> AccentGreenStart
    MuscleGroup.BACK -> AccentCyanStart
    MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES -> AccentAmberStart
    MuscleGroup.SHOULDERS -> AccentGreenEnd
    MuscleGroup.CORE -> AccentCyanStart
    else -> AccentGreenStart
}

enum class SectionType { SETUP, EXECUTION, TIPS, LEGACY }

private data class ExerciseSection(val type: SectionType, val content: String)

private fun parseExerciseSections(description: String): List<ExerciseSection> {
    if (!description.contains("##SETUP##")) {
        // Legacy format: return as single block
        return listOf(ExerciseSection(SectionType.LEGACY, description))
    }
    
    val sections = mutableListOf<ExerciseSection>()
    val parts = description.split("##")
    
    var i = 0
    while (i < parts.size) {
        val part = parts[i].trim()
        if (part == "SETUP" && i + 1 < parts.size) {
            sections.add(ExerciseSection(SectionType.SETUP, parts[i + 1].trim()))
            i += 2
        } else if (part == "EXECUTION" && i + 1 < parts.size) {
            sections.add(ExerciseSection(SectionType.EXECUTION, parts[i + 1].trim()))
            i += 2
        } else if (part == "TIPS" && i + 1 < parts.size) {
            sections.add(ExerciseSection(SectionType.TIPS, parts[i + 1].trim()))
            i += 2
        } else {
            i++
        }
    }
    
    return sections
}

@Composable
private fun ExerciseSectionCard(section: ExerciseSection) {
    if (section.type == SectionType.LEGACY) {
        // Legacy format: render as-is
        GlassmorphicCard {
            Column {
                Text(
                    text = stringResource(R.string.exercise_detail_description),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = section.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        return
    }
    
    val (emoji, titleRes, accentColor) = when (section.type) {
        SectionType.SETUP -> Triple("🔧", R.string.exercise_section_setup, AccentCyanStart)
        SectionType.EXECUTION -> Triple("💪", R.string.exercise_section_execution, AccentGreenStart)
        SectionType.TIPS -> Triple("💡", R.string.exercise_section_tips, AccentAmberStart)
        else -> Triple("", R.string.exercise_detail_description, AccentGreenStart)
    }
    
    GlassmorphicCard(accentColor = accentColor) {
        Column {
            // Section header with emoji
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = accentColor,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // Section content
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 24.sp,
            )
        }
    }
}
