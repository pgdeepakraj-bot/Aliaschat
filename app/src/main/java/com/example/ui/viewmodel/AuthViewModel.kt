package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data object Unauthenticated : AuthUiState
    data class AuthenticatedNeedUsername(val user: FirebaseUser) : AuthUiState
    data class AuthenticatedReady(val user: FirebaseUser, val profile: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername: StateFlow<Boolean> = _isCheckingUsername.asStateFlow()

    private val _usernameAvailability = MutableStateFlow<Boolean?>(null)
    val usernameAvailability: StateFlow<Boolean?> = _usernameAvailability.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private var usernameCheckJob: Job? = null

    init {
        checkCurrentAuthState()
    }

    fun checkCurrentAuthState() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = AuthUiState.Unauthenticated
        } else {
            loadUserProfile(user)
        }
    }

    private fun loadUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Checking
            val profile = userRepository.getUserProfile(user.uid)
            if (profile != null && profile.username.isNotBlank()) {
                _uiState.value = AuthUiState.AuthenticatedReady(user, profile)
            } else {
                _uiState.value = AuthUiState.AuthenticatedNeedUsername(user)
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        usernameCheckJob?.cancel()
        val clean = username.lowercase().trim().removePrefix("@")
        if (clean.length < 3) {
            _usernameAvailability.value = null
            _isCheckingUsername.value = false
            return
        }

        usernameCheckJob = viewModelScope.launch {
            _isCheckingUsername.value = true
            delay(350) // Debounce
            val available = userRepository.isUsernameAvailable(clean)
            _usernameAvailability.value = available
            _isCheckingUsername.value = false
        }
    }

    fun registerUsername(
        username: String,
        displayName: String,
        statusMessage: String = "Available on AliasChat",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("User is not signed in")
            return
        }
        val clean = username.lowercase().trim().removePrefix("@")
        if (clean.length < 3 || clean.length > 25) {
            onError("Username must be between 3 and 25 characters.")
            return
        }

        viewModelScope.launch {
            _isRegistering.value = true
            val name = displayName.ifBlank { user.displayName ?: "User_$clean" }
            val result = userRepository.registerUsernameAndProfile(
                username = clean,
                displayName = name,
                avatarUrl = user.photoUrl?.toString() ?: "",
                statusMessage = statusMessage
            )
            _isRegistering.value = false

            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _uiState.value = AuthUiState.AuthenticatedReady(user, profile)
                onSuccess()
            } else {
                val msg = result.exceptionOrNull()?.localizedMessage ?: "Failed to claim username. It might be taken."
                onError(msg)
            }
        }
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        loadUserProfile(user)
                    } else {
                        _uiState.value = AuthUiState.Error("Sign in failed: no user object returned.")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Unexpected credential type returned")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(context: Context, credentialManager: CredentialManager) {
        viewModelScope.launch {
            try {
                userRepository.updatePresence(false)
            } catch (_: Exception) {}
            auth.signOut()
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            _uiState.value = AuthUiState.Unauthenticated
        }
    }
}
