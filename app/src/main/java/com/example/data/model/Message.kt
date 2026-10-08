package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val memberUids: List<String> = emptyList(),
    val text: String = "",
    val type: String = "text", // "text", "voice", "call_log", "ai"
    val voiceDurationSeconds: Int = 0,
    val status: String = "sent", // "sent", "delivered", "read"
    val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "chatId" to chatId,
            "senderId" to senderId,
            "senderUsername" to senderUsername,
            "memberUids" to memberUids,
            "text" to text,
            "type" to type,
            "voiceDurationSeconds" to voiceDurationSeconds,
            "status" to status,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
        ).filterValues { it != null }
    }
}
