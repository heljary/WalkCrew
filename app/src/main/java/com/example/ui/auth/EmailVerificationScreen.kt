package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.Spacing
import com.google.firebase.auth.FirebaseUser

@Composable
fun EmailVerificationScreen(
    user: FirebaseUser,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val cooldownSeconds by viewModel.resendCooldown.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val userEmail = user.email ?: "your email address"

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepForest)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("email_verification_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(LimeAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = "Verify Email",
                    tint = LimeAccent,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "Verify Your Email",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Instructions
            Text(
                text = "We sent a confirmation link to:\n$userEmail\n\nPlease tap the link in that email to activate your account and start walking with your crew.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAC7BF),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 1. "I've verified" Button
            Button(
                onClick = { viewModel.checkEmailVerification() },
                enabled = !isSigningIn,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = DeepForest,
                    disabledContainerColor = LimeAccent.copy(alpha = 0.35f),
                    disabledContentColor = DeepForest.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("ive_verified_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DeepForest,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "I've verified",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. "Resend email" Button with 60-second cooldown
            Button(
                onClick = { viewModel.resendVerificationEmail() },
                enabled = cooldownSeconds == 0 && !isSigningIn,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E382A),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF14291D),
                    disabledContentColor = Color(0xFF6E8276)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("resend_verification_email_button")
            ) {
                Text(
                    text = if (cooldownSeconds > 0) "Resend email (${cooldownSeconds}s)" else "Resend email",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. "Use a different account" Button (Sign out)
            OutlinedButton(
                onClick = { viewModel.signOut() },
                shape = ButtonShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFBAC7BF)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A4234)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("use_different_account_button")
            ) {
                Text(
                    text = "Use a different account",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFFBAC7BF)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
