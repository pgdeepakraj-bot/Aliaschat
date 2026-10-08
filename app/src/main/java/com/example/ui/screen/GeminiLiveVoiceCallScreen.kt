package com.example.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBubbleIncoming
import com.example.ui.theme.DarkBubbleOutgoing
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.GeminiLiveCallViewModel

@Composable
fun GeminiLiveVoiceCallScreen(
    liveViewModel: GeminiLiveCallViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpeaking by liveViewModel.isGeminiSpeaking.collectAsState()
    val isUserSpeaking by liveViewModel.isUserSpeaking.collectAsState()
    val isMuted by liveViewModel.isMuted.collectAsState()
    val isSpeakerOn by liveViewModel.isSpeakerOn.collectAsState()
    val durationSeconds by liveViewModel.callDurationSeconds.collectAsState()
    val statusText by liveViewModel.currentStatusText.collectAsState()
    val turns by liveViewModel.conversationTurns.collectAsState()

    var spokenTextInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) {
            listState.animateScrollToItem(turns.size - 1)
        }
    }

    // Orb animation pulse
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isSpeaking) 1.35f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03141F),
                        Color(0xFF0A2231),
                        DarkBackground
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(EmeraldPrimary.copy(alpha = 0.2f), ChatCyan.copy(alpha = 0.2f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini Live",
                        tint = ChatCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gemini Live API • gemini-3.8-live",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChatCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "AI Live Voice Call",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d • %s", mins, secs, statusText),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSpeaking) EmeraldLight else Color.LightGray
                )
            }

            // Center Visualizer & Live Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .padding(vertical = 8.dp)
            ) {
                // Outer Glow ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isSpeaking) {
                                    listOf(EmeraldLight.copy(alpha = 0.35f), Color.Transparent)
                                } else {
                                    listOf(ChatCyan.copy(alpha = 0.25f), Color.Transparent)
                                }
                            ),
                            shape = CircleShape
                        )
                )

                // Inner Orb
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isSpeaking) {
                                    listOf(EmeraldPrimary, EmeraldLight)
                                } else {
                                    listOf(ChatCyan, EmeraldPrimary)
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Speaking indicator",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Real-time Conversation Turns List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.85f))
            ) {
                if (turns.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Speak naturally with Gemini Live or type below...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(turns) { turn ->
                            val isMe = turn.speaker == "You"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isMe) DarkBubbleOutgoing else DarkBubbleIncoming,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = turn.speaker,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMe) EmeraldLight else ChatCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = turn.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Spoken Prompt Topics Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "Tell me a fun fact about encryption",
                    "How can I keep my identity private online?",
                    "Brainstorm creative WhatsApp alias names",
                    "What are the benefits of zero-phone messaging?"
                )
                items(suggestions) { prompt ->
                    AssistChip(
                        onClick = {
                            liveViewModel.submitUserSpokenText(prompt)
                        },
                        label = { Text(prompt, maxLines = 1, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            // Spoken text input bar (for testing speech input in container)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = spokenTextInput,
                    onValueChange = { spokenTextInput = it },
                    placeholder = { Text("Speak or type query to Gemini Live...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_call_voice_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedContainerColor = DarkSurfaceElevated
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val text = spokenTextInput.trim()
                        if (text.isNotBlank()) {
                            liveViewModel.submitUserSpokenText(text)
                            spokenTextInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(EmeraldPrimary, CircleShape)
                        .testTag("live_call_send_speech_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send spoken turn",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Call Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                IconButton(
                    onClick = { liveViewModel.toggleMute() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            if (isMuted) Color(0xFFEF5350) else DarkSurfaceElevated,
                            CircleShape
                        )
                        .testTag("live_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute Microphone",
                        tint = Color.White
                    )
                }

                // Interrupt AI button
                IconButton(
                    onClick = { liveViewModel.interruptGemini() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            if (isSpeaking) Color(0xFFFFA000) else DarkSurfaceElevated,
                            CircleShape
                        )
                        .testTag("live_interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt",
                        tint = Color.White
                    )
                }

                // Speakerphone
                IconButton(
                    onClick = { liveViewModel.toggleSpeaker() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            if (isSpeakerOn) EmeraldPrimary else DarkSurfaceElevated,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speakerphone",
                        tint = Color.White
                    )
                }

                // End Live Call
                IconButton(
                    onClick = {
                        liveViewModel.endLiveCall()
                        onClose()
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .testTag("end_live_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
