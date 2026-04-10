package com.gymbro.feature.coach

import androidx.lifecycle.viewModelScope
import com.gymbro.core.ai.AiCoachService
import com.gymbro.core.ai.MessageRole
import com.gymbro.core.error.toUserMessage
import com.gymbro.feature.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoachChatViewModel @Inject constructor(
    private val aiCoachService: AiCoachService,
) : BaseViewModel() {

    private val _state = MutableStateFlow(CoachChatState())
    val state: StateFlow<CoachChatState> = _state.asStateFlow()

    private val _effect = Channel<CoachChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
        val history = aiCoachService.getChatHistory()
        _state.update { it.copy(messages = history) }
    }

    fun onEvent(event: CoachChatEvent) {
        when (event) {
            is CoachChatEvent.UpdateInput -> {
                _state.update { it.copy(currentInput = event.text) }
            }
            is CoachChatEvent.SendMessage -> {
                sendMessage(_state.value.currentInput)
            }
            is CoachChatEvent.QuickPromptClicked -> {
                _state.update { it.copy(currentInput = event.prompt) }
                sendMessage(event.prompt)
            }
            is CoachChatEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is CoachChatEvent.ClearHistory -> {
                aiCoachService.clearHistory()
                _state.update { it.copy(messages = emptyList()) }
            }
        }
    }

    private fun sendMessage(message: String) {
        if (message.isBlank()) return

        _state.update { it.copy(currentInput = "", isLoading = true, error = null) }

        safeLaunch(
            onError = { error ->
                val isConfigError = error.message?.contains("not configured") == true
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = error.toUserMessage(),
                        isFirebaseConfigured = !isConfigError,
                    ) 
                }
            }
        ) {
            // getOrThrow() will automatically throw if Result is failure
            aiCoachService.sendMessage(message).getOrThrow()
            
            val updatedMessages = aiCoachService.getChatHistory()
            _state.update { 
                it.copy(
                    messages = updatedMessages,
                    isLoading = false,
                ) 
            }
            _effect.send(CoachChatEffect.ScrollToBottom)
        }
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
