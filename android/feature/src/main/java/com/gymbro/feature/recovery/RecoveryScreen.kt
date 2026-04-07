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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.SleepData
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
            .background(SurfaceDark)
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
                text = "Recovery",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (state.permissionsGranted) {
                IconButton(onClick = { onEvent(RecoveryEvent.RefreshData) }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = AccentGreen,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            !state.healthConnectAvailable -> {
                HealthConnectUnavailableCard()
            }
            !state.permissionsGranted -> {
                PermissionRequestCard(onRequestPermissions = {
                    onEvent(RecoveryEvent.RequestPermissions)
                })
            }
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentGreen)
                }
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
    val scoreColor = when {
        score >= 80 -> AccentGreen
        score >= 60 -> Color(0xFFFFD600)
        score >= 40 -> Color(0xFFFF9100)
        else -> Color(0xFFFF5252)
    }

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
            Text(
                text = "Readiness Score",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF9E9E9E),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular score indicator
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    // Background arc
                    drawArc(
                        color = Color(0xFF2A2A2A),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx()),
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                        size = Size(
                            size.width - 24.dp.toPx(),
                            size.height - 24.dp.toPx(),
                        ),
                    )
                    // Score arc
                    drawArc(
                        color = scoreColor,
                        startAngle = 135f,
                        sweepAngle = 270f * (score / 100f),
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx()),
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                        size = Size(
                            size.width - 24.dp.toPx(),
                            size.height - 24.dp.toPx(),
                        ),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = scoreColor,
                fontWeight = FontWeight.SemiBold,
            )
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
                label = "Sleep",
                value = String.format("%.1fh", sleepHours),
                iconTint = Color(0xFF7C4DFF),
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                label = "Resting HR",
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
                label = "HRV",
                value = hrv?.let { String.format("%.0f ms", it) } ?: "—",
                iconTint = Color(0xFF00BCD4),
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsWalk,
                label = "Steps",
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                color = Color(0xFF9E9E9E),
            )
        }
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
                text = "Sleep — Last 7 Days",
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
private fun HealthConnectUnavailableCard() {
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
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Health Connect Not Available",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Install Health Connect from the Play Store to track your recovery metrics.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
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
                text = "Connect Your Health Data",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GymBro uses sleep, heart rate, and step data to calculate your recovery readiness and optimize training.",
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
                    text = "Grant Permissions",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
