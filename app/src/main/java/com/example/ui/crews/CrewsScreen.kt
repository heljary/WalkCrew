package com.example.ui.crews

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.util.PluralUtils
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CrewModel
import com.example.data.models.CrewWithMembers
import com.example.data.models.LeaderboardMember
import com.example.ui.components.Avatar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BottomSheetShape
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
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewsScreen(
    viewModel: CrewViewModel,
    modifier: Modifier = Modifier,
    onCrewClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val crews by viewModel.crews.collectAsState()
    val activeCrew by viewModel.activeCrew.collectAsState()
    val friendsProgress by viewModel.friendsProgress.collectAsState()
    val friendFilter by viewModel.friendFilter.collectAsState()
    val nudgedFriends by viewModel.nudgedFriends.collectAsState()
    val friendReactions by viewModel.friendReactions.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val showCreateSheet by viewModel.showCreateSheet.collectAsState()
    val showJoinSheet by viewModel.showJoinSheet.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val paywallReason by viewModel.paywallReason.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Filter friends list based on selected filter
    val filteredFriends = remember(friendsProgress, friendFilter) {
        when (friendFilter) {
            FriendFilter.RANKED -> friendsProgress.sortedByDescending { it.steps }
            FriendFilter.GOAL_MET -> friendsProgress.filter { it.steps >= it.dailyStepGoal }
            FriendFilter.ALL -> friendsProgress
        }
    }

    val currentUserSteps = remember(friendsProgress) {
        friendsProgress.find { it.isCurrentUser }?.steps ?: 0L
    }

    val totalCrewDailySteps = remember(friendsProgress) {
        friendsProgress.sumOf { it.steps }
    }

    val totalCrewDailyGoal = remember(friendsProgress) {
        friendsProgress.sumOf { it.dailyStepGoal }.coerceAtLeast(10000L)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val navBarBottomPadding = 24.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = 12.dp,
                    bottom = navBarBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with title and quick actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeCrew?.crew?.name?.ifBlank { "Crew" } ?: "Crew",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = WalkCrewTheme.extendedColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Friends' daily steps & group progress",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkCrewTheme.extendedColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Join / Add quick icon
                            IconButton(
                                onClick = { viewModel.openJoinSheet() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, WalkCrewTheme.extendedColors.divider, CircleShape)
                                    .testTag("join_crew_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = "Join Crew",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Create Crew quick icon
                            IconButton(
                                onClick = { viewModel.openCreateSheet() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(LimeAccent)
                                    .testTag("add_crew_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Create Crew",
                                    tint = DeepForest,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Multiple Crews Switcher Tabs (if user is in multiple crews)
                if (crews.size > 1) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(crews, key = { it.crew.crewId }) { item ->
                                val isSelected = item.crew.crewId == activeCrew?.crew?.crewId
                                Box(
                                    modifier = Modifier
                                        .clip(ButtonShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else WalkCrewTheme.extendedColors.divider,
                                            shape = ButtonShape
                                        )
                                        .clickable { viewModel.selectCrew(item.crew.crewId) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("crew_tab_${item.crew.crewId}")
                                ) {
                                    Text(
                                        text = item.crew.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else WalkCrewTheme.extendedColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Crew Overview Hero Card or Empty State
                item {
                    if (crews.isEmpty()) {
                        EmptyCrewsCard(
                            onCreateClick = { viewModel.openCreateSheet() },
                            onJoinClick = { viewModel.openJoinSheet() }
                        )
                    } else {
                        val crew = activeCrew?.crew ?: crews.first().crew
                        val crewName = crew.name
                        val inviteCode = crew.inviteCode
                        val duration = crew.duration

                        CrewOverviewHeroCard(
                            crewName = crewName,
                            duration = duration,
                            totalStepsToday = totalCrewDailySteps,
                            totalGoalToday = totalCrewDailyGoal,
                            memberCount = friendsProgress.size,
                            inviteCode = inviteCode,
                            onShareCode = { viewModel.shareInviteCode(context, inviteCode) },
                            onCopyCode = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("WalkCrew Code", inviteCode))
                                Toast.makeText(context, "Code $inviteCode copied! 📋", Toast.LENGTH_SHORT).show()
                            },
                            onCardClick = {
                                onCrewClick(crew.crewId)
                            }
                        )
                    }
                }

                // Friends & Daily Step Progress Section Header + Filter Pills
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Friends' Daily Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WalkCrewTheme.extendedColors.textPrimary
                                )

                                // Live pulse indicator
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LimeAccent)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${friendsProgress.size} active",
                                        fontFamily = PlusJakartaSansFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForest
                                    )
                                }
                            }

                            // Tap to view full crew leaderboard / details
                            if (activeCrew != null) {
                                Text(
                                    text = "Full Stats ›",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onCrewClick(activeCrew!!.crew.crewId) }
                                        .padding(vertical = 4.dp)
                                        .testTag("view_full_stats_btn")
                                )
                            }
                        }

                        // Filter Pills: Leaderboard, Goal Met, All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FriendFilter.entries.forEach { filter ->
                                val isSelected = friendFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else WalkCrewTheme.extendedColors.divider,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setFilter(filter) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                        .testTag("filter_chip_${filter.name.lowercase()}")
                                ) {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else WalkCrewTheme.extendedColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Friends List - Cards featuring friend info and daily step progress
                if (filteredFriends.isEmpty()) {
                    item {
                        Surface(
                            shape = CardShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                    contentDescription = null,
                                    tint = WalkCrewTheme.extendedColors.textSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No friends match this filter",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WalkCrewTheme.extendedColors.textPrimary
                                )
                                Text(
                                    text = "Try selecting 'All Friends' to see everyone's progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WalkCrewTheme.extendedColors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredFriends, key = { it.userId }) { friend ->
                        val isNudged = nudgedFriends.contains(friend.userId)
                        val reactions = friendReactions[friend.userId] ?: emptyMap()

                        FriendDailyProgressCard(
                            member = friend,
                            currentUserSteps = currentUserSteps,
                            isNudged = isNudged,
                            reactions = reactions,
                            onNudgeClick = {
                                viewModel.sendNudge(friend.userId, friend.displayName)
                            },
                            onCheerClick = { emoji ->
                                viewModel.cheerFriend(friend.userId, friend.displayName, emoji)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Quick Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton(
                            text = "Create crew",
                            onClick = { viewModel.openCreateSheet() },
                            modifier = Modifier.weight(1f),
                            leadingIcon = Icons.Rounded.Add,
                            testTag = "create_crew_quick_btn"
                        )
                        SecondaryButton(
                            text = "Join with code",
                            onClick = { viewModel.openJoinSheet() },
                            modifier = Modifier.weight(1f),
                            testTag = "join_crew_quick_btn"
                        )
                    }
                }

                // If user has other active crews, list them below
                if (crews.size > 1) {
                    item {
                        SectionHeader(title = "All Your Crews")
                    }

                    items(crews, key = { it.crew.crewId }) { item ->
                        CrewCard(
                            crewWithMembers = item,
                            onClick = { onCrewClick(item.crew.crewId) },
                            onShareInvite = { code ->
                                viewModel.shareInviteCode(context, code)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Sheets
        if (showCreateSheet) {
            CreateCrewBottomSheet(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onDismiss = { viewModel.closeCreateSheet() },
                onCreate = { name, duration ->
                    viewModel.createCrew(name, duration)
                }
            )
        }

        if (showJoinSheet) {
            JoinCrewBottomSheet(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onDismiss = { viewModel.closeJoinSheet() },
                onJoin = { code ->
                    viewModel.joinCrewWithCode(code)
                }
            )
        }

        if (showPaywall) {
            com.example.ui.paywall.PaywallScreen(
                onDismiss = { viewModel.closePaywall() },
                triggerReason = paywallReason
            )
        }
    }
}

/**
 * Hero Overview card of the crew:
 * - Collective daily steps progress bar
 * - Member count
 * - Invite code with 1-tap copy & share
 */
@Composable
fun CrewOverviewHeroCard(
    crewName: String,
    duration: String,
    totalStepsToday: Long,
    totalGoalToday: Long,
    memberCount: Int,
    inviteCode: String,
    onShareCode: () -> Unit,
    onCopyCode: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (totalStepsToday.toFloat() / totalGoalToday.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "crew_progress"
    )

    Surface(
        shape = CardShape,
        color = DeepForest,
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable { onCardClick() }
            .testTag("crew_overview_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Header Row: Crew Name + Duration Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = crewName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${PluralUtils.friends(memberCount)} walking together • $duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBAC7BF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Lime Accent Pill
                Box(
                    modifier = Modifier
                        .clip(ButtonShape)
                        .background(LimeAccent)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Active",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Crew Daily Steps Progress Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Crew Steps Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBAC7BF)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalStepsToday),
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = LimeAccent
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}% of goal",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smooth Lime Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = LimeAccent,
                trackColor = Color(0xFF1B4E3C)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Code & Share Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Code box with copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF164837))
                        .clickable { onCopyCode() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("copy_invite_code_btn")
                ) {
                    Text(
                        text = "Code: ",
                        fontSize = 12.sp,
                        color = Color(0xFFBAC7BF)
                    )
                    Text(
                        text = inviteCode,
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LimeAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFBAC7BF),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Share button (min 48dp touch target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable { onShareCode() }
                        .testTag("share_hero_code_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Friend's Daily Step Progress Card:
 * - Rank medal/badge (#1, #2, #3, #4+)
 * - Avatar with status pulse / streak chip
 * - Name with "(You)" tag
 * - Current daily step count formatted
 * - Daily goal & % completed with custom animated progress bar
 * - Relative step diff vs you ("+1,200 ahead" or "on track")
 * - Quick Nudge button ("👟 Nudge")
 * - Quick Cheer emoji buttons
 */
@Composable
fun FriendDailyProgressCard(
    member: LeaderboardMember,
    currentUserSteps: Long,
    isNudged: Boolean,
    reactions: Map<String, Int>,
    onNudgeClick: () -> Unit,
    onCheerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentUser = member.isCurrentUser
    val goal = member.dailyStepGoal.coerceAtLeast(1000L)
    val progressRatio = (member.steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val percentInt = (progressRatio * 100).toInt()
    val isGoalMet = member.steps >= goal

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "friend_step_progress"
    )

    Surface(
        shape = CardShape,
        color = if (isCurrentUser) LimeAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isCurrentUser) LimeAccent.copy(alpha = 0.5f) else WalkCrewTheme.extendedColors.divider,
                shape = CardShape
            )
            .testTag("friend_card_${member.userId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Rank Badge + Avatar + Friend Details + Daily Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (member.rank) {
                                1 -> RankGold.copy(alpha = 0.2f)
                                2 -> RankSilver.copy(alpha = 0.2f)
                                3 -> RankBronze.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (member.rank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "#${member.rank}"
                        },
                        fontSize = if (member.rank <= 3) 15.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (member.rank) {
                            1 -> RankGold
                            2 -> RankSilver
                            3 -> RankBronze
                            else -> WalkCrewTheme.extendedColors.textSecondary
                        }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Friend Avatar
                Box {
                    Avatar(
                        initials = member.displayName.take(2).ifBlank { "W" },
                        colorIndex = member.avatarColorIndex,
                        size = 42.dp
                    )

                    // Streak Badge or active dot
                    if (member.streakDays > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .clip(CircleShape)
                                .background(DeepForest)
                                .border(1.dp, Color.White, CircleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "🔥${member.streakDays}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = LimeAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Friend Name & Status
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = member.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold,
                            color = WalkCrewTheme.extendedColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isCurrentUser) {
                            Box(
                                modifier = Modifier
                                    .clip(ButtonShape)
                                    .background(LimeAccent)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "You",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForest
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isGoalMet) "Goal crushed! 🎯" else member.statusText.ifBlank { "Walking today" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGoalMet) MaterialTheme.colorScheme.primary else WalkCrewTheme.extendedColors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Daily Step Count
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.getDefault()).format(member.steps),
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                    Text(
                        text = "$percentInt% of goal",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGoalMet) MaterialTheme.colorScheme.primary else WalkCrewTheme.extendedColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Daily Step Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isGoalMet) LimeAccent else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Step pace difference vs you + Interactive Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pace difference text
                val paceDiffText = when {
                    isCurrentUser -> "${NumberFormat.getNumberInstance(Locale.getDefault()).format(goal - member.steps).takeIf { goal > member.steps } ?: "0"} steps to goal"
                    member.steps > currentUserSteps -> "+${NumberFormat.getNumberInstance(Locale.getDefault()).format(member.steps - currentUserSteps)} ahead"
                    member.steps < currentUserSteps -> "-${NumberFormat.getNumberInstance(Locale.getDefault()).format(currentUserSteps - member.steps)} behind"
                    else -> "Tied with you"
                }

                Text(
                    text = paceDiffText,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                // Interactive Buttons (hidden on current user's own row)
                if (!isCurrentUser) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Cheer Reaction Pills
                        listOf("🔥", "👏", "💪").forEach { emoji ->
                            val count = reactions[emoji] ?: 0
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onCheerClick(emoji) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (count > 0) "$emoji $count" else emoji,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Nudge Button (for friends, min 48dp touch target)
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isNudged) { onNudgeClick() }
                                .testTag("nudge_friend_btn_${member.userId}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isNudged) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isNudged) WalkCrewTheme.extendedColors.divider
                                        else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isNudged) "Nudged ✓" else "👟 Nudge",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isNudged) WalkCrewTheme.extendedColors.textSecondary
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // Current user: display any received cheers from teammates as read-only badges
                    val activeReactions = reactions.filter { it.value > 0 }
                    if (activeReactions.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeReactions.forEach { (emoji, count) ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$emoji $count",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centered empty state card with two buttons: "Create a crew" and "Join with code"
 */
@Composable
fun EmptyCrewsCard(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
            .testTag("empty_crews_card"),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No crews yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WalkCrewTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Walk together, track friends' daily steps, and climb group leaderboards.",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Create crew",
                onClick = onCreateClick,
                leadingIcon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth(),
                testTag = "empty_create_crew_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Join with code",
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "empty_join_code_button"
            )
        }
    }
}

/**
 * Crew list card for multiple crews overview
 */
@Composable
fun CrewCard(
    crewWithMembers: CrewWithMembers,
    onClick: () -> Unit = {},
    onShareInvite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val crew = crewWithMembers.crew
    val memberCount = crew.members.size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
            .clip(CardShape)
            .clickable { onClick() }
            .testTag("crew_card_${crew.crewId}"),
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
                Text(
                    text = crew.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WalkCrewTheme.extendedColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(LimeAccent, ButtonShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("rank_pill_chip")
                ) {
                    Text(
                        text = "#${crewWithMembers.userRank} this week",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DeepForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Group,
                    contentDescription = null,
                    tint = WalkCrewTheme.extendedColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${PluralUtils.walkers(memberCount)} • ${crew.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkCrewTheme.extendedColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width((32 + (crewWithMembers.memberProfiles.size.coerceAtMost(4) - 1).coerceAtLeast(0) * 22).dp)
                ) {
                    val displayMembers = if (crewWithMembers.memberProfiles.isNotEmpty()) {
                        crewWithMembers.memberProfiles.take(4)
                    } else {
                        crew.members.take(4).mapIndexed { idx, id ->
                            com.example.data.models.UserProfile(
                                userId = id,
                                displayName = "W${idx + 1}",
                                avatarColorIndex = idx
                            )
                        }
                    }

                    displayMembers.forEachIndexed { index, profile ->
                        Box(
                            modifier = Modifier
                                .offset(x = (index * 20).dp)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Avatar(
                                initials = profile.displayName.take(2).ifBlank { "W" },
                                colorIndex = profile.avatarColorIndex,
                                size = 32.dp
                            )
                        }
                    }
                }

                SecondaryButton(
                    text = "Share Code",
                    onClick = { onShareInvite(crew.inviteCode) },
                    leadingIcon = Icons.Rounded.Share,
                    modifier = Modifier.height(48.dp),
                    testTag = "share_invite_btn_${crew.crewId}"
                )
            }
        }
    }
}

/**
 * Bottom Sheet: Create a crew
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCrewBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, duration: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var crewName by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("Weekly") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(WalkCrewTheme.extendedColors.divider, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = Spacing.screenHorizontal)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Create a Crew",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Start a group step challenge with your friends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Crew Name",
                    style = MaterialTheme.typography.labelSmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = crewName,
                    onValueChange = { crewName = it },
                    placeholder = { Text("e.g. Sunrise Striders") },
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
                        .testTag("crew_name_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Challenge Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Weekly", "Monthly", "Ongoing").forEach { duration ->
                        val isSelected = duration == selectedDuration
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = ButtonShape
                                )
                                .clickable { selectedDuration = duration }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("duration_$duration"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else WalkCrewTheme.extendedColors.textPrimary
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.highlight
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                PrimaryButton(
                    text = if (isLoading) "Creating crew..." else "Create Crew",
                    onClick = {
                        if (crewName.isNotBlank()) {
                            onCreate(crewName.trim(), selectedDuration)
                        }
                    },
                    enabled = crewName.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_create_crew_btn"
                )
            }
        }
    }
}

/**
 * Bottom Sheet: Join with code
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinCrewBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var codeInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(WalkCrewTheme.extendedColors.divider, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = Spacing.screenHorizontal)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join with Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WalkCrewTheme.extendedColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter the 6-character code shared by your crew leader",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkCrewTheme.extendedColors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                SixBoxCodeInput(
                    code = codeInput,
                    onCodeChange = { codeInput = it },
                    onComplete = { completedCode ->
                        onJoin(completedCode)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = WalkCrewTheme.extendedColors.highlight,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                PrimaryButton(
                    text = if (isLoading) "Joining crew..." else "Join Crew",
                    onClick = {
                        if (codeInput.length == 6) {
                            onJoin(codeInput)
                        }
                    },
                    enabled = codeInput.length == 6 && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_join_crew_btn"
                )
            }
        }
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun CrewsScreenPreview() {
    WalkCrewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Walk Crews",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
