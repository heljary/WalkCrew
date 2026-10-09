package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.CardShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme

@Composable
fun ProfileSetupScreen(
    initialName: String,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var displayName by remember { mutableStateOf(initialName) }
    var selectedColorIndex by remember { mutableIntStateOf(2) } // Lime default
    val isSaving by viewModel.isSigningIn.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screenHorizontal, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to WalkCrew",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Set up your walker profile to compete with crews",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Avatar live preview
                Avatar(
                    initials = displayName.ifBlank { "WC" },
                    colorIndex = selectedColorIndex,
                    size = 96.dp,
                    testTag = "setup_avatar_preview"
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Display Name Input Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape),
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Display Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = { Text("Enter your name") },
                            singleLine = true,
                            shape = CardShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = WalkCrewTheme.extendedColors.divider,
                                focusedTextColor = WalkCrewTheme.extendedColors.textPrimary,
                                unfocusedTextColor = WalkCrewTheme.extendedColors.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar Color Swatches Card (8 swatches)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape),
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Choose Avatar Color",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Select one of 8 signature WalkCrew colors",
                            style = MaterialTheme.typography.bodySmall,
                            color = WalkCrewTheme.extendedColors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarColors.forEachIndexed { index, color ->
                                val isSelected = index == selectedColorIndex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) WalkCrewTheme.extendedColors.textPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorIndex = index }
                                        .testTag("swatch_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected color",
                                            tint = if (color == LimeAccent) DeepForest else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = if (isSaving) "Saving profile..." else "Save & Continue",
                onClick = {
                    if (displayName.isNotBlank()) {
                        viewModel.completeProfileSetup(displayName, selectedColorIndex)
                    }
                },
                enabled = displayName.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_profile_button"
            )
        }
    }
}
