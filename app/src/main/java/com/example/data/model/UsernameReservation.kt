package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UsernameReservation(
    val username: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        return mapOf(
            "username" to username.lowercase().trim(),
            "userId" to userId,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
        )
    }
}
