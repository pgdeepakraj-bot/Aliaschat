package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.UsernameReservation
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

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

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId).snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val clean = username.lowercase().trim()
        if (clean.length < 3 || clean.length > 25 || !clean.matches(Regex("^[a-z0-9_]+$"))) {
            return false
        }
        val path = "usernames/$clean"
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            !doc.exists()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun registerUsernameAndProfile(
        username: String,
        displayName: String,
        avatarUrl: String = "",
        statusMessage: String = "Available on AliasChat"
    ): Result<UserProfile> {
        val uid = requireUserId()
        val cleanUsername = username.lowercase().trim()

        if (cleanUsername.length < 3 || cleanUsername.length > 25 || !cleanUsername.matches(Regex("^[a-z0-9_]+$"))) {
            return Result.failure(IllegalArgumentException("Username must be 3-25 alphanumeric characters or underscores."))
        }

        val usernameRef = db.collection("usernames").document(cleanUsername)
        val userRef = db.collection("users").document(uid)

        return try {
            val batch = db.batch()
            val reservation = UsernameReservation(username = cleanUsername, userId = uid)
            val profile = UserProfile(
                userId = uid,
                username = cleanUsername,
                displayName = displayName.ifBlank { "User_${cleanUsername.take(6)}" },
                avatarUrl = avatarUrl,
                statusMessage = statusMessage,
                isOnline = true
            )

            batch.set(usernameRef, reservation.toWriteMap())
            batch.set(userRef, profile.toWriteMap())
            batch.commit().await()

            Result.success(profile)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "usernames/$cleanUsername")
            Result.failure(e)
        }
    }

    suspend fun updatePresence(isOnline: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStatusMessage(statusMessage: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "statusMessage" to statusMessage.trim(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun findUserByUsername(username: String): UserProfile? {
        val clean = username.lowercase().trim().removePrefix("@")
        if (clean.length < 3) return null
        val path = "usernames/$clean"
        return try {
            val usernameDoc = db.collection("usernames").document(clean).get().await()
            if (!usernameDoc.exists()) return null
            val targetUid = usernameDoc.getString("userId") ?: return null
            getUserProfile(targetUid)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }
}
