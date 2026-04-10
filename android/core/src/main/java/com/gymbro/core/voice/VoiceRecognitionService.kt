package com.gymbro.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface VoiceRecognitionState {
    data object Idle : VoiceRecognitionState
    data object Listening : VoiceRecognitionState
    data class Result(val transcript: String) : VoiceRecognitionState
    data class Error(val message: String) : VoiceRecognitionState
}

@Singleton
class VoiceRecognitionService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(): Flow<VoiceRecognitionState> = callbackFlow {
        if (!isAvailable()) {
            trySend(VoiceRecognitionState.Error("Speech recognition not available"))
            close()
            return@callbackFlow
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(VoiceRecognitionState.Listening)
                }

                override fun onBeginningOfSpeech() {
                    // Speech detected
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - could use for visual feedback
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Partial audio buffer
                }

                override fun onEndOfSpeech() {
                    // User stopped speaking
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    trySend(VoiceRecognitionState.Error(errorMessage))
                    close()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull() ?: ""
                    if (transcript.isNotEmpty()) {
                        trySend(VoiceRecognitionState.Result(transcript))
                    } else {
                        trySend(VoiceRecognitionState.Error("No speech detected"))
                    }
                    close()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Could use for live transcription
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Additional events
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Use device locale for bilingual support (Spanish/English)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            startListening(intent)
        }

        awaitClose {
            stopListening()
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
