package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CoralAlert
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
    var otpCode by remember { mutableStateOf("") }

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

    LaunchedEffect(otpCode) {
        if (otpCode.length == 6 && !isSigningIn) {
            viewModel.verifyEmailCode(otpCode)
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
                    contentDescription = "Check your email",
                    tint = LimeAccent,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "Check your email",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtext
            Text(
                text = "We sent a 6-digit code to\n$userEmail\n\nEnter the code below to activate your WalkCrew account.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAC7BF),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 6-box OTP Input
            OtpCodeBoxes(
                otpValue = otpCode,
                onValueChange = { otpCode = it },
                enabled = !isSigningIn
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Can't find it? Check your Spam folder.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A8C83),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Verify Button (or loading indicator)
            Button(
                onClick = { if (otpCode.length == 6) viewModel.verifyEmailCode(otpCode) },
                enabled = otpCode.length == 6 && !isSigningIn,
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
                    .testTag("verify_code_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DeepForest,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Verify Code",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resend Code Button with 60s countdown
            Button(
                onClick = {
                    otpCode = ""
                    viewModel.resendVerificationEmail()
                },
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
                    .testTag("resend_code_button")
            ) {
                Text(
                    text = if (cooldownSeconds > 0) "Resend code (${cooldownSeconds}s)" else "Resend code",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Change email / Use a different account (signs out)
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
                    text = "Change email / Use a different account",
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
                .padding(16.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CoralAlert,
                    contentColor = Color.White,
                    shape = ButtonShape
                )
            }
        )
    }
}

@Composable
fun OtpCodeBoxes(
    otpValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val char = otpValue.getOrNull(i)?.toString() ?: ""

            OutlinedTextField(
                value = char,
                onValueChange = { value ->
                    if (!enabled) return@OutlinedTextField
                    if (value.length > 1) {
                        // Paste support for full code
                        val cleaned = value.filter { it.isDigit() }.take(6)
                        if (cleaned.isNotEmpty()) {
                            onValueChange(cleaned)
                            if (cleaned.length == 6) {
                                focusManager.clearFocus()
                            } else {
                                focusRequesters.getOrNull(cleaned.length)?.requestFocus()
                            }
                        }
                    } else if (value.isNotEmpty()) {
                        val digit = value.filter { it.isDigit() }
                        if (digit.isNotEmpty()) {
                            val sb = StringBuilder(otpValue)
                            while (sb.length <= i) sb.append(' ')
                            sb[i] = digit[0]
                            val newCode = sb.toString().trim().take(6)
                            onValueChange(newCode)
                            if (i < 5) {
                                focusRequesters[i + 1].requestFocus()
                            } else {
                                focusManager.clearFocus()
                            }
                        }
                    } else {
                        // Backspace
                        val sb = StringBuilder(otpValue)
                        if (i < sb.length) {
                            sb.setCharAt(i, ' ')
                        }
                        val newCode = sb.toString().trim().take(6)
                        onValueChange(newCode)
                        if (i > 0) {
                            focusRequesters[i - 1].requestFocus()
                        }
                    }
                },
                enabled = enabled,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (i == 5) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                    onNext = { if (i < 5) focusRequesters[i + 1].requestFocus() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LimeAccent,
                    unfocusedBorderColor = Color(0xFF2A4234),
                    focusedContainerColor = Color(0xFF14291D),
                    unfocusedContainerColor = Color(0xFF14291D),
                    cursorColor = LimeAccent
                ),
                modifier = Modifier
                    .size(52.dp)
                    .focusRequester(focusRequesters[i])
                    .testTag("otp_box_$i")
            )
        }
    }
}
