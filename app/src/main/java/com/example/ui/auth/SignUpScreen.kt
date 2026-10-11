package com.example.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CardShape
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DeepForest
import com.example.ui.theme.GoldStar
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.Spacing

private val EmailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

enum class PasswordStrength(val label: String, val fraction: Float, val color: Color) {
    EMPTY("", 0f, Color.Transparent),
    WEAK("Weak", 0.33f, CoralAlert),
    MEDIUM("Medium", 0.66f, GoldStar),
    STRONG("Strong", 1f, LimeAccent)
}

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        viewModel.navigateToUnauthScreen(UnauthScreen.WELCOME)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }

    // Live Validations
    val isEmailValid by remember { derivedStateOf { email.isNotBlank() && EmailRegex.matches(email.trim()) } }
    val isPasswordLengthValid by remember { derivedStateOf { password.length >= 8 } }
    val hasNumber by remember { derivedStateOf { password.any { it.isDigit() } } }
    val isPasswordValid by remember { derivedStateOf { isPasswordLengthValid && hasNumber } }
    val doPasswordsMatch by remember { derivedStateOf { password.isNotEmpty() && password == confirmPassword } }

    val passwordStrength by remember {
        derivedStateOf {
            when {
                password.isEmpty() -> PasswordStrength.EMPTY
                password.length < 8 || !password.any { it.isDigit() } -> PasswordStrength.WEAK
                password.length >= 10 && password.any { it.isUpperCase() } && password.any { !it.isLetterOrDigit() } -> PasswordStrength.STRONG
                else -> PasswordStrength.MEDIUM
            }
        }
    }

    val isFormValid by remember {
        derivedStateOf {
            displayName.trim().isNotBlank() &&
                isEmailValid &&
                isPasswordValid &&
                doPasswordsMatch &&
                agreeToTerms
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepForest)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("sign_up_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateToUnauthScreen(UnauthScreen.WELCOME) },
                    modifier = Modifier.testTag("sign_up_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = LimeAccent
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WalkCrew",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = LimeAccent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Create Account",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Join WalkCrew to track daily steps and compete with friends.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAC7BF)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                placeholder = { Text("e.g. Alex Walker") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = CardShape,
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_name_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("name@example.com") },
                singleLine = true,
                isError = email.isNotBlank() && !isEmailValid,
                supportingText = {
                    if (email.isNotBlank() && !isEmailValid) {
                        Text("Enter a valid email address", color = CoralAlert)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = CardShape,
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Color(0xFFBAC7BF)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = CardShape,
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_password_input")
            )

            // Password Strength Bar
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val animatedProgress by animateFloatAsState(targetValue = passwordStrength.fraction, label = "strength")
                val animatedColor by animateColorAsState(targetValue = passwordStrength.color, label = "strengthColor")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Strength: ${passwordStrength.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Min 8 chars + 1 number",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPasswordValid) LimeAccent else Color(0xFF8A9C91)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = animatedColor,
                    trackColor = Color(0xFF1E3326)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                        Text("Passwords do not match", color = CoralAlert)
                    }
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                            tint = Color(0xFFBAC7BF)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = CardShape,
                colors = outlinedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_up_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Terms and Privacy Policy Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { agreeToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = LimeAccent,
                        checkmarkColor = DeepForest,
                        uncheckedColor = Color(0xFF8A9C91)
                    ),
                    modifier = Modifier.testTag("terms_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "I agree to the ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBAC7BF)
                        )
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = LimeAccent,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://walkcrew.app/terms"))
                                context.startActivity(intent)
                            }
                        )
                        Text(
                            text = " and",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBAC7BF)
                        )
                    }
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = LimeAccent,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://walkcrew.app/privacy"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Button
            Button(
                onClick = {
                    if (isFormValid && !isSigningIn) {
                        viewModel.signUpWithEmail(displayName, email, password)
                    }
                },
                enabled = isFormValid && !isSigningIn,
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
                    .testTag("create_account_submit_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DeepForest,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Link to Sign In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBAC7BF)
                )
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = LimeAccent,
                    modifier = Modifier
                        .clickable { viewModel.navigateToUnauthScreen(UnauthScreen.SIGN_IN) }
                        .testTag("sign_up_to_sign_in_link")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LimeAccent,
    unfocusedBorderColor = Color(0xFF2A4234),
    focusedLabelColor = LimeAccent,
    unfocusedLabelColor = Color(0xFFBAC7BF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = LimeAccent,
    focusedContainerColor = Color(0xFF14291D),
    unfocusedContainerColor = Color(0xFF14291D)
)
