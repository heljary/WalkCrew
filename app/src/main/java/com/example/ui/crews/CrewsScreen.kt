package com.example.ui.crews

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CrewWithMembers
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
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewsScreen(
    viewModel: CrewViewModel,
    modifier: Modifier = Modifier,
    onCrewClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val crews by viewModel.crews.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val showCreateSheet by viewModel.showCreateSheet.collectAsState()
    val showJoinSheet by viewModel.showJoinSheet.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val paywallReason by viewModel.paywallReason.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Walk Crews",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = WalkCrewTheme.extendedColors.textPrimary
                        )
                        Text(
                            text = "Step together and climb group leaderboards",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WalkCrewTheme.extendedColors.textSecondary
                        )
                    }

                    if (crews.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.openCreateSheet() },
                            modifier = Modifier.testTag("add_crew_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Create Crew",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick Actions if user already has crews
            if (crews.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrimaryButton(
                            text = "Create a crew",
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

                item {
                    SectionHeader(title = "Your Active Crews")
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
            } else {
                // Empty State: Centered card with "No crews yet" and two buttons
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    EmptyCrewsCard(
                        onCreateClick = { viewModel.openCreateSheet() },
                        onJoinClick = { viewModel.openJoinSheet() }
                    )
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
                text = "Walk together, track progress on shared leaderboards, and win challenges together.",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Create a crew",
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
 * Crew list card:
 * - 28dp radius
 * - crew name (titleMedium)
 * - member count
 * - row of up to 4 overlapping Avatars
 * - user's current rank as a lime pill chip like "#2 this week"
 * - share invite action
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
            // Top Row: Crew Name & Lime Rank Pill Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = crew.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )

                // Lime Pill Chip: e.g. "#1 this week" or "#2 this week"
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

            // Member Count & Duration
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Group,
                    contentDescription = null,
                    tint = WalkCrewTheme.extendedColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$memberCount ${if (memberCount == 1) "walker" else "walkers"} • ${crew.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Row: Overlapping Avatars (up to 4) + Share Invite button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overlapping Avatars (up to 4)
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width((32 + (crewWithMembers.memberProfiles.size.coerceAtMost(4) - 1).coerceAtLeast(0) * 22).dp)
                ) {
                    val displayMembers = if (crewWithMembers.memberProfiles.isNotEmpty()) {
                        crewWithMembers.memberProfiles.take(4)
                    } else {
                        // Fallback avatar items based on members count
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

                // Share invite button
                SecondaryButton(
                    text = "Share Code",
                    onClick = { onShareInvite(crew.inviteCode) },
                    leadingIcon = Icons.Rounded.Share,
                    modifier = Modifier.height(44.dp),
                    testTag = "share_invite_btn_${crew.crewId}"
                )
            }
        }
    }
}

/**
 * Bottom Sheet: Create a crew
 * - 32dp top corners (BottomSheetShape)
 * - name field
 * - challenge duration (Weekly default)
 * - button
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            // Duration selector pills
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

/**
 * Bottom Sheet: Join with code
 * - 32dp top corners (BottomSheetShape)
 * - 6-box code input (each box 48dp, rounded 14dp, auto-advance focus)
 * - Join button
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            // 6-box code input (each box 48dp, rounded 14dp, auto-advance focus)
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
