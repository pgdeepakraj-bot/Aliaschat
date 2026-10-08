package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CallSession
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CallRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    fun observeUserCalls(userId: String): Flow<List<CallSession>> {
        val path = "calls"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeCall(callId: String): Flow<CallSession?> {
        val path = "calls/$callId"
        return db.collection("calls").document(callId).snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun initiateCall(
        callerId: String,
        callerUsername: String,
        callerName: String,
        receiverId: String,
        receiverUsername: String,
        callType: String = "audio"
    ): Result<CallSession> {
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val path = "calls/$callId"

        val session = CallSession(
            callId = callId,
            callerId = callerId,
            callerUsername = callerUsername,
            callerName = callerName,
            receiverId = receiverId,
            receiverUsername = receiverUsername,
            memberUids = listOf(callerId, receiverId),
            callType = callType,
            status = "ringing"
        )

        return try {
            db.collection("calls").document(callId).set(session.toWriteMap()).await()
            Result.success(session)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptCall(callId: String): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to "connected",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun declineCall(callId: String): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun endCall(callId: String, durationSeconds: Int): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to "ended",
                    "durationSeconds" to durationSeconds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
