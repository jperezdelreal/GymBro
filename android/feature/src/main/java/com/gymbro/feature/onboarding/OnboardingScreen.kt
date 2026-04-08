package com.gymbro.feature.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.ui.theme.GlassOverlay
import com.gymbro.core.ui.theme.OnSurfaceVariant
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.GradientButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
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
                    0 -> WelcomeHeroPage(
                        onStart = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                    1 -> FeaturesShowcasePage(
                        onNext = {
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    2 -> QuickSetupPage(
                        selectedUnit = state.selectedUnit,
                        userName = state.userName,
                        selectedGoal = state.selectedGoal,
                        onUnitSelected = { onEvent(OnboardingEvent.UnitSelected(it)) },
                        onNameChanged = { onEvent(OnboardingEvent.NameChanged(it)) },
                        onGoalSelected = { onEvent(OnboardingEvent.GoalSelected(it)) },
                        onComplete = { onEvent(OnboardingEvent.CompleteOnboarding) },
                    )
                }
            }

            PageIndicators(
                pageCount = 3,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}

// Screen 1 — Welcome Hero Page
@Composable
private fun WelcomeHeroPage(onStart: () -> Unit) {
    var iconVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        iconVisible = true
        delay(300)
        textVisible = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0.5f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "icon_scale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "text_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // Gradient glow effect behind icon
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-120).dp)
                .size(200.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentGreenStart.copy(alpha = 0.4f),
                            AccentGreenEnd.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // Large GymBro icon with animation
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = AccentGreenStart,
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // "GymBro" in gradient text
            Text(
                text = stringResource(R.string.onboarding_app_title),
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentGreenStart, AccentGreenEnd)
                    ),
                    fontSize = 56.sp
                ),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "Tu compañero de gym inteligente",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tagline
            Text(
                text = "Registra. Analiza. Conquista.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = OnSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.weight(0.7f))

            // CTA Button at bottom
            GradientButton(
                text = "Comenzar",
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(textAlpha),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Screen 2 — Features Showcase Page
@Composable
private fun FeaturesShowcasePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "¿Por qué GymBro?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Feature 1: Ultra-Fast Registration
        FeatureCard(
            icon = Icons.Default.Timer,
            iconColor = AccentGreenStart,
            title = "Registro Ultra-Rápido",
            description = "1-2 toques por serie. Sin formularios complejos, solo entrena.",
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feature 2: AI Personal Coach
        FeatureCard(
            icon = Icons.Default.Psychology,
            iconColor = AccentCyanStart,
            title = "IA Coach Personal",
            description = "Consejos basados en tu progreso. Tu asistente inteligente de entrenamiento.",
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feature 3: Smart Analytics
        FeatureCard(
            icon = Icons.Default.BarChart,
            iconColor = AccentAmberStart,
            title = "Analíticas Inteligentes",
            description = "Detecta mesetas y PRs automáticamente. Optimiza tu progreso.",
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        GradientButton(
            text = "Siguiente",
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
) {
    GlassmorphicCard(
        accentColor = iconColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                iconColor.copy(alpha = 0.2f),
                                iconColor.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

// Screen 3 — Quick Setup Page
@Composable
private fun QuickSetupPage(
    selectedUnit: WeightUnit,
    userName: String,
    selectedGoal: String,
    onUnitSelected: (WeightUnit) -> Unit,
    onNameChanged: (String) -> Unit,
    onGoalSelected: (String) -> Unit,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Configuración Rápida",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Name input
        Text(
            text = "¿Cómo te llamas?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = onNameChanged,
            placeholder = { 
                Text(
                    stringResource(R.string.onboarding_name_placeholder), 
                    color = OnSurfaceVariant
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentGreenStart,
                unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f),
                cursorColor = AccentGreenStart,
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Unit selection
        Text(
            text = "Unidad de peso",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectableCard(
                text = "kg",
                isSelected = selectedUnit == WeightUnit.KG,
                onClick = { onUnitSelected(WeightUnit.KG) },
                modifier = Modifier.weight(1f),
            )
            SelectableCard(
                text = "lbs",
                isSelected = selectedUnit == WeightUnit.LBS,
                onClick = { onUnitSelected(WeightUnit.LBS) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Goal selection
        Text(
            text = "Tu objetivo principal",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoalCard(
                text = "💪 Fuerza",
                description = "Levanta más peso",
                isSelected = selectedGoal == "strength",
                onClick = { onGoalSelected("strength") },
            )
            GoalCard(
                text = "🦾 Hipertrofia",
                description = "Gana masa muscular",
                isSelected = selectedGoal == "hypertrophy",
                onClick = { onGoalSelected("hypertrophy") },
            )
            GoalCard(
                text = "⚡ Ambos",
                description = "Fuerza + Volumen",
                isSelected = selectedGoal == "both",
                onClick = { onGoalSelected("both") },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        GradientButton(
            text = "¡Vamos!",
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SelectableCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Border overlay for glassmorphic effect when not selected
        if (!isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
                    .padding(1.dp)
            )
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.White,
        )
    }
}

@Composable
private fun GoalCard(
    text: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
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
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else OnSurfaceVariant,
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
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 300),
                label = "indicator_width"
            )
            
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
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
                    ),
            )
        }
    }
}
