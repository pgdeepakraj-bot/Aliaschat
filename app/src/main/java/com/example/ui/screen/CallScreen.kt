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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallSession
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.CallViewModel

@Composable
fun CallScreen(
    call: CallSession,
    callViewModel: CallViewModel,
    modifier: Modifier = Modifier
) {
    val isMuted by callViewModel.isMuted.collectAsState()
    val isSpeakerOn by callViewModel.isSpeakerOn.collectAsState()
    val isVideoEnabled by callViewModel.isVideoEnabled.collectAsState()
    val durationSeconds by callViewModel.callDurationSeconds.collectAsState()

    val isCaller = (call.callerId == callViewModel.currentUserId)
    val peerUsername = if (isCaller) call.receiverUsername else call.callerUsername
    val peerName = if (isCaller) "@${call.receiverUsername}" else call.callerName.ifBlank { "@${call.callerUsername}" }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0F1E29),
                        DarkBackground
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section: Peer Info & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Encrypted",
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End-to-End Encrypted",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = peerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "@$peerUsername",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                val statusDisplay = when (call.status) {
                    "ringing" -> if (isCaller) "Ringing..." else "Incoming call..."
                    "connected" -> {
                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        String.format("%02d:%02d", mins, secs)
                    }
                    else -> call.status.replaceFirstChar { it.uppercase() }
                }

                Text(
                    text = statusDisplay,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (call.status == "connected") EmeraldLight else Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Middle Section: Visualizer or Video view
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                if (isVideoEnabled) {
                    // Video Call View
                    Card(
                        modifier = Modifier
                            .size(240.dp, 320.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video on",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "HD Video Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "@$peerUsername",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                } else {
                    // Audio Call Avatar & Pulsing Wave Rings
                    Box(contentAlignment = Alignment.Center) {
                        if (call.status == "connected") {
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .scale(waveScale1)
                                    .background(
                                        color = EmeraldPrimary.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(EmeraldPrimary, EmeraldDark)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Peer Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Controls Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle
                    IconButton(
                        onClick = { callViewModel.toggleMute() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isMuted) Color(0xFFEF5350) else DarkSurfaceElevated,
                                CircleShape
                            )
                            .testTag("mute_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mute",
                            tint = Color.White
                        )
                    }

                    // Speakerphone Toggle
                    IconButton(
                        onClick = { callViewModel.toggleSpeaker() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isSpeakerOn) EmeraldPrimary else DarkSurfaceElevated,
                                CircleShape
                            )
                            .testTag("speaker_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Toggle Speaker",
                            tint = Color.White
                        )
                    }

                    // Video Camera Toggle
                    IconButton(
                        onClick = { callViewModel.toggleVideo() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isVideoEnabled) EmeraldPrimary else DarkSurfaceElevated,
                                CircleShape
                            )
                            .testTag("video_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Video",
                            tint = Color.White
                        )
                    }

                    // End Call Button (Big Red Button)
                    IconButton(
                        onClick = { callViewModel.endCall() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE53935), CircleShape)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
