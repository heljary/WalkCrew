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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsWalk
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
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val userName = userProfile?.displayName ?: "Alex"
    val avatarColorIndex = userProfile?.avatarColorIndex ?: 2

    // Refresh step data whenever app resumes
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSteps()
        onPauseOrDispose { }
    }

    // Permission launcher for Health Connect
    val healthConnectManager = remember { HealthConnectManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedSet ->
        val granted = grantedSet.containsAll(healthConnectManager.permissions)
        viewModel.onPermissionResult(granted)
    }

    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = 16.dp, bottom = 100.dp) // Leave room for floating pill nav bar
    ) {
        // Section 1: Top Row (Greeting & Avatar)
        StaggeredEntrance(index = 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting, $userName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                    Text(
                        text = "Step forward today",
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshSteps() },
                        modifier = Modifier.testTag("refresh_steps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh steps",
                            tint = WalkCrewTheme.extendedColors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Avatar(
                        initials = userName.take(2),
                        colorIndex = avatarColorIndex,
                        size = 48.dp,
                        onClick = onNavigateToProfile,
                        testTag = "home_user_avatar"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg)) // 24dp spacing

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

        Spacer(modifier = Modifier.height(Spacing.lg)) // 24dp spacing

        // Section 2: Big StepRing Card (surface, 28dp radius, 24dp padding)
        StaggeredEntrance(index = 2) {
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
                    val dailyGoal = userProfile?.dailyStepGoal ?: stats.goal
                    StepRing(
                        currentSteps = stats.steps,
                        goalSteps = dailyGoal,
                        modifier = Modifier.testTag("home_step_ring")
                    )

                    // Quick Step Simulator actions (so users can test ring & counter live)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecondaryButton(
                            text = "+1,000 steps",
                            onClick = { viewModel.addSimulatedSteps(1000L) },
                            leadingIcon = Icons.Rounded.Add,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            testTag = "simulate_steps_btn"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg)) // 24dp spacing

        // Section 2.5: Streaks Card under the StepRing
        // Flame icon in coral (#FF6B4A), streak number in headlineMedium,
        // row of last 7 days as small circles (filled lime if goal reached, empty outline if not, today pulsing)
        StaggeredEntrance(index = 3) {
            val dailyGoal = userProfile?.dailyStepGoal ?: stats.goal
            // Calculate consecutive streak days reaching daily goal
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

        Spacer(modifier = Modifier.height(Spacing.lg)) // 24dp spacing

        // Section 3: Row of 3 StatCards (equal width)
        StaggeredEntrance(index = 4) {
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

        Spacer(modifier = Modifier.height(Spacing.lg)) // 24dp spacing

        // Section 4: "This week" section with custom-drawn Canvas Bar Chart
        StaggeredEntrance(index = 5) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "This week",
                    trailingContent = {
                        Text(
                            text = "Past 7 days",
                            style = MaterialTheme.typography.labelSmall,
                            color = WalkCrewTheme.extendedColors.textSecondary
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

        Spacer(modifier = Modifier.height(Spacing.lg)) // 24dp spacing

        // Section 5: Placeholder "Your crews" card
        StaggeredEntrance(index = 6) {
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
