package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerUsername: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverUsername: String = "",
    val memberUids: List<String> = emptyList(),
    val callType: String = "audio", // "audio", "video"
    val status: String = "initiating", // "initiating", "ringing", "connected", "ended", "declined", "missed"
    val durationSeconds: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "callId" to callId,
            "callerId" to callerId,
            "callerUsername" to callerUsername,
            "callerName" to callerName,
            "receiverId" to receiverId,
            "receiverUsername" to receiverUsername,
            "memberUids" to memberUids,
            "callType" to callType,
            "status" to status,
            "durationSeconds" to durationSeconds,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}
