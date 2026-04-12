package com.gymbro.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.gymbro.core.sync.service.SyncStatus
import com.gymbro.core.R
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.Background
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.GradientButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileRoute(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onNavigateToExerciseLibrary: () -> Unit = {},
    onNavigateToTools: () -> Unit = {},
    onNavigateToRecovery: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowError -> { /* handled via state.error */ }
                is ProfileEffect.ShowMessage -> { /* could show snackbar */ }
            }
        }
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToCoach = onNavigateToCoach,
        onNavigateToExerciseLibrary = onNavigateToExerciseLibrary,
        onNavigateToTools = onNavigateToTools,
        onNavigateToRecovery = onNavigateToRecovery,
        onNavigateToProgress = onNavigateToProgress,
        onNavigateToAnalytics = onNavigateToAnalytics,
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onNavigateToExerciseLibrary: () -> Unit = {},
    onNavigateToTools: () -> Unit = {},
    onNavigateToRecovery: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Profile Header with Avatar and Stats
        ProfileHeader(
            isSignedIn = state.isSignedIn,
            displayName = state.user?.displayName,
            isAnonymous = state.user?.isAnonymous ?: true,
            isLoading = state.isLoading,
            totalWorkouts = state.totalWorkouts,
            activeDays = state.activeDays,
            currentStreak = state.currentStreak,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // AI Coach Button
        GradientButton(
            text = stringResource(R.string.profile_talk_to_coach),
            onClick = onNavigateToCoach,
            modifier = Modifier.fillMaxWidth(),
            gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress & Analytics Group (Amber Accent)
        SettingsGroup(
            title = stringResource(R.string.profile_progress_analytics),
            accentColor = AccentAmberStart,
        ) {
            SettingItem(
                icon = Icons.Default.ShowChart,
                label = stringResource(R.string.profile_progress),
                subtitle = stringResource(R.string.profile_progress_subtitle),
                iconTint = AccentAmberStart,
                onClick = onNavigateToProgress,
            )
            SettingItem(
                icon = Icons.Default.BarChart,
                label = stringResource(R.string.profile_analytics),
                subtitle = stringResource(R.string.profile_analytics_subtitle),
                iconTint = AccentAmberStart,
                onClick = onNavigateToAnalytics,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Settings Group (Green Accent)
        SettingsGroup(
            title = stringResource(R.string.profile_account),
            accentColor = AccentGreenStart,
        ) {
            if (state.isSignedIn) {
                SettingItem(
                    icon = Icons.Default.Person,
                    label = state.user?.displayName ?: stringResource(R.string.profile_anonymous_account),
                    subtitle = stringResource(R.string.profile_coming_soon),
                    iconTint = AccentGreenStart,
                    onClick = { },
                    enabled = false,
                )
                SettingItem(
                    icon = Icons.Default.Cloud,
                    label = stringResource(R.string.profile_cloud_sync_title),
                    subtitle = getSyncStatusText(state.syncStatus),
                    iconTint = AccentGreenStart,
                    onClick = { },
                    enabled = false,
                )
                SettingItemWithToggle(
                    icon = Icons.Default.Sync,
                    label = stringResource(R.string.profile_auto_sync_title),
                    iconTint = AccentGreenStart,
                    checked = state.autoSyncEnabled,
                    onCheckedChange = { onEvent(ProfileEvent.ToggleAutoSync(it)) },
                )
            } else {
                SettingItem(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.profile_sign_in_label),
                    subtitle = stringResource(R.string.profile_sign_in_subtitle_alt),
                    iconTint = AccentGreenStart,
                    onClick = { onEvent(ProfileEvent.SignIn) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preferences Group (Cyan Accent)
        SettingsGroup(
            title = stringResource(R.string.profile_preferences),
            accentColor = AccentCyanStart,
        ) {
            SettingItem(
                icon = Icons.Default.Timer,
                label = stringResource(R.string.profile_rest_timer),
                subtitle = stringResource(R.string.profile_rest_timer_default),
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.profile_weight_unit_label),
                subtitle = stringResource(R.string.profile_weight_unit_lbs),
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.LocalFireDepartment,
                label = stringResource(R.string.profile_training_phase),
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.Notifications,
                label = stringResource(R.string.profile_notifications),
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.MonitorHeart,
                label = stringResource(R.string.profile_health_integration),
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exercise Library
        SettingsGroup(
            title = stringResource(R.string.profile_exercise_library),
            accentColor = AccentGreenStart,
        ) {
            SettingItem(
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.profile_exercise_library),
                subtitle = stringResource(R.string.profile_exercise_library_subtitle),
                iconTint = AccentGreenStart,
                onClick = onNavigateToExerciseLibrary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tools
        SettingsGroup(
            title = stringResource(R.string.tools_title),
            accentColor = AccentAmberStart,
        ) {
            SettingItem(
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.tools_title),
                subtitle = stringResource(R.string.tools_subtitle),
                iconTint = AccentAmberStart,
                onClick = onNavigateToTools,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recovery
        SettingsGroup(
            title = stringResource(R.string.profile_recovery),
            accentColor = AccentCyanStart,
        ) {
            SettingItem(
                icon = Icons.Default.MonitorHeart,
                label = stringResource(R.string.profile_recovery),
                subtitle = stringResource(R.string.profile_recovery_subtitle),
                iconTint = AccentCyanStart,
                onClick = onNavigateToRecovery,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Group (Amber Accent)
        SettingsGroup(
            title = stringResource(R.string.profile_data_section),
            accentColor = AccentAmberStart,
        ) {
            SettingItem(
                icon = Icons.Default.Storage,
                label = stringResource(R.string.profile_export_data),
                iconTint = AccentAmberStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.profile_clear_all_data),
                iconTint = AccentRed,
                onClick = onNavigateToSettings,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Group (White Accent)
        SettingsGroup(
            title = stringResource(R.string.profile_about),
            accentColor = Color.White,
        ) {
            SettingItem(
                icon = Icons.Default.Info,
                label = stringResource(R.string.profile_version),
                subtitle = stringResource(R.string.profile_coming_soon),
                iconTint = Color.White,
                onClick = { },
                enabled = false,
            )
            SettingItem(
                icon = Icons.Default.Feedback,
                label = stringResource(R.string.profile_send_feedback_label),
                iconTint = Color.White,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:feedback@gymbro.app")
                        putExtra(Intent.EXTRA_SUBJECT, "GymBro Feedback")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button (if signed in)
        if (state.isSignedIn) {
            OutlinedButton(
                onClick = { onEvent(ProfileEvent.SignOut) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentRed,
                ),
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = stringResource(R.string.profile_sign_out_description),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.profile_sign_out_description),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        } else {
            // Sign In Button
            GradientButton(
                text = stringResource(R.string.profile_sign_in_button_text),
                onClick = { onEvent(ProfileEvent.SignIn) },
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
                enabled = !state.isLoading,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.profile_sign_in_message),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileHeader(
    isSignedIn: Boolean,
    displayName: String?,
    isAnonymous: Boolean,
    isLoading: Boolean,
    totalWorkouts: Int,
    activeDays: Int,
    currentStreak: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Large Avatar with Gradient Border Ring
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Gradient border ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(AccentGreenStart, AccentCyanStart, AccentAmberStart)
                        ),
                        shape = CircleShape
                    )
            )
            
            // Avatar background
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(AccentGreenStart.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentGreenStart,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_user_avatar),
                        tint = AccentGreenStart,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // User Name
        Text(
            text = if (isSignedIn) {
                displayName ?: stringResource(R.string.profile_anonymous_lifter_alt)
            } else {
                stringResource(R.string.profile_not_signed_in_alt)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Status
        Text(
            text = if (isSignedIn && isAnonymous) {
                stringResource(R.string.profile_guest_account_alt)
            } else if (isSignedIn) {
                stringResource(R.string.profile_connected)
            } else {
                stringResource(R.string.profile_sign_in_to_sync)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9E9E9E),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row in small GlassmorphicCards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                icon = Icons.Default.FitnessCenter,
                value = totalWorkouts.toString(),
                label = stringResource(R.string.profile_workouts_label),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.LocalFireDepartment,
                value = activeDays.toString(),
                label = stringResource(R.string.profile_active_days),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.LocalFireDepartment,
                value = currentStreak.toString(),
                label = stringResource(R.string.profile_streak_label),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGreenStart,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        GlassmorphicCard(accentColor = accentColor) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    iconTint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingItemWithToggle(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreenStart,
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0xFF2A2A2A),
            ),
        )
    }
}

@Composable
private fun getSyncStatusText(status: SyncStatus): String {
    return when (status) {
        SyncStatus.IDLE -> stringResource(R.string.profile_sync_ready)
        SyncStatus.SYNCING -> stringResource(R.string.profile_syncing)
        SyncStatus.SUCCESS -> stringResource(R.string.profile_synced)
        SyncStatus.ERROR -> stringResource(R.string.profile_sync_error)
        SyncStatus.OFFLINE -> stringResource(R.string.profile_offline_status)
        SyncStatus.DISABLED -> stringResource(R.string.profile_sync_disabled)
    }
}

