package com.gymbro.feature.workout

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gymbro.core.R
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.voice.ParsedVoiceInput
import com.gymbro.core.voice.VoiceInputParser
import com.gymbro.core.voice.VoiceRecognitionService
import com.gymbro.core.voice.VoiceRecognitionState
import kotlinx.coroutines.launch

private val AccentRed = Color(0xFFCF6679)

@Composable
fun VoiceInputButton(
    voiceRecognitionService: VoiceRecognitionService,
    defaultWeightUnit: UserPreferences.WeightUnit,
    onVoiceResult: (ParsedVoiceInput) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val parser = remember { VoiceInputParser() }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }

    // Clean up SpeechRecognizer when composable leaves composition
    DisposableEffect(voiceRecognitionService) {
        onDispose {
            voiceRecognitionService.destroy()
        }
    }

    val hasPermission = remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission.value = isGranted
        if (isGranted) {
            scope.launch {
                collectVoiceResults(
                    voiceRecognitionService, parser, defaultWeightUnit,
                    onListening = { isListening = true },
                    onResult = { isListening = false; onVoiceResult(it) },
                    onError = { isListening = false; onError(it) },
                )
            }
        } else {
            val activity = context.findActivity()
            if (activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            ) {
                showSettingsDialog = true
            } else {
                onError(context.getString(R.string.voice_input_permission_denied))
            }
        }
    }

    // Rationale dialog — shown before requesting permission
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text(stringResource(R.string.voice_input_permission_rationale_title)) },
            text = { Text(stringResource(R.string.voice_input_permission_rationale_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Settings dialog — shown when user permanently denied permission
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(stringResource(R.string.voice_input_permission_rationale_title)) },
            text = { Text(stringResource(R.string.voice_input_permission_denied_permanently)) },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.voice_input_permission_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Pulsing animation when listening
        if (isListening) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(scale)
                    .alpha(pulseAlpha)
                    .background(AccentRed, CircleShape)
            )
        }

        IconButton(
            onClick = {
                // Debounce rapid taps — ignore taps within 500ms
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 500L) return@IconButton
                lastTapTime = now

                if (isListening) {
                    voiceRecognitionService.stopListening()
                    isListening = false
                    return@IconButton
                }

                if (!voiceRecognitionService.isAvailable()) {
                    onError(context.getString(R.string.voice_input_not_available))
                    return@IconButton
                }

                if (hasPermission.value) {
                    scope.launch {
                        collectVoiceResults(
                            voiceRecognitionService, parser, defaultWeightUnit,
                            onListening = { isListening = true },
                            onResult = { isListening = false; onVoiceResult(it) },
                            onError = { isListening = false; onError(it) },
                        )
                    }
                } else {
                    val activity = context.findActivity()
                    if (activity != null &&
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
                    ) {
                        showRationaleDialog = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.voice_input_button),
                tint = if (isListening) AccentRed else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private suspend fun collectVoiceResults(
    voiceRecognitionService: VoiceRecognitionService,
    parser: VoiceInputParser,
    defaultWeightUnit: UserPreferences.WeightUnit,
    onListening: () -> Unit,
    onResult: (ParsedVoiceInput) -> Unit,
    onError: (String) -> Unit,
) {
    voiceRecognitionService.startListening().collect { state ->
        when (state) {
            is VoiceRecognitionState.Listening -> onListening()
            is VoiceRecognitionState.Result -> {
                val parsed = parser.parse(state.transcript, defaultWeightUnit)
                if (parsed != null) {
                    onResult(parsed)
                } else {
                    onError("Couldn't parse: \"${state.transcript}\"")
                }
            }
            is VoiceRecognitionState.Error -> onError(state.message)
            is VoiceRecognitionState.Idle -> {}
        }
    }
}

private fun Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
