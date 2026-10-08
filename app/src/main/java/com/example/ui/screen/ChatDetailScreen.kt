package com.example.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.ui.theme.ChatCyan
import com.example.ui.theme.DarkBubbleIncoming
import com.example.ui.theme.DarkBubbleOutgoing
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: Chat,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onStartCall: (targetUid: String, targetUsername: String, isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val messages by chatViewModel.messages.collectAsState()
    val inputText by chatViewModel.messageText.collectAsState()
    val isRecording by chatViewModel.isRecordingVoice.collectAsState()
    val recordDuration by chatViewModel.voiceDurationSeconds.collectAsState()

    val smartReplies by chatViewModel.smartReplies.collectAsState()
    val isGeneratingReplies by chatViewModel.isGeneratingReplies.collectAsState()
    val isRewriting by chatViewModel.isRewriting.collectAsState()
    val aiSummary by chatViewModel.aiSummary.collectAsState()
    val isSummarizing by chatViewModel.isSummarizing.collectAsState()
    val thinkingAnalysis by chatViewModel.thinkingAnalysis.collectAsState()
    val isThinking by chatViewModel.isThinking.collectAsState()

    var showRewriteMenu by remember { mutableStateOf(false) }
    var showDeepThinkingDialog by remember { mutableStateOf(false) }

    val isAiChat = chat.type == "ai" || chat.memberUids.contains("gemini_ai")
    val otherUserIndex = if (chat.memberUids.indexOf(chatViewModel.currentUserId) == 0) 1 else 0
    val peerUid = chat.memberUids.getOrNull(otherUserIndex) ?: ""
    val peerUsername = chat.memberUsernames.getOrNull(otherUserIndex) ?: if (isAiChat) "gemini" else "contact"
    val peerDisplayName = if (isAiChat) "Gemini Assistant" else "@$peerUsername"

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isAiChat) ChatCyan else EmeraldPrimary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAiChat) Icons.Default.AutoAwesome else Icons.Default.Person,
                                contentDescription = "Peer Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = peerDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAiChat) "AI Intelligence • Always Online" else "End-to-End Encrypted",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAiChat) ChatCyan else EmeraldLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isAiChat) {
                        // Audio Call Button
                        IconButton(
                            onClick = { onStartCall(peerUid, peerUsername, false) },
                            modifier = Modifier.testTag("chat_audio_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Voice Call",
                                tint = EmeraldPrimary
                            )
                        }
                        // Video Call Button
                        IconButton(
                            onClick = { onStartCall(peerUid, peerUsername, true) },
                            modifier = Modifier.testTag("chat_video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = EmeraldPrimary
                            )
                        }
                    }

                    // AI tools dropdown
                    IconButton(onClick = { showRewriteMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showRewriteMenu,
                        onDismissRequest = { showRewriteMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📝 Summarize Conversation") },
                            onClick = {
                                showRewriteMenu = false
                                chatViewModel.summarizeCurrentThread()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🧠 Deep Reasoning (High Thinking)") },
                            onClick = {
                                showRewriteMenu = false
                                showDeepThinkingDialog = true
                                chatViewModel.analyzeWithHighThinking()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderId == chatViewModel.currentUserId
                    MessageBubble(message = msg, isMe = isMe)
                }
            }

            // AI Summary Banner if present
            AnimatedVisibility(visible = aiSummary != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Summarize, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Chat Summary", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                            IconButton(onClick = { chatViewModel.aiSummary.value = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = aiSummary.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick Smart Replies Bar (gemini-3.1-flash-lite)
            if (smartReplies.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(smartReplies) { reply ->
                        AssistChip(
                            onClick = {
                                chatViewModel.messageText.value = reply
                                chatViewModel.smartReplies.value = emptyList()
                            },
                            label = { Text(reply, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurfaceElevated
                            )
                        )
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    // AI Quick Rewrite / Polish Bar if text is present
                    if (inputText.isNotBlank() && !isRecording) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            items(listOf("Formal", "Casual", "Concise", "Spanish", "French")) { style ->
                                AssistChip(
                                    onClick = { chatViewModel.rewriteCurrentDraft(style) },
                                    label = { Text("✨ $style", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = DarkSurfaceElevated.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            // Recording View
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkSurfaceElevated, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Recording",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format("Recording: %02d:%02d", recordDuration / 60, recordDuration % 60),
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { chatViewModel.cancelVoiceRecording() }) {
                                    Text("Cancel", color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { chatViewModel.finishAndSendVoiceNote() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(EmeraldPrimary, CircleShape)
                                    .testTag("send_voice_note_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Send Voice Note", tint = Color.White)
                            }
                        } else {
                            // Standard Text Input
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { chatViewModel.messageText.value = it },
                                placeholder = { Text("Message @$peerUsername...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedContainerColor = DarkSurfaceElevated,
                                    focusedContainerColor = DarkSurfaceElevated
                                ),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (inputText.isNotBlank()) {
                                IconButton(
                                    onClick = { chatViewModel.sendMessage() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(EmeraldPrimary, CircleShape)
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White
                                    )
                                }
                            } else {
                                // Mic button to record voice notes
                                IconButton(
                                    onClick = { chatViewModel.startVoiceRecording() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(DarkSurfaceElevated, CircleShape)
                                        .testTag("record_voice_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice note",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Deep Thinking Dialog (gemini-3.1-pro-preview with thinkingLevel = HIGH)
    if (showDeepThinkingDialog) {
        AlertDialog(
            onDismissRequest = { showDeepThinkingDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = ChatCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deep Reasoning (High Thinking)")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Powered by gemini-3.1-pro-preview with thinkingLevel: HIGH",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isThinking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ChatCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Reasoning deeply over conversation context...")
                        }
                    } else {
                        Text(
                            text = thinkingAnalysis ?: "No analysis available yet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeepThinkingDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isMe: Boolean
) {
    val bubbleColor = if (isMe) DarkBubbleOutgoing else DarkBubbleIncoming
    val alignment = if (isMe) Alignment.End else Alignment.Start

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = message.createdAt?.toDate()?.let { timeFormat.format(it) } ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (message.type == "ai") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChatCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gemini AI", style = MaterialTheme.typography.labelSmall, color = ChatCyan, fontWeight = FontWeight.Bold)
                    }
                } else if (!isMe) {
                    Text(
                        text = "@${message.senderUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (message.type == "voice") {
                    // Voice Note Player representation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${message.voiceDurationSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = if (message.status == "read") ChatCyan else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
