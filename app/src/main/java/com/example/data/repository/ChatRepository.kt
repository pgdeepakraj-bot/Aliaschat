package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing operation.")
    }

    fun observeUserChats(userId: String): Flow<List<Chat>> {
        val path = "chats"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.lastMessageTime ?: it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getOrCreateDirectChat(
        currentUserId: String,
        currentUsername: String,
        targetUserId: String,
        targetUsername: String
    ): Result<Chat> {
        val path = "chats"
        return try {
            // Check if direct chat already exists
            val existingSnapshot = db.collection(path)
                .whereArrayContains("memberUids", currentUserId)
                .get()
                .await()

            val existingChat = existingSnapshot.documents.firstOrNull { doc ->
                val members = doc.get("memberUids") as? List<*> ?: emptyList<Any>()
                val chatType = doc.getString("type") ?: ""
                chatType == "direct" && members.contains(targetUserId)
            }

            if (existingChat != null) {
                val chat = existingChat.toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (chat != null) return Result.success(chat)
            }

            // Create deterministic chat ID
            val sortedUids = listOf(currentUserId, targetUserId).sorted()
            val chatId = "chat_${sortedUids[0].take(8)}_${sortedUids[1].take(8)}_${UUID.randomUUID().toString().take(6)}"

            val newChat = Chat(
                chatId = chatId,
                type = if (targetUserId == "gemini_ai") "ai" else "direct",
                memberUids = listOf(currentUserId, targetUserId),
                memberUsernames = listOf(currentUsername, targetUsername),
                lastMessage = "Started a conversation",
                lastMessageSenderId = currentUserId,
                lastMessageSenderUsername = currentUsername
            )

            db.collection(path).document(chatId).set(newChat.toWriteMap()).await()
            Result.success(newChat)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> {
        val path = "chats/$chatId/messages"
        return db.collection("chats").document(chatId).collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderUsername: String,
        memberUids: List<String>,
        text: String,
        type: String = "text",
        voiceDurationSeconds: Int = 0
    ): Result<String> {
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val msgPath = "chats/$chatId/messages/$messageId"
        val chatPath = "chats/$chatId"

        val message = Message(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            senderUsername = senderUsername,
            memberUids = memberUids,
            text = text,
            type = type,
            voiceDurationSeconds = voiceDurationSeconds,
            status = "sent"
        )

        return try {
            val batch = db.batch()
            val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
            val chatRef = db.collection("chats").document(chatId)

            batch.set(msgRef, message.toWriteMap())
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to if (type == "voice") "🎤 Voice message ($voiceDurationSeconds s)" else text.take(100),
                    "lastMessageSenderId" to senderId,
                    "lastMessageSenderUsername" to senderUsername,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgPath)
            Result.failure(e)
        }
    }

    suspend fun markMessageRead(chatId: String, messageId: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update("status", "read")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
