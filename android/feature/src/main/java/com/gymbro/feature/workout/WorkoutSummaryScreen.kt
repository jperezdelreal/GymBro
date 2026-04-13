package com.gymbro.feature.workout

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.R
import com.gymbro.core.ui.theme.AccentGreen
import com.gymbro.core.ui.theme.AccentCyan
import com.gymbro.core.ui.theme.AccentAmber
import com.gymbro.core.ui.theme.Surface
import com.gymbro.core.ui.theme.Background
import com.gymbro.feature.common.ConfettiOverlay
import com.gymbro.feature.common.PRCelebration
import kotlinx.coroutines.delay

@Composable
fun WorkoutSummaryScreen(
    durationSeconds: Long,
    totalVolume: Double,
    totalSets: Int,
    exerciseCount: Int,
    personalRecords: List<PersonalRecord>,
    weightUnitLabel: String,
    nextWorkoutName: String? = null,
    onDone: () -> Unit,
){
    var showCelebration by remember { mutableStateOf(personalRecords.isNotEmpty()) }
    var showCompletionConfetti by remember { mutableStateOf(true) }
    val view = LocalView.current

    LaunchedEffect(Unit) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        delay(3500)
        showCompletionConfetti = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Checkmark
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.workout_summary_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.workout_summary_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.workout_summary_motivation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentGreen,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.common_duration),
                    value = formatSummaryDuration(durationSeconds),
                    color = AccentCyan,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    icon = Icons.Default.FitnessCenter,
                    label = stringResource(R.string.common_volume),
                    value = "${totalVolume.toLong()} $weightUnitLabel",
                    color = AccentGreen,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    icon = Icons.Default.BarChart,
                    label = stringResource(R.string.workout_sets),
                    value = "$totalSets",
                    color = AccentAmber,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    icon = Icons.Default.Star,
                    label = stringResource(R.string.workout_exercises),
                    value = "$exerciseCount",
                    color = AccentGreen,
                    modifier = Modifier.weight(1f),
                )
            }

            if (personalRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SummaryCard(
                    icon = Icons.Default.Star,
                    label = stringResource(R.string.history_prs),
                    value = "${personalRecords.size}",
                    color = AccentAmber,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (nextWorkoutName != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.workout_summary_next_hint, nextWorkoutName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(stringResource(R.string.workout_summary_done), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (showCompletionConfetti) {
            ConfettiOverlay(
                modifier = Modifier.fillMaxSize(),
                useLottie = true,
            )
        }

        if (showCelebration) {
            PRCelebration(
                personalRecords = personalRecords,
                onDismiss = { showCelebration = false },
            )
        }
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

private fun formatSummaryDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
