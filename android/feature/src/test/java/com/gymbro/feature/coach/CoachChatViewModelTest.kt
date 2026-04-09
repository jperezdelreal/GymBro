package com.gymbro.feature.coach

import app.cash.turbine.test
import com.gymbro.core.ai.AiCoachService
import com.gymbro.core.ai.ChatMessage
import com.gymbro.core.ai.MessageRole
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoachChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var aiCoachService: AiCoachService
    private lateinit var viewModel: CoachChatViewModel

    @Before
    fun setup() {
        aiCoachService = mockk(relaxed = true)
        every { aiCoachService.getChatHistory() } returns emptyList()
        viewModel = CoachChatViewModel(aiCoachService)
    }

    @Test
    fun `initial state loads chat history`() = runTest {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "Hello"),
            ChatMessage(role = MessageRole.ASSISTANT, content = "Hi!")
        )
        every { aiCoachService.getChatHistory() } returns messages
        
        val newViewModel = CoachChatViewModel(aiCoachService)
        
        val state = newViewModel.state.value
        assertEquals(2, state.messages.size)
        assertEquals("", state.currentInput)
        assertFalse(state.isLoading)
    }

    @Test
    fun `update input changes current input`() = runTest {
        viewModel.onEvent(CoachChatEvent.UpdateInput("Test message"))
        
        val state = viewModel.state.value
        assertEquals("Test message", state.currentInput)
    }

    @Test
    fun `send message success updates messages and clears input`() = runTest {
        val assistantMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "Response")
        coEvery { aiCoachService.sendMessage(any()) } returns Result.success(assistantMessage)
        every { aiCoachService.getChatHistory() } returns listOf(assistantMessage)
        
        viewModel.onEvent(CoachChatEvent.UpdateInput("Test"))
        viewModel.effect.test {
            viewModel.onEvent(CoachChatEvent.SendMessage)
            
            assertEquals(CoachChatEffect.ScrollToBottom, awaitItem())
        }
        
        val state = viewModel.state.value
        assertEquals("", state.currentInput)
        assertFalse(state.isLoading)
    }

    @Test
    fun `send message failure sets error state`() = runTest {
        coEvery { aiCoachService.sendMessage(any()) } returns Result.failure(RuntimeException("API error"))
        
        viewModel.onEvent(CoachChatEvent.UpdateInput("Test"))
        viewModel.onEvent(CoachChatEvent.SendMessage)
        
        val state = viewModel.state.value
        assertEquals("API error", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `clear history clears messages`() = runTest {
        viewModel.onEvent(CoachChatEvent.ClearHistory)
        
        verify { aiCoachService.clearHistory() }
        val state = viewModel.state.value
        assertTrue(state.messages.isEmpty())
    }
}
