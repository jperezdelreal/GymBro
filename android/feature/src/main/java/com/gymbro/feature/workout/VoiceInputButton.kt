package com.gymbro.feature.workout

import android.Manifest
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
) {
    var isListening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val parser = remember { VoiceInputParser() }
    var hasPermission by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            startVoiceRecognition(
                voiceRecognitionService = voiceRecognitionService,
                parser = parser,
                defaultWeightUnit = defaultWeightUnit,
                onListening = { isListening = true },
                onResult = { result ->
                    isListening = false
                    onVoiceResult(result)
                },
                onError = {
                    isListening = false
                }
            )
        }
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
            val alpha by infiniteTransition.animateFloat(
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
                    .alpha(alpha)
                    .background(AccentRed, CircleShape)
            )
        }

        IconButton(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    startVoiceRecognition(
                        voiceRecognitionService = voiceRecognitionService,
                        parser = parser,
                        defaultWeightUnit = defaultWeightUnit,
                        onListening = { isListening = true },
                        onResult = { result ->
                            isListening = false
                            onVoiceResult(result)
                        },
                        onError = {
                            isListening = false
                        }
                    )
                }
            },
            modifier = Modifier
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice input",
                tint = if (isListening) AccentRed else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun startVoiceRecognition(
    voiceRecognitionService: VoiceRecognitionService,
    parser: VoiceInputParser,
    defaultWeightUnit: UserPreferences.WeightUnit,
    onListening: () -> Unit,
    onResult: (ParsedVoiceInput) -> Unit,
    onError: (String) -> Unit,
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
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
}
