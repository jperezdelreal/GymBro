package com.gymbro.feature.settings

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
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.preferences.UserPreferences.WeightUnit

private val AccentGreen = Color(0xFF00FF87)
private val CardBackground = Color(0xFF1E1E1E)
private val SurfaceDark = Color(0xFF121212)

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> { /* Show snackbar */ }
                is SettingsEffect.ShowError -> { /* Show error snackbar */ }
                is SettingsEffect.NavigateBack -> onNavigateBack()
                is SettingsEffect.OpenUrl -> { /* Open URL in browser */ }
                is SettingsEffect.OpenHealthConnectSettings -> { /* Open Health Connect */ }
            }
        }
    }

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                ),
            )
        },
        containerColor = SurfaceDark,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Account Section
            SectionTitle("Account")
            SettingsCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "Sign In Status",
                        subtitle = "Manage your account",
                        iconTint = AccentGreen,
                        onClick = { },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = "Clear All Data",
                        subtitle = "Reset app to defaults",
                        iconTint = Color(0xFFFF5252),
                        onClick = { showClearDataDialog = true },
                    )
                }
            }

            // Workout Section
            SectionTitle("Workout Preferences")
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
                                    text = "Weight Unit",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Display weights in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E),
                                )
                            }
                        }
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = state.weightUnit == WeightUnit.KG,
                                onClick = { onEvent(SettingsEvent.SetWeightUnit(WeightUnit.KG)) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) {
                                Text("kg", style = MaterialTheme.typography.labelMedium)
                            }
                            SegmentedButton(
                                selected = state.weightUnit == WeightUnit.LBS,
                                onClick = { onEvent(SettingsEvent.SetWeightUnit(WeightUnit.LBS)) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) {
                                Text("lbs", style = MaterialTheme.typography.labelMedium)
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
                                    text = "Default Rest Timer",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Time between sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E),
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
                                text = "${state.defaultRestTimer}s",
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
                        title = "Auto-Start Rest Timer",
                        subtitle = "Start timer after completing a set",
                        checked = state.autoStartRestTimer,
                        onCheckedChange = { onEvent(SettingsEvent.SetAutoStartRestTimer(it)) },
                    )
                }
            }

            // Notifications Section
            SectionTitle("Notifications")
            SettingsCard {
                SettingsToggle(
                    icon = Icons.Default.Notifications,
                    title = "Workout Reminders",
                    subtitle = "Get reminded to train",
                    checked = state.notificationsEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.SetNotifications(it)) },
                )
            }

            // Health Connect Section
            SectionTitle("Health Connect")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.MonitorHeart,
                    title = if (state.isHealthConnectConnected) "Connected" else "Not Connected",
                    subtitle = if (state.isHealthConnectAvailable) {
                        "Sync with Health Connect"
                    } else {
                        "Health Connect not available"
                    },
                    iconTint = if (state.isHealthConnectConnected) AccentGreen else Color(0xFF9E9E9E),
                    onClick = { if (state.isHealthConnectAvailable) onEvent(SettingsEvent.OpenHealthConnect) },
                    enabled = state.isHealthConnectAvailable,
                )
            }

            // About Section
            SectionTitle("About")
            SettingsCard {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = state.appVersion,
                        iconTint = AccentGreen,
                        onClick = { },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Feedback,
                        title = "Send Feedback",
                        subtitle = "Report issues or suggest features",
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
                    text = "Clear All Data?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "This will reset all settings to defaults. This action cannot be undone.",
                    color = Color(0xFF9E9E9E),
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
                    Text("Clear Data", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = Color.White)
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
        color = Color(0xFF9E9E9E),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
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
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon, iconTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) Color.White else Color(0xFF757575),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
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
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
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
