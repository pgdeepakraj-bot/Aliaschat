package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.data.service.GeminiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    val currentUserId: String,
    val currentUsername: String
) : ViewModel() {

    val chats: StateFlow<List<Chat>> = chatRepository.observeUserChats(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedChat = MutableStateFlow<Chat?>(null)
    val selectedChat: StateFlow<Chat?> = _selectedChat.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var messageObservationJob: Job? = null

    // Message input & voice notes
    val messageText = MutableStateFlow("")
    val isRecordingVoice = MutableStateFlow(false)
    val voiceDurationSeconds = MutableStateFlow(0)
    private var voiceTimerJob: Job? = null

    // Contact search by username
    val searchQuery = MutableStateFlow("")
    val searchResult = MutableStateFlow<UserProfile?>(null)
    val isSearching = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)

    // AI Intelligence States
    val smartReplies = MutableStateFlow<List<String>>(emptyList())
    val isGeneratingReplies = MutableStateFlow(false)

    val isRewriting = MutableStateFlow(false)
    val aiSummary = MutableStateFlow<String?>(null)
    val isSummarizing = MutableStateFlow(false)

    val thinkingAnalysis = MutableStateFlow<String?>(null)
    val isThinking = MutableStateFlow(false)

    fun selectChat(chat: Chat) {
        _selectedChat.value = chat
        messageText.value = ""
        smartReplies.value = emptyList()
        aiSummary.value = null
        thinkingAnalysis.value = null

        messageObservationJob?.cancel()
        messageObservationJob = viewModelScope.launch {
            chatRepository.observeMessages(chat.chatId).collect { msgList ->
                _messages.value = msgList
                // When new incoming message arrives from peer, trigger smart reply generation
                val lastMsg = msgList.lastOrNull()
                if (lastMsg != null && lastMsg.senderId != currentUserId && lastMsg.text.isNotBlank()) {
                    generateSmartReplies(lastMsg.text)
                }
            }
        }
    }

    fun clearSelectedChat() {
        _selectedChat.value = null
        _messages.value = emptyList()
        messageObservationJob?.cancel()
    }

    fun sendMessage() {
        val chat = _selectedChat.value ?: return
        val text = messageText.value.trim()
        if (text.isBlank()) return

        messageText.value = ""
        smartReplies.value = emptyList()

        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chat.chatId,
                senderId = currentUserId,
                senderUsername = currentUsername,
                memberUids = chat.memberUids,
                text = text,
                type = "text"
            )

            // If chatting with Gemini AI assistant
            if (chat.type == "ai" || chat.memberUids.contains("gemini_ai")) {
                delay(600)
                val aiResponse = GeminiService.generateContent(
                    model = "gemini-3.5-flash",
                    prompt = text,
                    systemInstruction = "You are Gemini AI Assistant inside AliasChat. Provide helpful, conversational responses."
                )
                chatRepository.sendMessage(
                    chatId = chat.chatId,
                    senderId = "gemini_ai",
                    senderUsername = "gemini",
                    memberUids = chat.memberUids,
                    text = aiResponse,
                    type = "ai"
                )
            }
        }
    }

    fun startVoiceRecording() {
        isRecordingVoice.value = true
        voiceDurationSeconds.value = 0
        voiceTimerJob?.cancel()
        voiceTimerJob = viewModelScope.launch {
            while (isRecordingVoice.value) {
                delay(1000)
                voiceDurationSeconds.value += 1
            }
        }
    }

    fun cancelVoiceRecording() {
        isRecordingVoice.value = false
        voiceTimerJob?.cancel()
        voiceDurationSeconds.value = 0
    }

    fun finishAndSendVoiceNote() {
        val duration = voiceDurationSeconds.value.coerceAtLeast(1)
        val chat = _selectedChat.value ?: return
        cancelVoiceRecording()

        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chat.chatId,
                senderId = currentUserId,
                senderUsername = currentUsername,
                memberUids = chat.memberUids,
                text = "Voice message",
                type = "voice",
                voiceDurationSeconds = duration
            )
        }
    }

    fun searchUserByUsername(username: String) {
        val clean = username.lowercase().trim().removePrefix("@")
        searchQuery.value = clean
        searchError.value = null
        if (clean.length < 3) {
            searchResult.value = null
            return
        }

        viewModelScope.launch {
            isSearching.value = true
            val profile = userRepository.findUserByUsername(clean)
            isSearching.value = false
            if (profile != null) {
                searchResult.value = profile
                searchError.value = null
            } else {
                searchResult.value = null
                searchError.value = "No user found with alias @$clean"
            }
        }
    }

    fun startChatWithUser(targetUser: UserProfile, onChatReady: (Chat) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.getOrCreateDirectChat(
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                targetUserId = targetUser.userId,
                targetUsername = targetUser.username
            )
            if (result.isSuccess) {
                val chat = result.getOrThrow()
                selectChat(chat)
                onChatReady(chat)
            }
        }
    }

    fun startChatWithGemini(onChatReady: (Chat) -> Unit) {
        val geminiUser = UserProfile(
            userId = "gemini_ai",
            username = "gemini",
            displayName = "Gemini AI Assistant",
            statusMessage = "Always online • Intelligence & Live Voice"
        )
        startChatWithUser(geminiUser, onChatReady)
    }

    fun generateSmartReplies(contextText: String) {
        viewModelScope.launch {
            isGeneratingReplies.value = true
            val replies = GeminiService.generateSmartReplies(contextText)
            smartReplies.value = replies
            isGeneratingReplies.value = false
        }
    }

    fun rewriteCurrentDraft(style: String) {
        val draft = messageText.value.trim()
        if (draft.isBlank()) return

        viewModelScope.launch {
            isRewriting.value = true
            val polished = GeminiService.rewriteMessage(draft, style)
            if (!polished.startsWith("Error")) {
                messageText.value = polished
            }
            isRewriting.value = false
        }
    }

    fun summarizeCurrentThread() {
        val msgs = _messages.value
        if (msgs.isEmpty()) return

        val textPayload = msgs.takeLast(25).joinToString("\n") { m ->
            "${m.senderUsername}: ${m.text}"
        }

        viewModelScope.launch {
            isSummarizing.value = true
            aiSummary.value = GeminiService.summarizeThread(textPayload)
            isSummarizing.value = false
        }
    }

    fun analyzeWithHighThinking(customPrompt: String? = null) {
        val msgs = _messages.value
        val context = customPrompt ?: msgs.takeLast(15).joinToString("\n") { "${it.senderUsername}: ${it.text}" }
        if (context.isBlank()) return

        viewModelScope.launch {
            isThinking.value = true
            thinkingAnalysis.value = GeminiService.analyzeWithHighThinking(context)
            isThinking.value = false
        }
    }
}
