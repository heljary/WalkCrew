package com.example.ui.crews

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.util.rememberResponsiveLayoutInfo
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.example.notifications.NotificationHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.LeaderboardMember
import com.example.ui.components.Avatar
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CardShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.RankBronze
import com.example.ui.theme.RankGold
import com.example.ui.theme.RankSilver
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CrewDetailScreen(
    viewModel: CrewDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detailState by viewModel.detailState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val reactionsMap by viewModel.reactions.collectAsState()
    val nudgedUsersToday by viewModel.nudgedUsersToday.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val darkTheme = isSystemInDarkTheme()
    val layoutInfo = rememberResponsiveLayoutInfo()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingNudgeMember by remember { mutableStateOf<LeaderboardMember?>(null) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    // Floating reaction picker target user
    var activeReactionTarget by remember { mutableStateOf<LeaderboardMember?>(null) }

    // Android 13+ Notification permission request launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.createNotificationChannels(context)
            pendingNudgeMember?.let { member ->
                viewModel.sendNudge(member.userId, member.displayName)
                pendingNudgeMember = null
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    BackHandler { onBackClick() }

    val activeLeaderboard = when (selectedTab) {
        LeaderboardTab.TODAY -> detailState.todayLeaderboard
        LeaderboardTab.THIS_WEEK -> detailState.weekLeaderboard
    }

    val userDisplaySteps = when (selectedTab) {
        LeaderboardTab.TODAY -> detailState.userTodaySteps
        LeaderboardTab.THIS_WEEK -> detailState.userWeekSteps
    }

    fun handleNudgeClick(member: LeaderboardMember) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            pendingNudgeMember = member
            showNotificationRationale = true
        } else {
            viewModel.sendNudge(member.userId, member.displayName)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // 1. Top App Bar: Back arrow, Crew name, Share icon button
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("crew_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = WalkCrewTheme.extendedColors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = detailState.crew.name.ifBlank { "Crew Leaderboard" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WalkCrewTheme.extendedColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { viewModel.shareInviteCode(context) },
                        modifier = Modifier.testTag("crew_detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share invite code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (detailState.isLoading && detailState.crew.crewId.isEmpty()) {
                // Shimmer Loading Skeleton
                ShimmerLeaderboardSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .padding(innerPadding)
                        .padding(horizontal = Spacing.screenHorizontal)
                )
            } else if (layoutInfo.isExpandedWidth) {
                // Rule 12: On Expanded width, show leaderboard and challenge header side by side in two columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1100.dp)
                        .padding(innerPadding)
                        .padding(horizontal = Spacing.screenHorizontal)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Challenge Header Card & Controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ChallengeHeaderCard(
                            currentWeekNumber = detailState.currentWeekNumber,
                            timeLeftFormatted = detailState.timeLeftFormatted,
                            userDisplaySteps = userDisplaySteps,
                            selectedTab = selectedTab,
                            darkTheme = darkTheme
                        )

                        PillSegmentedControl(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (detailState.crew.members.size <= 1) {
                            SingleMemberInviteCard(
                                crewName = detailState.crew.name,
                                inviteCode = detailState.crew.inviteCode,
                                onInviteClick = { viewModel.shareInviteCode(context) }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Right Column: Podium & Leaderboard Rows
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (activeLeaderboard.isNotEmpty()) {
                            item {
                                PodiumSection(
                                    members = activeLeaderboard.take(3),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }

                        val runnersUp = activeLeaderboard.drop(3)
                        if (runnersUp.isNotEmpty()) {
                            items(runnersUp, key = { it.userId }) { member ->
                                val leaderSteps = activeLeaderboard.firstOrNull()?.steps ?: 1L
                                val userReactions = reactionsMap[member.userId] ?: emptyMap()
                                val isNudgedToday = nudgedUsersToday.contains(member.userId)

                                LeaderboardRow(
                                    member = member,
                                    maxSteps = leaderSteps.coerceAtLeast(1L),
                                    reactions = userReactions,
                                    isNudgedToday = isNudgedToday,
                                    onNudgeClick = { handleNudgeClick(member) },
                                    onLongPress = { activeReactionTarget = member },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            } else {
                // Compact / Medium width: Centered single-column layout (max width 640dp)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .padding(innerPadding)
                        .padding(horizontal = Spacing.screenHorizontal)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 2. Challenge Header Card
                    item {
                        ChallengeHeaderCard(
                            currentWeekNumber = detailState.currentWeekNumber,
                            timeLeftFormatted = detailState.timeLeftFormatted,
                            userDisplaySteps = userDisplaySteps,
                            selectedTab = selectedTab,
                            darkTheme = darkTheme
                        )
                    }

                    // 3. Custom Pill Segmented Control
                    item {
                        PillSegmentedControl(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Empty state if only 1 member
                    if (detailState.crew.members.size <= 1) {
                        item {
                            SingleMemberInviteCard(
                                crewName = detailState.crew.name,
                                inviteCode = detailState.crew.inviteCode,
                                onInviteClick = { viewModel.shareInviteCode(context) }
                            )
                        }
                    }

                    // 4. Podium for Top 3
                    if (activeLeaderboard.isNotEmpty()) {
                        item {
                            PodiumSection(
                                members = activeLeaderboard.take(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }

                    // 5. Leaderboard rows for rank 4 and below
                    val runnersUp = activeLeaderboard.drop(3)
                    if (runnersUp.isNotEmpty()) {
                        items(runnersUp, key = { it.userId }) { member ->
                            val leaderSteps = activeLeaderboard.firstOrNull()?.steps ?: 1L
                            val userReactions = reactionsMap[member.userId] ?: emptyMap()
                            val isNudgedToday = nudgedUsersToday.contains(member.userId)

                            LeaderboardRow(
                                member = member,
                                maxSteps = leaderSteps.coerceAtLeast(1L),
                                reactions = userReactions,
                                isNudgedToday = isNudgedToday,
                                onNudgeClick = { handleNudgeClick(member) },
                                onLongPress = { activeReactionTarget = member },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Floating Reactions Bar popup on long-press
            activeReactionTarget?.let { target ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { activeReactionTarget = null },
                    contentAlignment = Alignment.Center
                ) {
                    FloatingReactionsBar(
                        targetName = target.displayName,
                        onReactionSelected = { reactionEmoji ->
                            viewModel.addReaction(target.userId, reactionEmoji)
                            activeReactionTarget = null
                        },
                        onDismiss = { activeReactionTarget = null }
                    )
                }
            }
        }
    }

    // Android 13+ Notification Permission Rationale Dialog
    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Allow WalkCrew Nudges?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "WalkCrew sends friendly notifications when teammates nudge you to get your steps in or when you hit streaks. We never send spam.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text(
                        text = "Enable Notifications",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationale = false }) {
                    Text("Not now", color = WalkCrewTheme.extendedColors.textSecondary)
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Challenge Header Card displaying current week, countdown, and user's steps
 */
@Composable
private fun ChallengeHeaderCard(
    currentWeekNumber: Int,
    timeLeftFormatted: String,
    userDisplaySteps: Long,
    selectedTab: LeaderboardTab,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val headerBackground = if (darkTheme) MaterialTheme.colorScheme.surface else DeepForest
    val headerTextColor = if (darkTheme) WalkCrewTheme.extendedColors.textPrimary else Color(0xFFF1F5EC)
    val headerSubtitleColor = if (darkTheme) WalkCrewTheme.extendedColors.textSecondary else Color(0xFFBAC7BF)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
            .testTag("challenge_header_card"),
        shape = CardShape,
        color = headerBackground,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Week 41" label
                Text(
                    text = "Week $currentWeekNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                )

                // Countdown in lime
                Box(
                    modifier = Modifier
                        .background(LimeAccent.copy(alpha = 0.18f), ButtonShape)
                        .border(1.dp, LimeAccent.copy(alpha = 0.4f), ButtonShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = timeLeftFormatted,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = LimeAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User's total steps in displayLarge tabular font
            Text(
                text = NumberFormat.getNumberInstance(Locale.getDefault()).format(userDisplaySteps),
                style = MaterialTheme.typography.displayLarge,
                color = headerTextColor,
                modifier = Modifier.testTag("user_total_steps_header")
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (selectedTab == LeaderboardTab.THIS_WEEK) "your steps this week" else "your steps today",
                style = MaterialTheme.typography.bodyMedium,
                color = headerSubtitleColor
            )
        }
    }
}

/**
 * Custom Pill Segmented Control ("Today" and "This week")
 */
@Composable
private fun PillSegmentedControl(
    selectedTab: LeaderboardTab,
    onTabSelected: (LeaderboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val containerBg = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = ButtonShape,
        color = containerBg,
        modifier = modifier
            .height(52.dp)
            .border(1.dp, WalkCrewTheme.extendedColors.divider, ButtonShape)
            .testTag("leaderboard_pill_tabs"),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LeaderboardTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ButtonShape)
                        .background(
                            if (isSelected) {
                                if (darkTheme) LimeAccent else DeepForest
                            } else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isSelected) {
                            if (darkTheme) DeepForest else Color(0xFFF1F5EC)
                        } else {
                            WalkCrewTheme.extendedColors.textSecondary
                        }
                    )
                }
            }
        }
    }
}

/**
 * Podium for the Top 3 members:
 * - 3 columns with different heights (1st tallest in middle, 2nd left, 3rd right)
 * - Avatars on top, name, steps, rank circle in gold/silver/bronze
 * - Animated growing columns on load
 */
@Composable
private fun PodiumSection(
    members: List<LeaderboardMember>,
    modifier: Modifier = Modifier
) {
    val layoutInfo = rememberResponsiveLayoutInfo()
    val first = members.getOrNull(0)
    val second = members.getOrNull(1)
    val third = members.getOrNull(2)

    val growthProgress = remember { Animatable(0f) }

    LaunchedEffect(members) {
        growthProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // Rule 3: Define a scale factor based on screen width relative to a 360dp baseline, clamped between 0.85 and 1.25.
    // Apply it to podium column heights, avatar sizes, and text sizes.
    val height1 = layoutInfo.scale(155.dp)
    val height2 = layoutInfo.scale(110.dp)
    val height3 = layoutInfo.scale(85.dp)
    val avatar1 = layoutInfo.scale(52.dp)
    val avatarOther = layoutInfo.scale(44.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Left)
        PodiumColumn(
            member = second,
            rank = 2,
            targetHeight = height2,
            avatarSize = avatarOther,
            rankColor = RankSilver,
            growth = growthProgress.value,
            modifier = Modifier.weight(1f)
        )

        // 1st Place (Middle - Tallest)
        PodiumColumn(
            member = first,
            rank = 1,
            targetHeight = height1,
            avatarSize = avatar1,
            rankColor = RankGold,
            growth = growthProgress.value,
            modifier = Modifier.weight(1.1f)
        )

        // 3rd Place (Right)
        PodiumColumn(
            member = third,
            rank = 3,
            targetHeight = height3,
            avatarSize = avatarOther,
            rankColor = RankBronze,
            growth = growthProgress.value,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PodiumColumn(
    member: LeaderboardMember?,
    rank: Int,
    targetHeight: androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    rankColor: Color,
    growth: Float,
    modifier: Modifier = Modifier
) {
    val animatedHeight = targetHeight * growth

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (member != null) {
            // Avatar with rank circle badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Avatar(
                    initials = member.displayName.take(2),
                    colorIndex = member.avatarColorIndex,
                    size = avatarSize
                )

                // Rank Circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(rankColor, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = DeepForest
                    )
                }
            }

            Text(
                text = member.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = WalkCrewTheme.extendedColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = NumberFormat.getNumberInstance(Locale.getDefault()).format(member.steps),
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Podium Column Pillar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight.coerceAtLeast(8.dp))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    if (rank == 1) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(
                    width = 1.dp,
                    color = WalkCrewTheme.extendedColors.divider,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (rank == 1) MaterialTheme.colorScheme.primary else WalkCrewTheme.extendedColors.textSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Leaderboard row for rank 4 and below:
 * - Rank number (textSecondary)
 * - Avatar
 * - Name
 * - Small outlined pill button "Nudge" (except on current user)
 * - Steps right-aligned in bold
 * - Thin progress bar showing steps relative to leader
 * - Highlight current user with lime tint background (#C8F23C at 18% opacity)
 * - Reaction count chips under the row
 * - Long press to trigger reaction bar
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LeaderboardRow(
    member: LeaderboardMember,
    maxSteps: Long,
    reactions: Map<String, Int>,
    isNudgedToday: Boolean,
    onNudgeClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (member.steps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
    val isCurrentUser = member.isCurrentUser

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentUser) LimeAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isCurrentUser) LimeAccent.copy(alpha = 0.4f) else WalkCrewTheme.extendedColors.divider,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .testTag("leaderboard_row_${member.userId}"),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank number
                Text(
                    text = "#${member.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textSecondary,
                    modifier = Modifier.width(34.dp)
                )

                Avatar(
                    initials = member.displayName.take(2),
                    colorIndex = member.avatarColorIndex,
                    size = 38.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrentUser) "${member.displayName} (You)" else member.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                        color = WalkCrewTheme.extendedColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Small outlined pill button "Nudge" (except current user) - min 48dp touch target
                if (!isCurrentUser) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isNudgedToday) { onNudgeClick() }
                            .testTag("nudge_btn_${member.userId}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (isNudgedToday) WalkCrewTheme.extendedColors.divider else MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isNudgedToday) "Nudged" else "Nudge",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isNudgedToday) WalkCrewTheme.extendedColors.textSecondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Steps in bold right-aligned
                Text(
                    text = NumberFormat.getNumberInstance(Locale.getDefault()).format(member.steps),
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thin progress bar showing steps relative to the leader
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (isCurrentUser) LimeAccent else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Reactions chips under the row
            val activeReactions = reactions.filter { it.value > 0 }
            if (activeReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeReactions.forEach { (emoji, count) ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, WalkCrewTheme.extendedColors.divider, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$emoji $count",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WalkCrewTheme.extendedColors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating bar with 5 reactions (fire, clap, muscle, turtle, rocket) on long-press
 */
@Composable
private fun FloatingReactionsBar(
    targetName: String,
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val reactionsList = listOf(
        "🔥" to "Fire",
        "👏" to "Clap",
        "💪" to "Muscle",
        "🐢" to "Turtle",
        "🚀" to "Rocket"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .border(1.dp, WalkCrewTheme.extendedColors.divider, RoundedCornerShape(24.dp))
            .padding(8.dp)
            .testTag("floating_reactions_bar"),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "React to $targetName",
                style = MaterialTheme.typography.labelSmall,
                color = WalkCrewTheme.extendedColors.textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactionsList.forEach { (emoji, label) ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onReactionSelected(emoji) }
                            .testTag("reaction_option_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 8. Friendly card encouraging user to invite friends if only 1 member
 */
@Composable
private fun SingleMemberInviteCard(
    crewName: String,
    inviteCode: String,
    onInviteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
            .testTag("single_member_invite_card"),
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
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You're the first walker!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WalkCrewTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Crew challenges are better with teammates. Share code $inviteCode with your friends to start competing.",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                text = "Invite friends",
                onClick = onInviteClick,
                leadingIcon = Icons.Rounded.Share,
                modifier = Modifier.fillMaxWidth(),
                testTag = "invite_friends_button"
            )
        }
    }
}

/**
 * 6. Subtle Shimmer Skeleton while loading
 */
@Composable
private fun ShimmerLeaderboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(CardShape)
                .background(WalkCrewTheme.extendedColors.divider.copy(alpha = alpha))
        )

        // Pill tabs skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(ButtonShape)
                .background(WalkCrewTheme.extendedColors.divider.copy(alpha = alpha))
        )

        // Podium skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(CardShape)
                .background(WalkCrewTheme.extendedColors.divider.copy(alpha = alpha))
        )
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun CrewDetailScreenContentPreview() {
    val sampleMembers = listOf(
        LeaderboardMember(
            userId = "user_1",
            displayName = "Alexandria Constantopoulos", // 25 characters to verify Rule 7!
            steps = 24890L,
            rank = 1,
            avatarColorIndex = 2,
            isCurrentUser = false
        ),
        LeaderboardMember(
            userId = "user_2",
            displayName = "Marcus Vance",
            steps = 19430L,
            rank = 2,
            avatarColorIndex = 1,
            isCurrentUser = true
        ),
        LeaderboardMember(
            userId = "user_3",
            displayName = "Sarah Chen",
            steps = 15210L,
            rank = 3,
            avatarColorIndex = 0,
            isCurrentUser = false
        ),
        LeaderboardMember(
            userId = "user_4",
            displayName = "Benjamin Worthington-Smyth", // 26 characters
            steps = 12400L,
            rank = 4,
            avatarColorIndex = 3,
            isCurrentUser = false
        ),
        LeaderboardMember(
            userId = "user_5",
            displayName = "Emma Watson",
            steps = 9850L,
            rank = 5,
            avatarColorIndex = 4,
            isCurrentUser = false
        )
    )

    WalkCrewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ChallengeHeaderCard(
                    currentWeekNumber = 41,
                    timeLeftFormatted = "3d 14h left",
                    userDisplaySteps = 19430L,
                    selectedTab = LeaderboardTab.THIS_WEEK,
                    darkTheme = isSystemInDarkTheme()
                )

                PodiumSection(members = sampleMembers.take(3))

                sampleMembers.drop(3).forEach { member ->
                    LeaderboardRow(
                        member = member,
                        maxSteps = 24890L,
                        reactions = mapOf("🔥" to 2),
                        isNudgedToday = false,
                        onNudgeClick = {},
                        onLongPress = {}
                    )
                }
            }
        }
    }
}
