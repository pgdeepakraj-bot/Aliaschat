package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.service.GeminiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveVoiceTurn(
    val speaker: String, // "You" or "Gemini"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiLiveCallViewModel : ViewModel() {

    val isCallActive = MutableStateFlow(false)
    val isGeminiSpeaking = MutableStateFlow(false)
    val isUserSpeaking = MutableStateFlow(false)
    val isMuted = MutableStateFlow(false)
    val isSpeakerOn = MutableStateFlow(true)
    val callDurationSeconds = MutableStateFlow(0)

    val currentStatusText = MutableStateFlow("Connecting to Gemini Live (gemini-3.8-live)...")
    val conversationTurns = MutableStateFlow<List<LiveVoiceTurn>>(emptyList())

    private var durationTimerJob: Job? = null
    private var simulatedSpeechJob: Job? = null

    fun startLiveCall() {
        isCallActive.value = true
        isMuted.value = false
        isSpeakerOn.value = true
        callDurationSeconds.value = 0
        conversationTurns.value = emptyList()
        currentStatusText.value = "Connecting to Gemini Live (gemini-3.8-live)..."

        durationTimerJob?.cancel()
        durationTimerJob = viewModelScope.launch {
            delay(1200)
            currentStatusText.value = "Live audio session connected"
            // Greeting turn from Gemini Live
            isGeminiSpeaking.value = true
            currentStatusText.value = "Gemini is speaking..."
            val greeting = "Hi there! I'm Gemini Live. I can hear you clearly. What would you like to talk about today?"
            delay(1500)
            conversationTurns.value = listOf(LiveVoiceTurn(speaker = "Gemini", text = greeting))
            isGeminiSpeaking.value = false
            currentStatusText.value = "Listening to you..."
            isUserSpeaking.value = true

            // Duration timer
            while (isCallActive.value) {
                delay(1000)
                callDurationSeconds.value += 1
            }
        }
    }

    fun submitUserSpokenText(spokenText: String) {
        val clean = spokenText.trim()
        if (clean.isBlank() || isMuted.value) return

        viewModelScope.launch {
            isUserSpeaking.value = false
            currentStatusText.value = "Processing voice stream..."
            val currentList = conversationTurns.value.toMutableList()
            currentList.add(LiveVoiceTurn(speaker = "You", text = clean))
            conversationTurns.value = currentList

            val historyContext = currentList.takeLast(6).joinToString("\n") { "${it.speaker}: ${it.text}" }

            isGeminiSpeaking.value = true
            currentStatusText.value = "Gemini is responding..."

            val reply = GeminiService.liveVoiceConversation(clean, historyContext)

            currentList.add(LiveVoiceTurn(speaker = "Gemini", text = reply))
            conversationTurns.value = currentList

            // Simulate speech duration based on response length
            val speechDuration = (reply.split(" ").size * 280L).coerceIn(1500L, 7000L)
            delay(speechDuration)

            isGeminiSpeaking.value = false
            currentStatusText.value = "Listening to you..."
            isUserSpeaking.value = true
        }
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        if (isMuted.value) {
            currentStatusText.value = "Microphone muted"
            isUserSpeaking.value = false
        } else {
            currentStatusText.value = "Listening to you..."
            isUserSpeaking.value = true
        }
    }

    fun toggleSpeaker() {
        isSpeakerOn.value = !isSpeakerOn.value
    }

    fun interruptGemini() {
        if (isGeminiSpeaking.value) {
            isGeminiSpeaking.value = false
            currentStatusText.value = "Interrupted • Listening to you..."
            isUserSpeaking.value = true
        }
    }

    fun endLiveCall() {
        isCallActive.value = false
        isGeminiSpeaking.value = false
        isUserSpeaking.value = false
        durationTimerJob?.cancel()
        simulatedSpeechJob?.cancel()
        currentStatusText.value = "Call ended"
    }
}
