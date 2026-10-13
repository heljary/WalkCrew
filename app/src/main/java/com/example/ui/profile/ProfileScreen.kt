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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.ui.theme.LimeAccent

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    authViewModel: com.example.ui.auth.AuthViewModel? = null,
    photoViewModel: ProfilePhotoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSignOut: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val userName = userProfile?.displayName?.ifBlank { null } ?: "Walker"
    val avatarColorIndex = userProfile?.avatarColorIndex ?: 2
    val healthState by viewModel.healthConnectState.collectAsState()

    val photoUiState by photoViewModel.uiState.collectAsState()
    val currentPhotoUrl = photoUiState.photoUrl ?: userProfile?.photoUrl

    var showAvatarActionSheet by remember { mutableStateOf(false) }
    var showFullscreenPhotoViewer by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoViewModel.uploadPhoto(uri)
        }
    }

    LaunchedEffect(photoUiState.errorMessage) {
        photoUiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            photoViewModel.clearError()
        }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var debugTapCount by remember { mutableStateOf(0) }
    var deleteAccountPassword by remember { mutableStateOf("") }
    var tempName by remember { mutableStateOf(userName) }
    var testFirestoreResult by remember { mutableStateOf<String?>(null) }
    var isTestingFirestore by remember { mutableStateOf(false) }

    val userStats by viewModel.userStats.collectAsState()
    val crewRepository = remember { com.example.data.repository.CrewRepository.getInstance(context) }
    val currentUserId = authViewModel?.currentUserId ?: ""
    val userCrews by crewRepository.observeUserCrews(currentUserId).collectAsState(initial = emptyList())

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
                // Profile Avatar with Clickable Wrapper & Edit Badge
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .testTag("profile_avatar_wrapper"),
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(
                        initials = formattedUserName.take(2).uppercase().ifBlank { "WC" },
                        photoUrl = currentPhotoUrl,
                        colorIndex = avatarColorIndex,
                        size = 84.dp,
                        onClick = {
                            if (!photoUiState.isUploading && !photoUiState.isDeleting) {
                                showAvatarActionSheet = true
                            }
                        },
                        testTag = "profile_large_avatar"
                    )

                    // Centered loading spinner on top of avatar while uploading or deleting
                    if (photoUiState.isUploading || photoUiState.isDeleting) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("avatar_uploading_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = LimeAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Edit badge at bottom-right
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable(
                                enabled = !photoUiState.isUploading && !photoUiState.isDeleting,
                                onClick = { showAvatarActionSheet = true }
                            )
                            .testTag("avatar_edit_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Edit profile photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

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

        Spacer(modifier = Modifier.height(8.dp))

        // App Version Text (Tap 5 times in debug builds to open hidden Debug screen)
        Text(
            text = "WalkCrew v1.0.0 (${if (com.example.BuildConfig.DEBUG) "Debug • Tap 5x for Info" else "Production"})",
            style = MaterialTheme.typography.labelSmall,
            color = WalkCrewTheme.extendedColors.textSecondary.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (com.example.BuildConfig.DEBUG) {
                        debugTapCount++
                        if (debugTapCount >= 5) {
                            debugTapCount = 0
                            showDebugDialog = true
                        }
                    }
                }
                .testTag("app_version_debug_text")
        )
    }

    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = {
                Text(
                    text = "🛠️ WalkCrew Debug Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Project ID: ${com.google.firebase.FirebaseApp.getInstance().options.projectId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "Database ID: ${com.example.data.FirebaseInitializer.FIRESTORE_DATABASE_ID}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "App ID: ${com.google.firebase.FirebaseApp.getInstance().options.applicationId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "API Key: ...${com.google.firebase.FirebaseApp.getInstance().options.apiKey.takeLast(4)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "User ID: ${authViewModel?.currentUserId ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "Crews Joined: " + androidx.compose.ui.res.pluralStringResource(
                            com.example.R.plurals.crews_count,
                            userCrews.size,
                            userCrews.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "Last Log Error: ${com.example.data.WalkCrewLogger.lastFirestoreError ?: "None (All healthy)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (com.example.data.WalkCrewLogger.lastFirestoreError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📊 Today's Steps Firestore State",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val stepLogRepo = remember { com.example.data.repository.StepLogRepository.getInstance(context) }
                    val stepDebugInfo by stepLogRepo.debugInfo.collectAsState()
                    Text(
                        text = "Status: ${stepDebugInfo.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stepDebugInfo.status.startsWith("Error")) MaterialTheme.colorScheme.error else WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "Doc Path: ${stepDebugInfo.documentPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "Raw Value: ${stepDebugInfo.rawValue?.let { "$it steps" } ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                    val syncFormatted = remember(stepDebugInfo.lastSyncTimeMillis) {
                        stepDebugInfo.lastSyncTimeMillis?.let {
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it))
                        } ?: "Never"
                    }
                    Text(
                        text = "Last Sync: $syncFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    Text(
                        text = "Served From: ${if (stepDebugInfo.isFromCache) "Cache (Offline)" else "Server"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PrimaryButton(
                        text = if (isTestingFirestore) "Testing..." else "Test Firestore",
                        onClick = {
                            val uid = authViewModel?.currentUserId
                            if (uid != null) {
                                isTestingFirestore = true
                                val db = com.example.data.FirebaseInitializer.getFirestore()
                                val testRef = db.collection("users").document(uid).collection("debug").document("ping")
                                testRef.set(mapOf("ping" to true, "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                    .addOnSuccessListener {
                                        testRef.get().addOnSuccessListener { doc ->
                                            testFirestoreResult = "Ping success! Write & Read OK."
                                            isTestingFirestore = false
                                        }.addOnFailureListener { e ->
                                            testFirestoreResult = "Read failed: ${e.message}"
                                            isTestingFirestore = false
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        testFirestoreResult = "Write failed: ${e.message}"
                                        isTestingFirestore = false
                                    }
                            } else {
                                testFirestoreResult = "Error: Not signed in"
                            }
                        },
                        enabled = !isTestingFirestore,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "test_firestore_btn"
                    )

                    if (testFirestoreResult != null) {
                        Text(
                            text = testFirestoreResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (testFirestoreResult!!.contains("success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDebugDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    } else if (provider == "google.com") {
                        Text(
                            text = "Please tap below to verify your Google identity.",
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

    // Profile Photo Action Modal (ModalBottomSheet)
    if (showAvatarActionSheet) {
        ProfilePhotoActionSheet(
            hasPhoto = !currentPhotoUrl.isNullOrBlank(),
            onDismiss = { showAvatarActionSheet = false },
            onViewPhoto = {
                showAvatarActionSheet = false
                showFullscreenPhotoViewer = true
            },
            onChooseFromGallery = {
                showAvatarActionSheet = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhoto = {
                showAvatarActionSheet = false
                photoViewModel.deletePhoto()
            }
        )
    }

    // Fullscreen Photo Viewer Dialog
    if (showFullscreenPhotoViewer && !currentPhotoUrl.isNullOrBlank()) {
        FullscreenPhotoViewerDialog(
            photoUrl = currentPhotoUrl,
            displayName = formattedUserName,
            onDismiss = { showFullscreenPhotoViewer = false }
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = navBarBottomPadding)
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
