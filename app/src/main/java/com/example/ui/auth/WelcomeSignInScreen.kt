package com.example.ui.auth

import android.app.Activity
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import com.example.ui.theme.WalkCrewTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.Spacing

@Composable
fun WelcomeSignInScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
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
            .background(DeepForest) // Full-screen forest green background
            .testTag("welcome_sign_in_screen")
    ) {
        // Decorative background: Large faint circle outline pattern (Canvas, 6% opacity)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color.White.copy(alpha = 0.06f)
            val strokeWidth = 2.dp.toPx()

            // Concentric offset decorative circle outlines
            val centerTop = Offset(size.width * 0.5f, size.height * 0.25f)
            val centerBottom = Offset(size.width * 0.8f, size.height * 0.75f)

            drawCircle(
                color = strokeColor,
                radius = size.width * 0.35f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 0.6f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 0.85f,
                center = centerTop,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = strokeColor,
                radius = size.width * 1.1f,
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

        // Content - Centered, scrollable, max-width constrained (Rules 10 & 12)
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
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Center Branding & Wordmark
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "WalkCrew",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        lineHeight = 46.sp,
                        color = LimeAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("walkcrew_wordmark")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Walk together. Win together.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = Color(0xFFBAC7BF), // textSecondary-light on dark forest
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("walkcrew_tagline")
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Bottom Action Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big pill button "Continue with Google" (lime background)
                    Button(
                        onClick = {
                            if (context is Activity) {
                                viewModel.signInWithGoogle(context)
                            }
                        },
                        enabled = !isSigningIn,
                        shape = ButtonShape, // Fully rounded pill
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
                            .testTag("continue_with_google_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = DeepForest,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Continue with Google",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DeepForest
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sign in to join group step challenges and sync with your crew.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A9C91),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun WelcomeSignInScreenPreview() {
    WalkCrewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepForest)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WalkCrew",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = LimeAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Walk together. Win together.",
                    color = Color(0xFFBAC7BF)
                )
            }
        }
    }
}
