package com.gymbro.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.core.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.ui.theme.OnSurfaceVariant
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.GradientButton
import kotlinx.coroutines.launch

private val AccentGreen = Color(0xFF00FF87)
private val SurfaceVariant = Color(0xFF2C2C2E)

@Composable
fun OnboardingRoute(
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    OnboardingScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 7 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(OnboardingEvent.PageChanged(page))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> TrainingGoalPage(
                        selectedGoal = state.selectedGoal,
                        onGoalSelected = { onEvent(OnboardingEvent.GoalSelected(it)) },
                    )
                    2 -> ExperienceLevelPage(
                        selectedExperience = state.selectedExperience,
                        onExperienceSelected = { onEvent(OnboardingEvent.ExperienceSelected(it)) },
                    )
                    3 -> TrainingFrequencyPage(
                        trainingDays = state.trainingDaysPerWeek,
                        onDaysSelected = { onEvent(OnboardingEvent.TrainingDaysSelected(it)) },
                    )
                    4 -> TrackPage()
                    5 -> ProgressPage()
                    6 -> GetStartedPage(
                        selectedUnit = state.selectedUnit,
                        userName = state.userName,
                        isGeneratingPlan = state.isGeneratingPlan,
                        onUnitSelected = { onEvent(OnboardingEvent.UnitSelected(it)) },
                        onNameChanged = { onEvent(OnboardingEvent.NameChanged(it)) },
                        onComplete = { onEvent(OnboardingEvent.CompleteOnboarding) },
                    )
                }
            }

            // Navigation buttons + page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button (subtle, disabled on first page)
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_back),
                        fontWeight = FontWeight.Medium,
                    )
                }

                PageIndicators(
                    pageCount = 7,
                    currentPage = pagerState.currentPage,
                )

                // Next/Let's Go button (last page shows Let's Go via existing CTA)
                if (pagerState.currentPage < 6) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreenStart,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    // Empty spacer to keep layout balanced on last page
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = AccentGreenStart,
            modifier = Modifier.size(160.dp),
        )
        Spacer(modifier = Modifier.height(40.dp))
        
        // Gradient text for app title
        Text(
            text = stringResource(R.string.onboarding_app_title),
            style = MaterialTheme.typography.displayLarge.copy(
                brush = Brush.linearGradient(
                    colors = listOf(AccentGreenStart, AccentGreenEnd)
                )
            ),
            fontWeight = FontWeight.Bold,
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.onboarding_smart_gym_companion),
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrackPage() {
    OnboardingFeaturePage(
        icon = Icons.Default.Timer,
        title = stringResource(R.string.onboarding_fast_logging_title),
        description = stringResource(R.string.onboarding_fast_logging_description),
    )
}

@Composable
private fun ProgressPage() {
    OnboardingFeaturePage(
        icon = Icons.Default.TrendingUp,
        title = stringResource(R.string.onboarding_track_progress_title),
        description = stringResource(R.string.onboarding_track_progress_description),
    )
}

@Composable
private fun OnboardingFeaturePage(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(100.dp),
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )
    }
}

@Composable
private fun GetStartedPage(
    selectedUnit: WeightUnit,
    userName: String,
    isGeneratingPlan: Boolean,
    onUnitSelected: (WeightUnit) -> Unit,
    onNameChanged: (String) -> Unit,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_get_started_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.onboarding_preferred_units),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UnitCard(
                text = stringResource(R.string.onboarding_unit_kg),
                isSelected = selectedUnit == WeightUnit.KG,
                onClick = { onUnitSelected(WeightUnit.KG) },
                modifier = Modifier.weight(1f),
            )
            UnitCard(
                text = stringResource(R.string.onboarding_unit_lbs),
                isSelected = selectedUnit == WeightUnit.LBS,
                onClick = { onUnitSelected(WeightUnit.LBS) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_your_name),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = onNameChanged,
            placeholder = { Text(stringResource(R.string.onboarding_name_placeholder), color = OnSurfaceVariant) },
            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentGreenStart,
                unfocusedBorderColor = OnSurfaceVariant,
                cursorColor = AccentGreenStart,
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(48.dp))

        GradientButton(
            text = if (isGeneratingPlan) {
                stringResource(R.string.onboarding_generating_plan)
            } else {
                stringResource(R.string.onboarding_lets_go)
            },
            onClick = onComplete,
            enabled = !isGeneratingPlan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("onboarding_start"),
        )
    }
}

@Composable
private fun UnitCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    GlassmorphicCard(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(AccentGreenStart, AccentGreenEnd)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(GlassOverlay, GlassOverlay)
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .then(
                Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun TrainingGoalPage(
    selectedGoal: UserPreferences.TrainingGoal,
    onGoalSelected: (UserPreferences.TrainingGoal) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_training_goal_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_training_goal_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(48.dp))

        GoalCard(
            title = stringResource(R.string.onboarding_goal_strength),
            description = stringResource(R.string.onboarding_goal_strength_desc),
            isSelected = selectedGoal == UserPreferences.TrainingGoal.STRENGTH,
            onClick = { onGoalSelected(UserPreferences.TrainingGoal.STRENGTH) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard(
            title = stringResource(R.string.onboarding_goal_powerlifting),
            description = stringResource(R.string.onboarding_goal_powerlifting_desc),
            isSelected = selectedGoal == UserPreferences.TrainingGoal.POWERLIFTING,
            onClick = { onGoalSelected(UserPreferences.TrainingGoal.POWERLIFTING) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard(
            title = stringResource(R.string.onboarding_goal_hypertrophy),
            description = stringResource(R.string.onboarding_goal_hypertrophy_desc),
            isSelected = selectedGoal == UserPreferences.TrainingGoal.HYPERTROPHY,
            onClick = { onGoalSelected(UserPreferences.TrainingGoal.HYPERTROPHY) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard(
            title = stringResource(R.string.onboarding_goal_general_fitness),
            description = stringResource(R.string.onboarding_goal_general_fitness_desc),
            isSelected = selectedGoal == UserPreferences.TrainingGoal.GENERAL_FITNESS,
            onClick = { onGoalSelected(UserPreferences.TrainingGoal.GENERAL_FITNESS) },
        )
    }
}

@Composable
private fun ExperienceLevelPage(
    selectedExperience: UserPreferences.ExperienceLevel,
    onExperienceSelected: (UserPreferences.ExperienceLevel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_experience_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_experience_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(48.dp))

        GoalCard(
            title = stringResource(R.string.onboarding_experience_beginner),
            description = stringResource(R.string.onboarding_experience_beginner_desc),
            isSelected = selectedExperience == UserPreferences.ExperienceLevel.BEGINNER,
            onClick = { onExperienceSelected(UserPreferences.ExperienceLevel.BEGINNER) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard(
            title = stringResource(R.string.onboarding_experience_intermediate),
            description = stringResource(R.string.onboarding_experience_intermediate_desc),
            isSelected = selectedExperience == UserPreferences.ExperienceLevel.INTERMEDIATE,
            onClick = { onExperienceSelected(UserPreferences.ExperienceLevel.INTERMEDIATE) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard(
            title = stringResource(R.string.onboarding_experience_advanced),
            description = stringResource(R.string.onboarding_experience_advanced_desc),
            isSelected = selectedExperience == UserPreferences.ExperienceLevel.ADVANCED,
            onClick = { onExperienceSelected(UserPreferences.ExperienceLevel.ADVANCED) },
        )
    }
}

@Composable
private fun TrainingFrequencyPage(
    trainingDays: Int,
    onDaysSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_frequency_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_frequency_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FrequencyCard(
                days = 2,
                isSelected = trainingDays == 2,
                onClick = { onDaysSelected(2) },
                modifier = Modifier.weight(1f),
            )
            FrequencyCard(
                days = 3,
                isSelected = trainingDays == 3,
                onClick = { onDaysSelected(3) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FrequencyCard(
                days = 4,
                isSelected = trainingDays == 4,
                onClick = { onDaysSelected(4) },
                modifier = Modifier.weight(1f),
            )
            FrequencyCard(
                days = 5,
                isSelected = trainingDays == 5,
                onClick = { onDaysSelected(5) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FrequencyCard(
                days = 6,
                isSelected = trainingDays == 6,
                onClick = { onDaysSelected(6) },
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(AccentGreenStart, AccentGreenEnd)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(GlassOverlay, GlassOverlay)
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.Black.copy(alpha = 0.8f) else OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FrequencyCard(
    days: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    GlassmorphicCard(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(AccentGreenStart, AccentGreenEnd)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(GlassOverlay, GlassOverlay)
                    )
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_frequency_days, days),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) AccentGreenStart else GlassOverlay
                    ),
            )
        }
    }
}
