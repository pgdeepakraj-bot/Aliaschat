package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AliasChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerUsernameAndProfile_validUser_succeeds() = runBlocking {
        val email = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
        val uid = signInTestUser(email)
        val userRepo = UserRepository(firestore)

        val uniqueUsername = "alice_${UUID.randomUUID().toString().take(6)}"
        val result = withTimeout(5000L) {
            userRepo.registerUsernameAndProfile(
                username = uniqueUsername,
                displayName = "Alice Wonderland"
            )
        }

        assertTrue("Profile registration should succeed", result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(uid, profile.userId)
        assertEquals(uniqueUsername, profile.username)

        // Verify username is now taken
        val isAvailable = userRepo.isUsernameAvailable(uniqueUsername)
        assertTrue("Registered username should not be available", !isAvailable)
    }

    @Test
    fun chatAndMessaging_validMembers_succeeds() = runBlocking {
        val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
        val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@test.com"

        val aliceUid = signInTestUser(aliceEmail)
        val userRepo = UserRepository(firestore)
        val aliceUsername = "alice_${UUID.randomUUID().toString().take(6)}"
        userRepo.registerUsernameAndProfile(aliceUsername, "Alice")

        val bobUid = signInTestUser(bobEmail)
        val bobUsername = "bob_${UUID.randomUUID().toString().take(6)}"
        userRepo.registerUsernameAndProfile(bobUsername, "Bob")

        // Alice starts a chat with Bob
        signInTestUser(aliceEmail)
        val chatRepo = ChatRepository(firestore)

        val chatResult = withTimeout(5000L) {
            chatRepo.getOrCreateDirectChat(
                currentUserId = aliceUid,
                currentUsername = aliceUsername,
                targetUserId = bobUid,
                targetUsername = bobUsername
            )
        }
        assertTrue("Chat creation should succeed", chatResult.isSuccess)
        val chat = chatResult.getOrThrow()
        assertTrue("Chat members should contain Alice", chat.memberUids.contains(aliceUid))
        assertTrue("Chat members should contain Bob", chat.memberUids.contains(bobUid))

        // Alice sends a message
        val msgResult = withTimeout(5000L) {
            chatRepo.sendMessage(
                chatId = chat.chatId,
                senderId = aliceUid,
                senderUsername = aliceUsername,
                memberUids = listOf(aliceUid, bobUid),
                text = "Hey Bob! This is private over secure usernames."
            )
        }
        assertTrue("Message send should succeed", msgResult.isSuccess)

        // Observe messages
        val messages = withTimeout(5000L) {
            chatRepo.observeMessages(chat.chatId).first { list -> list.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Hey Bob! This is private over secure usernames.", messages.first().text)
    }

    @Test
    fun callingSignaling_validCall_succeeds() = runBlocking {
        val callerEmail = "caller_${UUID.randomUUID().toString().take(6)}@test.com"
        val receiverEmail = "rec_${UUID.randomUUID().toString().take(6)}@test.com"

        val callerUid = signInTestUser(callerEmail)
        val receiverUid = signInTestUser(receiverEmail)

        signInTestUser(callerEmail)
        val callRepo = CallRepository(firestore)

        val callResult = withTimeout(5000L) {
            callRepo.initiateCall(
                callerId = callerUid,
                callerUsername = "caller_alias",
                callerName = "Caller Alias",
                receiverId = receiverUid,
                receiverUsername = "receiver_alias",
                callType = "audio"
            )
        }

        assertTrue("Call initiation should succeed", callResult.isSuccess)
        val call = callResult.getOrThrow()
        assertEquals("ringing", call.status)

        // Accept call as receiver
        signInTestUser(receiverEmail)
        val acceptResult = withTimeout(5000L) {
            callRepo.acceptCall(call.callId)
        }
        assertTrue("Accept call should succeed", acceptResult.isSuccess)

        // End call
        val endResult = withTimeout(5000L) {
            callRepo.endCall(call.callId, durationSeconds = 42)
        }
        assertTrue("End call should succeed", endResult.isSuccess)
    }
}
