package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun UsernameSetupScreen(
    user: FirebaseUser,
    authViewModel: AuthViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var usernameInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf(user.displayName ?: "") }
    var statusMessageInput by remember { mutableStateOf("Hey there! I am using AliasChat.") }

    val isChecking by authViewModel.isCheckingUsername.collectAsState()
    val isAvailable by authViewModel.usernameAvailability.collectAsState()
    val isRegistering by authViewModel.isRegistering.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = "Username Alias",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose Your Secure Alias",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Your friends will contact you by @username instead of a mobile number. It must be unique.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Username Input with live validation
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { input ->
                        val filtered = input.lowercase().filter { it.isLetterOrDigit() || it == '_' }
                        usernameInput = filtered
                        authViewModel.checkUsernameAvailability(filtered)
                    },
                    label = { Text("Choose Username Handle") },
                    leadingIcon = {
                        Text(
                            text = "@",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        } else if (usernameInput.length >= 3 && isAvailable != null) {
                            if (isAvailable == true) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Available",
                                    tint = EmeraldLight
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Taken",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    supportingText = {
                        if (usernameInput.isNotBlank() && usernameInput.length < 3) {
                            Text("Must be at least 3 characters", color = MaterialTheme.colorScheme.error)
                        } else if (isAvailable == true) {
                            Text("@$usernameInput is available!", color = EmeraldLight)
                        } else if (isAvailable == false) {
                            Text("@$usernameInput is already taken. Try another.", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Letters, numbers, and underscores (3-25 chars)")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display Name Input
                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Name")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Message Input
                OutlinedTextField(
                    value = statusMessageInput,
                    onValueChange = { statusMessageInput = it },
                    label = { Text("About / Status") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_message_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Guarantee Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Shield",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AliasChat does not collect, verify, or link phone numbers. Your identity is your username.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Confirm Button
            Button(
                onClick = {
                    authViewModel.registerUsername(
                        username = usernameInput,
                        displayName = displayNameInput,
                        statusMessage = statusMessageInput,
                        onSuccess = onCompleted,
                        onError = { error ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("claim_username_button"),
                enabled = usernameInput.length >= 3 && isAvailable == true && !isRegistering,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Registering Alias...")
                } else {
                    Text(
                        text = "Claim @$usernameInput & Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
