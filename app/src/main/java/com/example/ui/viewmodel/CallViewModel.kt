package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.repository.CallRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CallViewModel(
    private val callRepository: CallRepository,
    val currentUserId: String,
    val currentUsername: String,
    val currentDisplayName: String
) : ViewModel() {

    val calls: StateFlow<List<CallSession>> = callRepository.observeUserCalls(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    private val _incomingCall = MutableStateFlow<CallSession?>(null)
    val incomingCall: StateFlow<CallSession?> = _incomingCall.asStateFlow()

    val isMuted = MutableStateFlow(false)
    val isSpeakerOn = MutableStateFlow(false)
    val isVideoEnabled = MutableStateFlow(true)
    val callDurationSeconds = MutableStateFlow(0)

    private var activeCallJob: Job? = null
    private var durationTimerJob: Job? = null

    init {
        observeIncomingCalls()
    }

    private fun observeIncomingCalls() {
        viewModelScope.launch {
            callRepository.observeUserCalls(currentUserId).collect { callList ->
                // Check if there is an incoming ringing call where receiverId is currentUserId
                val ringing = callList.firstOrNull { it.receiverId == currentUserId && it.status == "ringing" }
                if (_activeCall.value == null) {
                    _incomingCall.value = ringing
                } else if (ringing == null && _incomingCall.value != null) {
                    _incomingCall.value = null
                }
            }
        }
    }

    fun startCall(
        targetUserId: String,
        targetUsername: String,
        callType: String = "audio"
    ) {
        viewModelScope.launch {
            isMuted.value = false
            isSpeakerOn.value = false
            isVideoEnabled.value = (callType == "video")
            callDurationSeconds.value = 0

            val result = callRepository.initiateCall(
                callerId = currentUserId,
                callerUsername = currentUsername,
                callerName = currentDisplayName,
                receiverId = targetUserId,
                receiverUsername = targetUsername,
                callType = callType
            )

            if (result.isSuccess) {
                val call = result.getOrThrow()
                setActiveCallSession(call)
            }
        }
    }

    fun acceptIncomingCall() {
        val incoming = _incomingCall.value ?: return
        _incomingCall.value = null
        viewModelScope.launch {
            callRepository.acceptCall(incoming.callId)
            setActiveCallSession(incoming)
        }
    }

    fun declineIncomingCall() {
        val incoming = _incomingCall.value ?: return
        _incomingCall.value = null
        viewModelScope.launch {
            callRepository.declineCall(incoming.callId)
        }
    }

    private fun setActiveCallSession(session: CallSession) {
        _activeCall.value = session
        activeCallJob?.cancel()
        activeCallJob = viewModelScope.launch {
            callRepository.observeCall(session.callId).collect { updatedCall ->
                if (updatedCall == null || updatedCall.status in listOf("ended", "declined", "missed")) {
                    stopDurationTimer()
                    _activeCall.value = null
                } else {
                    _activeCall.value = updatedCall
                    if (updatedCall.status == "connected" && durationTimerJob == null) {
                        startDurationTimer()
                    }
                }
            }
        }
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                callDurationSeconds.value += 1
            }
        }
    }

    private fun stopDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = null
    }

    fun endCall() {
        val call = _activeCall.value ?: return
        val duration = callDurationSeconds.value
        stopDurationTimer()
        _activeCall.value = null
        activeCallJob?.cancel()

        viewModelScope.launch {
            callRepository.endCall(call.callId, duration)
        }
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
    }

    fun toggleSpeaker() {
        isSpeakerOn.value = !isSpeakerOn.value
    }

    fun toggleVideo() {
        isVideoEnabled.value = !isVideoEnabled.value
    }
}
