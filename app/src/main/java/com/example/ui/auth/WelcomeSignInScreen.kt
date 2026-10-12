package com.example.ui.auth

import android.app.Activity
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CardShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.Spacing

private val DarkButtonText = Color(0xFF181D19)

@Composable
fun WelcomeSignInScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeAuthAction by viewModel.activeAuthAction.collectAsState()
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val isGoogleLoading by viewModel.isGoogleLoading.collectAsState()
    val isAnyAuthLoading = isSigningIn || activeAuthAction != AuthAction.NONE
    val errorMessage by viewModel.errorMessage.collectAsState()
    val collisionDialog by viewModel.collisionDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepForest)
            .testTag("welcome_screen")
    ) {
        // Decorative background: Large faint circle outline pattern (Canvas, 6% opacity)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color.White.copy(alpha = 0.06f)
            val strokeWidth = 2.dp.toPx()

            val centerTop = Offset(size.width * 0.5f, size.height * 0.22f)
            val centerBottom = Offset(size.width * 0.85f, size.height * 0.8f)

            drawCircle(
                color = strokeColor,
                radius = size.width * 0.35f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 0.62f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 0.9f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 1.15f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 0.5f,
                center = centerBottom,
                style = Stroke(width = strokeWidth)
            )
        }

        // Content - Centered, scrollable, max-width constrained
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Center Branding & Wordmark
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "WalkCrew",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp,
                        lineHeight = 50.sp,
                        color = LimeAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("walkcrew_wordmark")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Walk together. Win together.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = Color(0xFFBAC7BF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("walkcrew_tagline")
                    )
                }

                Spacer(modifier = Modifier.height(56.dp))

                // Stacked Pill Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. "Continue with Google" (White surface, dark text, vector Google G)
                    Button(
                        onClick = {
                            if (!isAnyAuthLoading && context is Activity) {
                                viewModel.signInWithGoogle(context)
                            }
                        },
                        enabled = !isAnyAuthLoading,
                        shape = ButtonShape,
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = DarkButtonText,
                            disabledContainerColor = Color.White.copy(alpha = 0.6f),
                            disabledContentColor = DarkButtonText.copy(alpha = 0.6f)
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("continue_with_google_button")
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = DarkButtonText,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DarkButtonText
                                )
                            }
                        }
                    }

                    // 2. "Continue with email" (Lime button)
                    Button(
                        onClick = {
                            if (!isAnyAuthLoading) {
                                viewModel.navigateToUnauthScreen(UnauthScreen.SIGN_UP)
                            }
                        },
                        enabled = !isAnyAuthLoading,
                        shape = ButtonShape,
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = DeepForest,
                            disabledContainerColor = LimeAccent.copy(alpha = 0.6f),
                            disabledContentColor = DeepForest.copy(alpha = 0.6f)
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("continue_with_email_button")
                    ) {
                        Text(
                            text = "Continue with email",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DeepForest
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Small text "Already have an account? Sign in"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
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
                            .clickable(enabled = !isAnyAuthLoading) {
                                viewModel.navigateToUnauthScreen(UnauthScreen.SIGN_IN)
                            }
                            .testTag("welcome_to_sign_in_link")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Account Linking Collision Dialog
        collisionDialog?.let { dialog ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissCollisionDialog() },
                title = {
                    Text(
                        text = "Link Your Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "This email (${dialog.email}) is already registered with ${dialog.existingMethod}. Please sign in with ${dialog.existingMethod} to link your accounts together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBAC7BF)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissCollisionDialog()
                            viewModel.navigateToUnauthScreen(UnauthScreen.SIGN_IN)
                        },
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = DeepForest)
                    ) {
                        Text("Sign In to Link", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCollisionDialog() }) {
                        Text("Cancel", color = Color(0xFFBAC7BF))
                    }
                },
                containerColor = Color(0xFF14291D),
                shape = CardShape
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
