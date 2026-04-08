package com.gymbro.feature.coach

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.ai.ChatMessage
import com.gymbro.core.ai.MessageRole
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AccentGreen = Color(0xFF00FF87)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val UserBubble = Color(0xFF2D2D2D)

@Composable
fun CoachChatRoute(
    onNavigateBack: () -> Unit,
    viewModel: CoachChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CoachChatEffect.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                }
            }
        }
    }

    CoachChatScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        listState = listState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachChatScreen(
    state: CoachChatState,
    onEvent: (CoachChatEvent) -> Unit,
    onNavigateBack: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AccentGreen, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text("GymBro AI Coach")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { onEvent(CoachChatEvent.ClearHistory) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear history",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        containerColor = DarkBackground,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {
            if (!state.isFirebaseConfigured) {
                FirebaseNotConfiguredMessage()
            } else if (state.error != null) {
                ErrorMessage(
                    message = state.error,
                    onDismiss = { onEvent(CoachChatEvent.ClearError) },
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    item {
                        WelcomeMessage()
                    }
                    item {
                        QuickPrompts(onPromptClick = { prompt ->
                            onEvent(CoachChatEvent.QuickPromptClicked(prompt))
                        })
                    }
                }

                items(state.messages) { message ->
                    ChatBubble(message = message)
                }

                if (state.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }

            MessageInput(
                text = state.currentInput,
                onTextChange = { onEvent(CoachChatEvent.UpdateInput(it)) },
                onSend = { onEvent(CoachChatEvent.SendMessage) },
                enabled = !state.isLoading && state.isFirebaseConfigured,
            )
        }
    }
}

@Composable
private fun WelcomeMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(AccentGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GymBro AI Coach",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your personal strength training advisor",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun QuickPrompts(onPromptClick: (String) -> Unit) {
    Column {
        Text(
            text = "Quick prompts:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                listOf(
                    "How should I break my bench plateau?",
                    "What should I train today?",
                    "Tips for better squat depth",
                    "Am I training too often?",
                )
            ) { prompt ->
                Surface(
                    onClick = { onPromptClick(prompt) },
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    contentColor = AccentGreen,
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(AccentGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = "Coach",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.8f else 0.95f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) UserBubble else DarkSurface,
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 12.dp,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(AccentGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = "Coach is thinking...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = AccentGreen,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
                placeholder = {
                    Text(
                        text = if (enabled) "Ask your coach..." else "Firebase not configured",
                        color = Color.White.copy(alpha = 0.5f),
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground,
                    disabledContainerColor = DarkBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = AccentGreen,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = enabled,
            )
            
            Surface(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                shape = CircleShape,
                color = if (enabled && text.isNotBlank()) AccentGreen else Color.Gray,
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (enabled && text.isNotBlank()) Color.Black else Color.DarkGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun FirebaseNotConfiguredMessage() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3E2723),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "⚠️ Firebase Not Configured",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFAB91),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI Coach requires Firebase AI (Vertex AI). Add google-services.json to your project to enable this feature.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF5D1F1A),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFEF9A9A),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun formatTimestamp(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
