package com.gymbro.feature.recovery

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.gymbro.core.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.SleepData
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentAmberEnd
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.Background
import com.gymbro.core.ui.theme.OnSurfaceVariant
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.AnimatedProgressCircle
import androidx.compose.ui.graphics.Brush
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AccentGreen = Color(0xFF00FF87)
private val CardBackground = Color(0xFF1E1E1E)
private val SurfaceDark = Color(0xFF121212)

@Composable
fun RecoveryRoute(
    onRequestPermissions: () -> Unit = {},
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecoveryEffect.LaunchPermissionRequest -> onRequestPermissions()
                is RecoveryEffect.ShowError -> { /* handled via state.error */ }
            }
        }
    }

    RecoveryScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun RecoveryScreen(
    state: RecoveryState,
    onEvent: (RecoveryEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recovery_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            if (state.permissionsGranted) {
                IconButton(onClick = { onEvent(RecoveryEvent.RefreshData) }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = AccentGreen,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            !state.healthConnectAvailable -> {
                ManualRecoveryEntryCard(
                    manualEntry = state.manualEntry,
                    onSleepQualityChange = { onEvent(RecoveryEvent.UpdateSleepHours(it)) },
                    onMuscleSorenessChange = { onEvent(RecoveryEvent.UpdateReadinessScore(it)) },
                    onEnergyLevelChange = { },
                    onSave = { onEvent(RecoveryEvent.SaveManualEntry) },
                )
            }
            !state.permissionsGranted -> {
                PermissionRequestCard(onRequestPermissions = {
                    onEvent(RecoveryEvent.RequestPermissions)
                })
            }
            state.isLoading -> {
                FullScreenLoading(message = stringResource(R.string.recovery_loading))
            }
            else -> {
                // Readiness Score Card
                ReadinessScoreCard(
                    score = state.recoveryMetrics.readinessScore,
                    label = state.recoveryMetrics.readinessLabel,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics Row
                MetricsGrid(
                    sleepHours = state.recoveryMetrics.sleepHours,
                    restingHR = state.recoveryMetrics.restingHR,
                    hrv = state.recoveryMetrics.hrv,
                    steps = state.recoveryMetrics.steps,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sleep Chart (last 7 days)
                if (state.sleepHistory.isNotEmpty()) {
                    SleepChartCard(sleepHistory = state.sleepHistory)
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1F1F)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = state.error,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessScoreCard(score: Int, label: String) {
    val gradientColors = when {
        score >= 80 -> listOf(AccentGreenStart, AccentGreenEnd)
        score >= 60 -> listOf(AccentAmberStart, AccentAmberEnd)
        else -> listOf(AccentRed, AccentRed)
    }
    val scoreColor = gradientColors[0]

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.recovery_readiness_score),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large animated progress circle with gradient
            Box(contentAlignment = Alignment.Center, modifier = Modifier.semantics {
                contentDescription = "Readiness score: $score out of 100, $label"
            }) {
                AnimatedProgressCircle(
                    progress = score / 100f,
                    size = 160.dp,
                    strokeWidth = 16.dp,
                    gradientColors = gradientColors,
                    modifier = Modifier.clearAndSetSemantics { }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = scoreColor,
                fontWeight = FontWeight.SemiBold,
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            RecoveryTips(score = score)
        }
    }
}

@Composable
private fun MetricsGrid(
    sleepHours: Double,
    restingHR: Double?,
    hrv: Double?,
    steps: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bedtime,
                label = stringResource(R.string.recovery_sleep_label),
                value = String.format("%.1fh", sleepHours),
                iconTint = Color(0xFF7C4DFF),
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                label = stringResource(R.string.recovery_resting_hr_label),
                value = restingHR?.let { String.format("%.0f bpm", it) } ?: "—",
                iconTint = Color(0xFFFF5252),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MonitorHeart,
                label = stringResource(R.string.recovery_hrv_label),
                value = hrv?.let { String.format("%.0f ms", it) } ?: "—",
                iconTint = Color(0xFF00BCD4),
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsWalk,
                label = stringResource(R.string.recovery_steps_label),
                value = String.format("%,d", steps),
                iconTint = AccentGreen,
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
) {
    GlassmorphicCard(
        modifier = modifier.semantics(mergeDescendants = true) { 
            contentDescription = "$label: $value"
        },
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecoveryTips(score: Int) {
    val tipText = when {
        score >= 80 -> stringResource(R.string.recovery_tip_green)
        score >= 60 -> stringResource(R.string.recovery_tip_amber)
        else -> stringResource(R.string.recovery_tip_red)
    }
    
    val tipColor = when {
        score >= 80 -> AccentGreenStart
        score >= 60 -> AccentAmberStart
        else -> AccentRed
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tipColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "💡 Recovery Tip",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = tipColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tipText,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun SleepChartCard(sleepHistory: List<SleepData>) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.recovery_sleep_chart_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bar chart
            val maxHours = 10f
            val barColor = Color(0xFF7C4DFF)
            val sorted = sleepHistory.sortedBy { it.startTime }.takeLast(7)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val barCount = sorted.size.coerceAtLeast(1)
                val barWidth = (size.width / barCount) * 0.6f
                val spacing = (size.width / barCount) * 0.4f

                sorted.forEachIndexed { index, sleep ->
                    val height = (sleep.durationHours.toFloat() / maxHours) * size.height
                    val x = index * (barWidth + spacing) + spacing / 2

                    // Bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
            }

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                sorted.forEach { sleep ->
                    val dayLabel = sleep.startTime
                        .atZone(ZoneId.systemDefault())
                        .format(dayFormatter)
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualRecoveryEntryCard(
    manualEntry: ManualRecoveryEntry,
    onSleepQualityChange: (Float) -> Unit,
    onMuscleSorenessChange: (Float) -> Unit,
    onEnergyLevelChange: (Float) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Info banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.MonitorHeart,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.recovery_manual_mode_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9E9E9E),
                )
            }
        }

        // Recovery Score Display
        val scoreColor = when {
            manualEntry.recoveryScore >= 70 -> AccentGreenStart
            manualEntry.recoveryScore >= 40 -> AccentAmberStart
            else -> AccentRed
        }

        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.recovery_score_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${manualEntry.recoveryScore}",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )

                Text(
                    text = when {
                        manualEntry.recoveryScore >= 70 -> stringResource(R.string.recovery_status_good)
                        manualEntry.recoveryScore >= 40 -> stringResource(R.string.recovery_status_fair)
                        else -> stringResource(R.string.recovery_status_poor)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = scoreColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Sleep Hours Input
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFF7C4DFF),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.recovery_manual_sleep_hours),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = String.format("%.1f hours", manualEntry.sleepHours),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = manualEntry.sleepHours,
                    onValueChange = onSleepQualityChange,
                    valueRange = 0f..12f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C4DFF),
                        activeTrackColor = Color(0xFF7C4DFF),
                        inactiveTrackColor = Color(0xFF7C4DFF).copy(alpha = 0.3f),
                    ),
                )
            }
        }

        // Readiness Score Slider
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AccentGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.recovery_manual_readiness),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${manualEntry.readinessScore.toInt()}/10 — ${manualEntry.readinessLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = manualEntry.readinessScore,
                    onValueChange = onMuscleSorenessChange,
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGreen,
                        activeTrackColor = AccentGreen,
                        inactiveTrackColor = AccentGreen.copy(alpha = 0.3f),
                    ),
                )
                
                // Readiness labels row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Wrecked",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                    )
                    Text(
                        text = "Tired",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                    )
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                    )
                    Text(
                        text = "Good",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                    )
                    Text(
                        text = "Crushed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = Color.Black,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.recovery_manual_save),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PermissionRequestCard(onRequestPermissions: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.MonitorHeart,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.recovery_permissions_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.recovery_permissions_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.recovery_grant_permissions),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
