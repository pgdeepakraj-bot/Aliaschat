package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val statusMessage: String = "Available on AliasChat",
    val isOnline: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "username" to username.lowercase().trim(),
            "displayName" to displayName.trim(),
            "avatarUrl" to avatarUrl,
            "statusMessage" to statusMessage,
            "isOnline" to isOnline,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}
