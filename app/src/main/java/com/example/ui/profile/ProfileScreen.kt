package com.example.ui.profile

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.example.ui.home.formatDisplayName
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HealthConnectState
import com.example.health.HealthConnectRationaleActivity
import com.example.ui.components.Avatar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.CardShape
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme

@Composable
fun ProfileScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    authViewModel: com.example.ui.auth.AuthViewModel? = null,
    onSignOut: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val userName = userProfile?.displayName?.ifBlank { null } ?: "Walker"
    val avatarColorIndex = userProfile?.avatarColorIndex ?: 2
    val healthState by viewModel.healthConnectState.collectAsState()
    val revenueCatManager = remember { com.example.billing.RevenueCatManager.getInstance(context) }
    val isPremium by revenueCatManager.isPremium.collectAsState()

    LaunchedEffect(Unit) {
        revenueCatManager.refreshCustomerInfo()
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountPassword by remember { mutableStateOf("") }
    var tempName by remember { mutableStateOf(userName) }

    val formattedUserName = remember(userName) { formatDisplayName(userName) }
    val navBarBottomPadding = 24.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = 16.dp, bottom = navBarBottomPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WalkCrewTheme.extendedColors.textPrimary
        )

        // Profile Avatar & Name Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                .testTag("profile_avatar_card"),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(
                    initials = formattedUserName.take(2).uppercase().ifBlank { "WC" },
                    colorIndex = avatarColorIndex,
                    size = 72.dp,
                    testTag = "profile_large_avatar"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedUserName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                    IconButton(onClick = {
                        tempName = userName
                        showEditNameDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit name",
                            tint = WalkCrewTheme.extendedColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                val currentGoal = userProfile?.dailyStepGoal ?: 8000L
                Text(
                    text = "Daily Goal: ${java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).format(currentGoal)} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 8 Preset Avatar Color Options
                Text(
                    text = "Avatar Theme Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AvatarColors.forEachIndexed { index, color ->
                        val isSelected = index == avatarColorIndex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) WalkCrewTheme.extendedColors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateProfile(userName, index)
                                }
                                .testTag("color_preset_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Step Goal Setting (3,000 to 20,000 steps, step 500)
        SectionHeader(title = "Daily Step Goal")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                .testTag("daily_goal_card"),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                val currentGoal = userProfile?.dailyStepGoal ?: 8000L
                var sliderValue by remember(currentGoal) { mutableStateOf(currentGoal.toFloat()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )
                        Text(
                            text = "Used for your daily ring and streaks",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )
                    }

                    Text(
                        text = "${java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).format(sliderValue.toLong())} steps",
                        fontFamily = com.example.ui.theme.PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { newVal ->
                        // Step 500
                        val snapped = (Math.round(newVal / 500.0) * 500).toFloat()
                        sliderValue = snapped.coerceIn(3000f, 20000f)
                    },
                    onValueChangeFinished = {
                        viewModel.updateDailyGoal(sliderValue.toLong())
                    },
                    valueRange = 3000f..20000f,
                    steps = ((20000 - 3000) / 500) - 1, // 33 discrete steps
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step_goal_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "3,000",
                        style = MaterialTheme.typography.labelSmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "20,000",
                        style = MaterialTheme.typography.labelSmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                }
            }
        }

        // Daily Reminder Notification Settings (6:00 PM if below 60% of goal)
        SectionHeader(title = "Reminders & Notifications")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                .testTag("daily_reminder_card"),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            val reminderOn = userProfile?.reminderEnabled ?: true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (reminderOn) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "6:00 PM Step Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )
                        Text(
                            text = "Only notifies if you are below 60% of your daily goal to help maintain your streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WalkCrewTheme.extendedColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = reminderOn,
                    onCheckedChange = { isChecked ->
                        viewModel.toggleDailyReminder(isChecked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = com.example.ui.theme.DeepForest,
                        checkedTrackColor = com.example.ui.theme.LimeAccent,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("reminder_toggle_switch")
                )
            }
        }

        // Health Connect Status Card
        SectionHeader(title = "Health Connect & Cloud Sync")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                .testTag("health_connect_status_card"),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (healthState) {
                            is HealthConnectState.AvailableAndGranted -> Icons.Rounded.CheckCircle
                            else -> Icons.AutoMirrored.Rounded.DirectionsWalk
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Status: " + when (healthState) {
                                is HealthConnectState.AvailableAndGranted -> "Connected & Syncing"
                                is HealthConnectState.NeedsPermission -> "Permission Needed"
                                is HealthConnectState.PermissionDenied -> "Permission Denied"
                                is HealthConnectState.NotInstalled -> "Not Installed"
                                else -> "Local Step Mode"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )
                        Text(
                            text = "WorkManager periodic sync active (every 15 min to Firestore)",
                            style = MaterialTheme.typography.bodySmall,
                            color = WalkCrewTheme.extendedColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Privacy Rationale",
                        onClick = {
                            val intent = Intent(context, HealthConnectRationaleActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Rounded.Info,
                        testTag = "view_rationale_button"
                    )

                    SecondaryButton(
                        text = "Sync Now",
                        onClick = { viewModel.refreshSteps() },
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Rounded.Refresh,
                        testTag = "sync_now_button"
                    )
                }
            }
        }

        // Plan & Entitlement Section
        SectionHeader(title = "Plan & Membership")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isPremium) com.example.ui.theme.LimeAccent.copy(alpha = 0.5f) else WalkCrewTheme.extendedColors.divider,
                    shape = CardShape
                )
                .testTag("membership_status_card"),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPremium) "WalkCrew Premium" else "Free Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPremium) "Entitlement: active ('premium')" else "Entitlement: none ('free')",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPremium) MaterialTheme.colorScheme.primary else WalkCrewTheme.extendedColors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isPremium) com.example.ui.theme.LimeAccent else MaterialTheme.colorScheme.surfaceVariant,
                                shape = com.example.ui.theme.ButtonShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isPremium) "PRO ACTIVE" else "FREE TIER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPremium) com.example.ui.theme.DeepForest else WalkCrewTheme.extendedColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isPremium) {
                        "You have full access to unlimited crews, custom themes, detailed step history, and ad-free experience."
                    } else {
                        "Free tier allows joining up to 2 crews. Upgrade to Premium for unlimited crews and custom styling."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (isPremium) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = "View Benefits",
                            onClick = onNavigateToPaywall,
                            modifier = Modifier.weight(1f),
                            testTag = "view_premium_benefits_btn"
                        )
                        SecondaryButton(
                            text = "Restore",
                            onClick = { revenueCatManager.restorePurchases() },
                            modifier = Modifier.weight(1f),
                            testTag = "restore_in_profile_btn"
                        )
                    }
                } else {
                    PrimaryButton(
                        text = "Upgrade to Pro",
                        onClick = onNavigateToPaywall,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "upgrade_to_pro_button"
                    )
                }
            }
        }

        // Account Sign Out Section
        SecondaryButton(
            text = "Sign Out",
            onClick = { showSignOutConfirmDialog = true },
            leadingIcon = Icons.AutoMirrored.Rounded.ExitToApp,
            modifier = Modifier.fillMaxWidth(),
            testTag = "sign_out_button"
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Account Deletion Section (Required for Google Play Store Policy)
        TextButton(
            onClick = {
                deleteAccountPassword = ""
                showDeleteAccountDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_account_button")
        ) {
            Text(
                text = "Delete Account & Data",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of WalkCrew? You can sign back in anytime to access your crew and step progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirmDialog = false
                    onSignOut()
                }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Cancel", color = WalkCrewTheme.extendedColors.textSecondary)
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Profile Name",
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) {
                        viewModel.updateProfile(tempName.trim(), avatarColorIndex)
                    }
                    showEditNameDialog = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = WalkCrewTheme.extendedColors.textSecondary)
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDeleteAccountDialog) {
        val provider = authViewModel?.getCurrentUserProvider() ?: "password"
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Delete Account & Data?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete your WalkCrew account? All your step history, crew memberships, and profile data will be permanently removed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Security verification is required before deletion:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (provider == "password") {
                        OutlinedTextField(
                            value = deleteAccountPassword,
                            onValueChange = { deleteAccountPassword = it },
                            label = { Text("Enter your password") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (provider == "google.com") {
                        Text(
                            text = "Please tap below to verify your Google identity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (provider == "facebook.com") {
                        Text(
                            text = "Please tap below to verify your Facebook identity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentContext = context
                        showDeleteAccountDialog = false
                        if (authViewModel != null) {
                            when (provider) {
                                "password" -> authViewModel.reauthenticateAndDeleteWithPassword(deleteAccountPassword)
                                "google.com" -> {
                                    if (currentContext is android.app.Activity) {
                                        authViewModel.reauthenticateAndDeleteWithGoogle(currentContext)
                                    }
                                }
                                "facebook.com" -> {
                                    if (currentContext is android.app.Activity) {
                                        authViewModel.reauthenticateAndDeleteWithFacebook(currentContext)
                                    }
                                }
                                else -> onDeleteAccount()
                            }
                        } else {
                            onDeleteAccount()
                        }
                    },
                    enabled = provider != "password" || deleteAccountPassword.isNotBlank()
                ) {
                    Text("Verify & Delete Permanently", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = WalkCrewTheme.extendedColors.textSecondary)
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun ProfileScreenPreview() {
    WalkCrewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Profile & Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
