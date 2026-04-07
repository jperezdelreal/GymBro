package com.gymbro.feature.profile

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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.sync.service.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AccentGreen = Color(0xFF00FF87)
private val CardBackground = Color(0xFF1E1E1E)
private val SurfaceDark = Color(0xFF121212)

@Composable
fun ProfileRoute(
    onNavigateToSettings: () -> Unit = {},
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
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // User Card
        UserCard(
            isSignedIn = state.isSignedIn,
            displayName = state.user?.displayName,
            isAnonymous = state.user?.isAnonymous ?: true,
            isLoading = state.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sync Status Card
        SyncStatusCard(
            syncStatus = state.syncStatus,
            lastSyncTime = state.lastSyncTime,
            isSignedIn = state.isSignedIn,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-sync Toggle
        if (state.isSignedIn) {
            AutoSyncCard(
                autoSyncEnabled = state.autoSyncEnabled,
                onToggle = { onEvent(ProfileEvent.ToggleAutoSync(it)) },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Actions
        ActionsCard(
            isSignedIn = state.isSignedIn,
            isLoading = state.isLoading,
            isSyncing = state.syncStatus == SyncStatus.SYNCING,
            onSignIn = { onEvent(ProfileEvent.SignIn) },
            onSignOut = { onEvent(ProfileEvent.SignOut) },
            onSyncNow = { onEvent(ProfileEvent.SyncNow) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Button
        OutlinedButton(
            onClick = onNavigateToSettings,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AccentGreen,
            ),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        // Error
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

@Composable
private fun UserCard(
    isSignedIn: Boolean,
    displayName: String?,
    isAnonymous: Boolean,
    isLoading: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AccentGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentGreen,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "User",
                        tint = AccentGreen,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = if (isSignedIn) {
                        displayName ?: "Anonymous Lifter"
                    } else {
                        "Not Signed In"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isSignedIn && isAnonymous) {
                        "Guest account — data syncs to cloud"
                    } else if (isSignedIn) {
                        "Signed in — data syncs to cloud"
                    } else {
                        "Sign in to back up your training data"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                )
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    isSignedIn: Boolean,
) {
    val (icon, label, color) = when (syncStatus) {
        SyncStatus.IDLE -> Triple(Icons.Default.Cloud, "Ready to sync", Color(0xFF9E9E9E))
        SyncStatus.SYNCING -> Triple(Icons.Default.CloudSync, "Syncing…", AccentGreen)
        SyncStatus.SUCCESS -> Triple(Icons.Default.CloudDone, "Synced", AccentGreen)
        SyncStatus.ERROR -> Triple(Icons.Default.CloudOff, "Sync error", Color(0xFFFF5252))
        SyncStatus.OFFLINE -> Triple(Icons.Default.CloudOff, "Offline", Color(0xFFFF9100))
        SyncStatus.DISABLED -> Triple(Icons.Default.CloudOff, "Sync disabled", Color(0xFF9E9E9E))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIcon(icon = icon, tint = color)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cloud Sync",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (!isSignedIn) "Sign in to enable" else label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
                if (lastSyncTime != null) {
                    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    Text(
                        text = "Last: ${formatter.format(Date(lastSyncTime))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF757575),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(tint.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AutoSyncCard(
    autoSyncEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Auto-Sync",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                    Text(
                        text = "Sync after each workout",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                    )
                }
            }

            Switch(
                checked = autoSyncEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentGreen,
                    uncheckedThumbColor = Color(0xFF9E9E9E),
                    uncheckedTrackColor = Color(0xFF2A2A2A),
                ),
            )
        }
    }
}

@Composable
private fun ActionsCard(
    isSignedIn: Boolean,
    isLoading: Boolean,
    isSyncing: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isSignedIn) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black,
                        disabledContainerColor = AccentGreen.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSyncing) "Syncing…" else "Sync Now",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5252),
                    ),
                ) {
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                Button(
                    onClick = onSignIn,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Sign In as Guest",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                Text(
                    text = "Sign in anonymously to enable cloud backup.\nYour training data stays private.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
