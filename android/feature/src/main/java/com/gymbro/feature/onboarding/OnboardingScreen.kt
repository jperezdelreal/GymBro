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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.core.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.ui.theme.OnSurfaceVariant
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.GradientButton

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
    val pagerState = rememberPagerState(pageCount = { 4 })

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
                    1 -> TrackPage()
                    2 -> ProgressPage()
                    3 -> GetStartedPage(
                        selectedUnit = state.selectedUnit,
                        userName = state.userName,
                        onUnitSelected = { onEvent(OnboardingEvent.UnitSelected(it)) },
                        onNameChanged = { onEvent(OnboardingEvent.NameChanged(it)) },
                        onComplete = { onEvent(OnboardingEvent.CompleteOnboarding) },
                    )
                }
            }

            PageIndicators(
                pageCount = 4,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 32.dp),
            )
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
            modifier = Modifier.fillMaxWidth(),
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
            text = stringResource(R.string.onboarding_lets_go),
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
