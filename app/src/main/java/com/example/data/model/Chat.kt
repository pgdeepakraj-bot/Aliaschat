package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class Chat(
    val chatId: String = "",
    val type: String = "direct", // "direct", "group", "ai"
    val memberUids: List<String> = emptyList(),
    val memberUsernames: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageSenderUsername: String = "",
    val lastMessageTime: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "chatId" to chatId,
            "type" to type,
            "memberUids" to memberUids,
            "memberUsernames" to memberUsernames,
            "lastMessage" to lastMessage,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageSenderUsername" to lastMessageSenderUsername,
            "lastMessageTime" to (lastMessageTime ?: FieldValue.serverTimestamp()),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}
