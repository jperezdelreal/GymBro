package com.gymbro.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.preferences.UserPreferences.ThemePreference
import com.gymbro.core.preferences.UserPreferences.TrainingPhase
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.core.ui.theme.AccentGreen
import com.gymbro.core.ui.theme.SurfacePrimary
import com.gymbro.core.ui.theme.Surface

private val CardBackground = Surface

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SettingsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SettingsEffect.NavigateBack -> onNavigateBack()
                is SettingsEffect.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
                    context.startActivity(intent)
                }
                is SettingsEffect.OpenHealthConnectSettings -> {
                    try {
                        val intent = context.packageManager.getLaunchIntentForPackage(
                            "com.google.android.apps.healthdata"
                        )
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            val playStoreIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                            )
                            context.startActivity(playStoreIntent)
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.settings_open_health_connect_error)
                        )
                    }
                }
                is SettingsEffect.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showRedoSetupDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val trainingPhaseSelectorDescription = stringResource(R.string.settings_cd_training_phase)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfacePrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfacePrimary,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Appearance Section
            SectionTitle(stringResource(R.string.settings_appearance))
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIcon(Icons.Default.Palette, AccentGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.settings_appearance),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SegmentedButton(
                            selected = state.themePreference == ThemePreference.DARK,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(SettingsEvent.SetThemePreference(ThemePreference.DARK))
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 3),
                        ) {
                            Text(stringResource(R.string.theme_dark), style = MaterialTheme.typography.labelMedium)
                        }
                        SegmentedButton(
                            selected = state.themePreference == ThemePreference.LIGHT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(SettingsEvent.SetThemePreference(ThemePreference.LIGHT))
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 3),
                        ) {
                            Text(stringResource(R.string.theme_light), style = MaterialTheme.typography.labelMedium)
                        }
                        SegmentedButton(
                            selected = state.themePreference == ThemePreference.SYSTEM,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(SettingsEvent.SetThemePreference(ThemePreference.SYSTEM))
                            },
                            shape = SegmentedButtonDefaults.itemShape(2, 3),
                        ) {
                            Text(stringResource(R.string.theme_system), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Account Section
            SectionTitle(stringResource(R.string.settings_section_account))
            SettingsCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.settings_redo_setup),
                        subtitle = stringResource(R.string.settings_redo_setup_subtitle),
                        iconTint = AccentGreen,
                        onClick = { showRedoSetupDialog = true },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.settings_clear_data),
                        subtitle = stringResource(R.string.settings_clear_data_subtitle),
                        iconTint = Color(0xFFFF5252),
                        onClick = { showClearDataDialog = true },
                    )
                }
            }

            // Workout Section
            SectionTitle(stringResource(R.string.settings_section_workout))
            SettingsCard {
                Column {
                    // Weight Unit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIcon(Icons.Default.FitnessCenter, AccentGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_weight_unit),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stringResource(R.string.settings_weight_unit_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = state.weightUnit == WeightUnit.KG,
                                onClick = { onEvent(SettingsEvent.SetWeightUnit(WeightUnit.KG)) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) {
                                Text(stringResource(R.string.settings_weight_unit_kg), style = MaterialTheme.typography.labelMedium)
                            }
                            SegmentedButton(
                                selected = state.weightUnit == WeightUnit.LBS,
                                onClick = { onEvent(SettingsEvent.SetWeightUnit(WeightUnit.LBS)) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) {
                                Text(stringResource(R.string.settings_weight_unit_lbs), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    SettingsDivider()

                    // Training Phase
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIcon(Icons.Default.LocalFireDepartment, AccentGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_training_phase),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stringResource(R.string.settings_training_phase_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = trainingPhaseSelectorDescription },
                        ) {
                            SegmentedButton(
                                selected = state.trainingPhase == TrainingPhase.BULK,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvent(SettingsEvent.SetTrainingPhase(TrainingPhase.BULK))
                                },
                                shape = SegmentedButtonDefaults.itemShape(0, 3),
                            ) {
                                Text(stringResource(R.string.settings_training_phase_bulk), style = MaterialTheme.typography.labelMedium)
                            }
                            SegmentedButton(
                                selected = state.trainingPhase == TrainingPhase.CUT,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvent(SettingsEvent.SetTrainingPhase(TrainingPhase.CUT))
                                },
                                shape = SegmentedButtonDefaults.itemShape(1, 3),
                            ) {
                                Text(stringResource(R.string.settings_training_phase_cut), style = MaterialTheme.typography.labelMedium)
                            }
                            SegmentedButton(
                                selected = state.trainingPhase == TrainingPhase.MAINTENANCE,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvent(SettingsEvent.SetTrainingPhase(TrainingPhase.MAINTENANCE))
                                },
                                shape = SegmentedButtonDefaults.itemShape(2, 3),
                            ) {
                                Text(stringResource(R.string.settings_training_phase_maintenance), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    SettingsDivider()

                    // Default Rest Timer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIcon(Icons.Default.Timer, AccentGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_default_rest_timer),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stringResource(R.string.settings_rest_timer_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val newValue = maxOf(30, state.defaultRestTimer - 15)
                                onEvent(SettingsEvent.SetDefaultRestTimer(newValue))
                            }) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            Text(
                                text = stringResource(R.string.settings_rest_timer_value, state.defaultRestTimer),
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(60.dp),
                            )
                            TextButton(onClick = {
                                val newValue = minOf(300, state.defaultRestTimer + 15)
                                onEvent(SettingsEvent.SetDefaultRestTimer(newValue))
                            }) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }

                    SettingsDivider()

                    // Auto-start Rest Timer
                    SettingsToggle(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.settings_auto_start_rest_timer),
                        subtitle = stringResource(R.string.settings_auto_start_rest_timer_subtitle),
                        checked = state.autoStartRestTimer,
                        onCheckedChange = { onEvent(SettingsEvent.SetAutoStartRestTimer(it)) },
                    )
                }
            }

            // Notifications Section
            SectionTitle(stringResource(R.string.settings_section_notifications))
            SettingsCard {
                SettingsToggle(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_workout_reminders),
                    subtitle = stringResource(R.string.settings_workout_reminders_subtitle),
                    checked = state.notificationsEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.SetNotifications(it)) },
                )
            }

            // Health Connect Section
            SectionTitle(stringResource(R.string.settings_section_health_connect))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.MonitorHeart,
                    title = if (state.isHealthConnectConnected) stringResource(R.string.settings_health_connect_connected) else stringResource(R.string.settings_health_connect_not_connected),
                    subtitle = if (state.isHealthConnectAvailable) {
                        stringResource(R.string.settings_health_connect_sync)
                    } else {
                        stringResource(R.string.settings_health_connect_unavailable)
                    },
                    iconTint = if (state.isHealthConnectConnected) AccentGreen else Color(0xFF9E9E9E),
                    onClick = { if (state.isHealthConnectAvailable) onEvent(SettingsEvent.OpenHealthConnect) },
                    enabled = state.isHealthConnectAvailable,
                )
            }

            // About Section
            SectionTitle(stringResource(R.string.settings_section_about))
            SettingsCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_app_version),
                        subtitle = state.appVersion,
                        iconTint = AccentGreen,
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Feedback,
                        title = stringResource(R.string.settings_send_feedback),
                        subtitle = stringResource(R.string.settings_send_feedback_subtitle),
                        iconTint = AccentGreen,
                        onClick = { onEvent(SettingsEvent.SendFeedback) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = CardBackground,
            title = {
                Text(
                    text = stringResource(R.string.settings_clear_data_confirm_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_clear_data_confirm_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(SettingsEvent.ClearAllData)
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                    ),
                ) {
                    Text(stringResource(R.string.settings_clear_data_button), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
    }

    if (showRedoSetupDialog) {
        AlertDialog(
            onDismissRequest = { showRedoSetupDialog = false },
            containerColor = CardBackground,
            title = {
                Text(
                    text = stringResource(R.string.settings_redo_setup_confirm_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_redo_setup_confirm_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(SettingsEvent.RedoSetup)
                        showRedoSetupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                    ),
                ) {
                    Text(stringResource(R.string.settings_redo_setup_confirm), color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRedoSetupDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).semantics { heading() }
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(tint.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val modifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) Modifier.clickable(enabled = enabled, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            })
            else Modifier
        )
        .padding(vertical = 8.dp)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon, iconTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon, AccentGreen)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0xFF2A2A2A),
            ),
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF2A2A2A))
            .padding(vertical = 4.dp),
    )
}
