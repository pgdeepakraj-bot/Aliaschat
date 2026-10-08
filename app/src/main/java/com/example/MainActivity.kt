package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.screen.AuthScreen
import com.example.ui.screen.CallScreen
import com.example.ui.screen.ChatDetailScreen
import com.example.ui.screen.GeminiLiveVoiceCallScreen
import com.example.ui.screen.IncomingCallDialog
import com.example.ui.screen.MainScreen
import com.example.ui.screen.UsernameSetupScreen
import com.example.ui.theme.AliasChatTheme
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.GeminiLiveCallViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var callRepository: CallRepository
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Repositories initialized with named Firestore database ID from strings
        userRepository = UserRepository(this)
        chatRepository = ChatRepository(this)
        callRepository = CallRepository(this)
        authViewModel = AuthViewModel(userRepository)

        setContent {
            AliasChatTheme {
                AliasChatApp(
                    authViewModel = authViewModel,
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    callRepository = callRepository
                )
            }
        }
    }
}

@Composable
fun AliasChatApp(
    authViewModel: AuthViewModel,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    callRepository: CallRepository
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val authUiState by authViewModel.uiState.collectAsState()

    // Listen to Firebase Auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            authViewModel.checkCurrentAuthState()
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    when (val state = authUiState) {
        is AuthUiState.Checking -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(EmeraldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Loading AliasChat",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "AliasChat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        is AuthUiState.Unauthenticated, is AuthUiState.Error -> {
            AuthScreen(authViewModel = authViewModel)
        }

        is AuthUiState.AuthenticatedNeedUsername -> {
            UsernameSetupScreen(
                user = state.user,
                authViewModel = authViewModel,
                onCompleted = {
                    authViewModel.checkCurrentAuthState()
                }
            )
        }

        is AuthUiState.AuthenticatedReady -> {
            val userProfile = state.profile

            val chatViewModel = remember(state.user.uid) {
                ChatViewModel(
                    chatRepository = chatRepository,
                    userRepository = userRepository,
                    currentUserId = state.user.uid,
                    currentUsername = userProfile.username
                )
            }

            val callViewModel = remember(state.user.uid) {
                CallViewModel(
                    callRepository = callRepository,
                    currentUserId = state.user.uid,
                    currentUsername = userProfile.username,
                    currentDisplayName = userProfile.displayName
                )
            }

            val liveCallViewModel = remember { GeminiLiveCallViewModel() }

            val selectedChat by chatViewModel.selectedChat.collectAsState()
            val activeCall by callViewModel.activeCall.collectAsState()
            val incomingCall by callViewModel.incomingCall.collectAsState()
            var isLiveGeminiCallOpen by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    activeCall != null -> {
                        CallScreen(
                            call = activeCall!!,
                            callViewModel = callViewModel
                        )
                    }

                    isLiveGeminiCallOpen -> {
                        GeminiLiveVoiceCallScreen(
                            liveViewModel = liveCallViewModel,
                            onClose = { isLiveGeminiCallOpen = false }
                        )
                    }

                    selectedChat != null -> {
                        ChatDetailScreen(
                            chat = selectedChat!!,
                            chatViewModel = chatViewModel,
                            onBack = { chatViewModel.clearSelectedChat() },
                            onStartCall = { targetUid, targetUsername, isVideo ->
                                callViewModel.startCall(
                                    targetUserId = targetUid,
                                    targetUsername = targetUsername,
                                    callType = if (isVideo) "video" else "audio"
                                )
                            }
                        )
                    }

                    else -> {
                        MainScreen(
                            userProfile = userProfile,
                            chatViewModel = chatViewModel,
                            callViewModel = callViewModel,
                            onOpenChat = { chat ->
                                chatViewModel.selectChat(chat)
                            },
                            onStartCall = { targetUid, targetUsername, isVideo ->
                                callViewModel.startCall(
                                    targetUserId = targetUid,
                                    targetUsername = targetUsername,
                                    callType = if (isVideo) "video" else "audio"
                                )
                            },
                            onStartGeminiLiveCall = {
                                liveCallViewModel.startLiveCall()
                                isLiveGeminiCallOpen = true
                            },
                            onSignOut = {
                                authViewModel.signOut(context, credentialManager)
                            }
                        )
                    }
                }

                // Global Incoming Call Alert Dialog
                incomingCall?.let { ringingCall ->
                    IncomingCallDialog(
                        call = ringingCall,
                        onAccept = { callViewModel.acceptIncomingCall() },
                        onDecline = { callViewModel.declineIncomingCall() }
                    )
                }
            }
        }
    }
}
