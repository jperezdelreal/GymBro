package com.gymbro.feature.coach

import com.gymbro.core.ai.ChatMessage

data class CoachChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFirebaseConfigured: Boolean = true,
)

sealed interface CoachChatEvent {
    data class UpdateInput(val text: String) : CoachChatEvent
    data object SendMessage : CoachChatEvent
    data class QuickPromptClicked(val prompt: String) : CoachChatEvent
    data object ClearError : CoachChatEvent
    data object ClearHistory : CoachChatEvent
}

sealed interface CoachChatEffect {
    data object ScrollToBottom : CoachChatEffect
}
