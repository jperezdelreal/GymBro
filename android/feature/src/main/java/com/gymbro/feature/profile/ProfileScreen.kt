package com.gymbro.feature.profile

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
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
) {
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
        )

        Spacer(modifier = Modifier.height(24.dp))

        // AI Coach Button
        GradientButton(
            text = "Hablar con AI Coach",
            onClick = onNavigateToCoach,
            modifier = Modifier.fillMaxWidth(),
            gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Account Settings Group (Green Accent)
        SettingsGroup(
            title = "Cuenta",
            accentColor = AccentGreenStart,
        ) {
            if (state.isSignedIn) {
                SettingItem(
                    icon = Icons.Default.Person,
                    label = state.user?.displayName ?: "Cuenta anónima",
                    iconTint = AccentGreenStart,
                    onClick = { /* Navigate to account details */ },
                )
                SettingItem(
                    icon = Icons.Default.Cloud,
                    label = "Sincronización en la nube",
                    subtitle = getSyncStatusText(state.syncStatus),
                    iconTint = AccentGreenStart,
                    onClick = { /* Navigate to sync settings */ },
                )
                SettingItemWithToggle(
                    icon = Icons.Default.Sync,
                    label = "Sincronización automática",
                    iconTint = AccentGreenStart,
                    checked = state.autoSyncEnabled,
                    onCheckedChange = { onEvent(ProfileEvent.ToggleAutoSync(it)) },
                )
            } else {
                SettingItem(
                    icon = Icons.Default.Person,
                    label = "Iniciar sesión",
                    subtitle = "Guarda tus datos en la nube",
                    iconTint = AccentGreenStart,
                    onClick = { onEvent(ProfileEvent.SignIn) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preferences Group (Cyan Accent)
        SettingsGroup(
            title = "Preferencias",
            accentColor = AccentCyanStart,
        ) {
            SettingItem(
                icon = Icons.Default.Timer,
                label = "Temporizador de descanso",
                subtitle = "2:00 predeterminado",
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.FitnessCenter,
                label = "Unidad de peso",
                subtitle = "lbs",
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.Notifications,
                label = "Notificaciones",
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.MonitorHeart,
                label = "Integración HealthKit",
                iconTint = AccentCyanStart,
                onClick = onNavigateToSettings,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Group (Amber Accent)
        SettingsGroup(
            title = "Datos",
            accentColor = AccentAmberStart,
        ) {
            SettingItem(
                icon = Icons.Default.Storage,
                label = "Exportar datos",
                iconTint = AccentAmberStart,
                onClick = onNavigateToSettings,
            )
            SettingItem(
                icon = Icons.Default.Delete,
                label = "Borrar todos los datos",
                iconTint = AccentRed,
                onClick = onNavigateToSettings,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Group (White Accent)
        SettingsGroup(
            title = "Acerca de",
            accentColor = Color.White,
        ) {
            SettingItem(
                icon = Icons.Default.Info,
                label = "Versión 1.0",
                iconTint = Color.White,
                onClick = { /* Show version info */ },
            )
            SettingItem(
                icon = Icons.Default.Feedback,
                label = "Enviar comentarios",
                iconTint = Color.White,
                onClick = { /* Open feedback */ },
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
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar sesión",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        } else {
            // Sign In Button
            GradientButton(
                text = "Iniciar sesión",
                onClick = { onEvent(ProfileEvent.SignIn) },
                modifier = Modifier.fillMaxWidth(),
                gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
                enabled = !state.isLoading,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Inicia sesión para guardar tus entrenamientos en la nube",
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
                        contentDescription = "Avatar de usuario",
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
                displayName ?: "Levantador Anónimo"
            } else {
                "No has iniciado sesión"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Status
        Text(
            text = if (isSignedIn && isAnonymous) {
                "Cuenta de invitado"
            } else if (isSignedIn) {
                "Conectado"
            } else {
                "Inicia sesión para sincronizar"
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
                value = "42",
                label = "Entrenamientos",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.LocalFireDepartment,
                value = "18",
                label = "Días activos",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.LocalFireDepartment,
                value = "7",
                label = "Racha",
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
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

private fun getSyncStatusText(status: SyncStatus): String {
    return when (status) {
        SyncStatus.IDLE -> "Listo para sincronizar"
        SyncStatus.SYNCING -> "Sincronizando..."
        SyncStatus.SUCCESS -> "Sincronizado"
        SyncStatus.ERROR -> "Error de sincronización"
        SyncStatus.OFFLINE -> "Sin conexión"
        SyncStatus.DISABLED -> "Deshabilitado"
    }
}

