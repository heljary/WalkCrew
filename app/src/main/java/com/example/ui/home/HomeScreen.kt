package com.example.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DailyStepData
import com.example.data.HealthConnectState
import com.example.data.UserStats
import com.example.health.HealthConnectManager
import com.example.ui.components.Avatar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StepRing
import com.example.ui.theme.CardShape
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import com.example.ui.util.rememberResponsiveLayoutInfo
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalTime
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToCrews: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val stats by viewModel.userStats.collectAsState()
    val healthState by viewModel.healthConnectState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val userName = userProfile?.displayName?.ifBlank { null } ?: "Walker"
    val avatarColorIndex = userProfile?.avatarColorIndex ?: 2

    val layoutInfo = rememberResponsiveLayoutInfo()
    val isNarrow = layoutInfo.screenWidthDp < 340.dp
    val isLandscapeMode = layoutInfo.isLandscape && !layoutInfo.isExpandedWidth

    // Refresh step data whenever app resumes
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSteps()
        onPauseOrDispose { }
    }

    // Permission launcher for ACTIVITY_RECOGNITION on Android 10+ (API 29+)
    val activityRecognitionPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onActivityRecognitionPermissionResult(granted)
    }

    // Auto-request ACTIVITY_RECOGNITION if not granted yet
    val hasActivityPermission by viewModel.hasActivityRecognitionPermission.collectAsState()
    LaunchedEffect(hasActivityPermission) {
        if (!hasActivityPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            activityRecognitionPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Permission launcher for Health Connect
    val healthConnectManager = remember { HealthConnectManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedSet ->
        val granted = grantedSet.containsAll(healthConnectManager.permissions)
        viewModel.onHealthConnectPermissionResult(granted)
    }

    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val dailyGoal = userProfile?.dailyStepGoal ?: stats.goal

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
                .widthIn(max = if (isLandscapeMode) 960.dp else 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = 16.dp, bottom = navBarBottomPadding) // Comfortable padding above root scaffold nav bar
        ) {
            // Section 1: Top Row (Greeting & Avatar)
            StaggeredEntrance(index = 0) {
                GreetingRow(
                    greeting = greeting,
                    userName = formattedUserName,
                    avatarColorIndex = avatarColorIndex,
                    photoUrl = userProfile?.photoUrl,
                    onRefresh = { viewModel.refreshSteps() },
                    onAddTestSteps = { viewModel.addSimulatedSteps(1000L) },
                    onProfile = onNavigateToProfile
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Activity Recognition Hardware Sensor Permission banner (Android 10+)
            if (!hasActivityPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                StaggeredEntrance(index = 1) {
                    ActivityRecognitionBanner(
                        hasPermission = hasActivityPermission,
                        onRequestPermission = {
                            activityRecognitionPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Health Connect Status banner (when not granted or not installed)
            StaggeredEntrance(index = 1) {
                HealthConnectBanner(
                    state = healthState,
                    onRequestPermission = {
                        permissionLauncher.launch(healthConnectManager.permissions)
                    },
                    onInstallHealthConnect = {
                        viewModel.installHealthConnect()
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (isLandscapeMode) {
                // Rule 13: In landscape on phones, show StepRing on the left and stats + chart on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StepRingCard(
                            stats = stats,
                            dailyGoal = dailyGoal
                        )

                        StreakCardSection(
                            stats = stats,
                            dailyGoal = dailyGoal
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StatCardsSection(
                            stats = stats,
                            isNarrow = isNarrow
                        )

                        WeeklyChartSection(
                            stats = stats
                        )

                        CrewsPlaceholderSection(
                            onNavigateToCrews = onNavigateToCrews
                        )
                    }
                }
            } else {
                // Portrait / Standard vertical stack
                StaggeredEntrance(index = 2) {
                    StepRingCard(
                        stats = stats,
                        dailyGoal = dailyGoal
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                StaggeredEntrance(index = 3) {
                    StreakCardSection(
                        stats = stats,
                        dailyGoal = dailyGoal
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                StaggeredEntrance(index = 4) {
                    StatCardsSection(
                        stats = stats,
                        isNarrow = isNarrow
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                StaggeredEntrance(index = 5) {
                    WeeklyChartSection(
                        stats = stats
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                StaggeredEntrance(index = 6) {
                    CrewsPlaceholderSection(
                        onNavigateToCrews = onNavigateToCrews
                    )
                }
            }
        }
    }
}

@Composable
private fun GreetingRow(
    greeting: String,
    userName: String,
    avatarColorIndex: Int,
    photoUrl: String? = null,
    onRefresh: () -> Unit,
    onAddTestSteps: () -> Unit = {},
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "$greeting, $userName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = WalkCrewTheme.extendedColors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "Step forward today",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B0)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (com.example.BuildConfig.DEBUG) {
                SecondaryButton(
                    text = "+1,000",
                    onClick = onAddTestSteps,
                    modifier = Modifier.height(36.dp).padding(end = 8.dp),
                    testTag = "debug_add_steps_button"
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("refresh_steps_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh steps",
                    tint = Color(0xFFB0B0B0)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Avatar(
                initials = userName.take(2).uppercase(Locale.getDefault()),
                photoUrl = photoUrl,
                colorIndex = avatarColorIndex,
                size = 48.dp,
                onClick = onProfile,
                testTag = "home_user_avatar"
            )
        }
    }
}

@Composable
private fun StepRingCard(
    stats: UserStats,
    dailyGoal: Long
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
            .testTag("step_ring_card"),
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
            StepRing(
                currentSteps = stats.steps,
                goalSteps = dailyGoal,
                modifier = Modifier.testTag("home_step_ring")
            )
        }
    }
}

@Composable
private fun StreakCardSection(
    stats: UserStats,
    dailyGoal: Long
) {
    val history = stats.weeklyHistory
    val streakCount = remember(history, dailyGoal, stats.steps) {
        var count = 0
        val reversed = history.reversed()
        for (day in reversed) {
            val steps = if (day.isToday) stats.steps else day.steps
            if (steps >= dailyGoal) {
                count++
            } else if (!day.isToday) {
                break
            }
        }
        count
    }

    StreakCard(
        weeklyHistory = history,
        dailyGoal = dailyGoal,
        currentStreakDays = streakCount
    )
}

@Composable
private fun StatCardsSection(
    stats: UserStats,
    isNarrow: Boolean
) {
    if (isNarrow) {
        // Narrow screens (< 340dp): 2+1 grid so text never truncates
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    title = "Distance",
                    value = String.format(Locale.getDefault(), "%.1f", stats.distanceKm),
                    unit = "km",
                    modifier = Modifier.weight(1f),
                    testTag = "stat_distance"
                )
                StatCard(
                    title = "Calories",
                    value = NumberFormat.getNumberInstance(Locale.getDefault()).format(stats.caloriesKcal),
                    unit = "kcal",
                    modifier = Modifier.weight(1f),
                    testTag = "stat_calories"
                )
            }
            StatCard(
                title = "Active time",
                value = "${stats.activeMinutes}",
                unit = "mins",
                modifier = Modifier.fillMaxWidth(),
                testTag = "stat_active_time"
            )
        }
    } else {
        // Normal screens: 3 equal cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            StatCard(
                title = "Distance",
                value = String.format(Locale.getDefault(), "%.1f", stats.distanceKm),
                unit = "km",
                modifier = Modifier.weight(1f),
                testTag = "stat_distance"
            )
            StatCard(
                title = "Calories",
                value = NumberFormat.getNumberInstance(Locale.getDefault()).format(stats.caloriesKcal),
                unit = "kcal",
                modifier = Modifier.weight(1f),
                testTag = "stat_calories"
            )
            StatCard(
                title = "Active time",
                value = "${stats.activeMinutes}",
                unit = "mins",
                modifier = Modifier.weight(1f),
                testTag = "stat_active_time"
            )
        }
    }
}

@Composable
private fun WeeklyChartSection(
    stats: UserStats
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "This week",
            trailingContent = {
                Text(
                    text = "Past 7 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB0B0B0)
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        WeeklyBarChart(
            weeklyData = stats.weeklyHistory,
            goalSteps = stats.goal
        )
    }
}

@Composable
private fun CrewsPlaceholderSection(
    onNavigateToCrews: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Your crews")

        Spacer(modifier = Modifier.height(Spacing.sm))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                .testTag("your_crews_placeholder_card"),
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No crew yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create or join a crew to start competing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkCrewTheme.extendedColors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Explore Crews",
                    onClick = onNavigateToCrews,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "join_crew_button"
                )
            }
        }
    }
}

/**
 * Staggered entrance animation wrapper (fade + slide up, staggered 60ms)
 */
@Composable
private fun StaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 60L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { 50 }
        )
    ) {
        content()
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    WalkCrewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen()
        }
    }
}

/**
 * Formats a dynamic user name into clean Title Case (e.g. "hamza" -> "Hamza", "hamza eljary" -> "Hamza Eljary").
 */
fun formatDisplayName(name: String): String {
    if (name.isBlank()) return "Walker"
    return name.trim().split(Regex("\\s+")).joinToString(" ") { word ->
        word.lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
