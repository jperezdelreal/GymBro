package com.gymbro.core.ai

import java.time.Instant
import java.util.UUID

data class ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val role: MessageRole,
    val content: String,
    val timestamp: Instant = Instant.now(),
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}
