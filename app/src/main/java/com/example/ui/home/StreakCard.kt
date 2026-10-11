package com.example.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyStepData
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.BottomSheetShape
import com.example.ui.theme.CardShape
import com.example.ui.theme.CoralHighlight
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.RankGold
import com.example.ui.theme.WalkCrewTheme
import kotlin.random.Random

/**
 * Streaks Card:
 * - Flame icon in coral (#FF6B4A)
 * - Streak number in headlineMedium
 * - Row of last 7 days as small circles (filled lime if goal reached, empty outline if not, today pulsing)
 * - Milestone trigger on click or on load for 3, 7, 14, 30 days with Canvas confetti
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakCard(
    weeklyHistory: List<DailyStepData>,
    dailyGoal: Long,
    currentStreakDays: Int,
    modifier: Modifier = Modifier,
    onMilestoneShown: (Int) -> Unit = {}
) {
    var showMilestoneSheet by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Pulse animation for today's circle indicator
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
            .clickable {
                // If user has reached a milestone (3, 7, 14, 30) or wants to view celebration
                val milestone = when {
                    currentStreakDays >= 30 -> 30
                    currentStreakDays >= 14 -> 14
                    currentStreakDays >= 7 -> 7
                    currentStreakDays >= 3 -> 3
                    else -> 3 // preview
                }
                showMilestoneSheet = milestone
            }
            .testTag("streak_card"),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flame icon in coral (#FF6B4A)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(CoralHighlight.copy(alpha = 0.16f), CircleShape)
                            .border(1.dp, CoralHighlight.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Streak flame",
                            tint = CoralHighlight,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currentStreakDays",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = WalkCrewTheme.extendedColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentStreakDays == 1) "day streak" else "days streak",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WalkCrewTheme.extendedColors.textSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Text(
                            text = if (currentStreakDays > 0) "Keep up the momentum!" else "Hit your goal today to start a streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = WalkCrewTheme.extendedColors.textSecondary
                        )
                    }
                }

                // Milestone Chip indicator
                if (currentStreakDays in listOf(3, 7, 14, 30)) {
                    Box(
                        modifier = Modifier
                            .background(LimeAccent, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Milestone!",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DeepForest
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Row of the last 7 days as small circles
            // filled lime if goal reached, empty outline if not, today pulsing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weeklyHistory.forEach { day ->
                    val isGoalMet = day.steps >= dailyGoal
                    val isToday = day.isToday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.dayInitial,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) Color(0xFFC6FF00) else Color(0xFF9E9E9E),
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isToday) {
                                // Pulsing ring behind today's circle
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(pulseAnim.value)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isGoalMet) LimeAccent else CoralHighlight,
                                            shape = CircleShape
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isGoalMet -> LimeAccent
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            isGoalMet -> LimeAccent
                                            isToday -> CoralHighlight
                                            else -> WalkCrewTheme.extendedColors.divider
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isGoalMet) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = DeepForest,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Milestone Celebration Bottom Sheet
    showMilestoneSheet?.let { milestoneDays ->
        ModalBottomSheet(
            onDismissRequest = { showMilestoneSheet = null },
            sheetState = sheetState,
            shape = BottomSheetShape,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.widthIn(max = 600.dp)) {
                    MilestoneCelebrationSheet(
                        streakDays = milestoneDays,
                        onDismiss = {
                            showMilestoneSheet = null
                            onMilestoneShown(milestoneDays)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Milestone bottom sheet at 3, 7, 14, 30 days with a confetti animation drawn in Canvas
 * using the brand colors (#C8F23C, #0E3B2C, #FF6B4A, #F5C542).
 */
@Composable
fun MilestoneCelebrationSheet(
    streakDays: Int,
    onDismiss: () -> Unit
) {
    val confettiAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        confettiAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("milestone_sheet")
    ) {
        // Confetti Canvas animation drawn in brand colors
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            val progress = confettiAnim.value
            val confettiColors = listOf(LimeAccent, DeepForest, CoralHighlight, RankGold)
            val random = Random(42) // Stable seed for deterministic visual paths

            val count = 48
            for (i in 0 until count) {
                val startX = random.nextFloat() * size.width
                val speed = 0.7f + random.nextFloat() * 0.6f
                val y = (progress * size.height * speed + (i * 7f)) % size.height
                val color = confettiColors[i % confettiColors.size]
                val pWidth = 8.dp.toPx()
                val pHeight = 14.dp.toPx()
                val rotation = progress * 360f * (if (i % 2 == 0) 1 else -1)

                drawRect(
                    color = color,
                    topLeft = Offset(
                        x = (startX + kotlin.math.sin((progress * 6f + i).toDouble()).toFloat() * 24.dp.toPx()).coerceIn(0f, size.width - pWidth),
                        y = y
                    ),
                    size = Size(pWidth, pHeight)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CoralHighlight.copy(alpha = 0.16f), CircleShape)
                    .border(2.dp, CoralHighlight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = CoralHighlight,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$streakDays Day Streak!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = WalkCrewTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (streakDays) {
                    3 -> "3 consecutive days reaching your daily step goal. Consistency builds champions!"
                    7 -> "A full week of non-stop action! You've unlocked the 7-day walker badge."
                    14 -> "Two weeks straight! You're an inspiration to your entire crew."
                    30 -> "30 days of dedication! You're officially a WalkCrew legend."
                    else -> "Incredible commitment to your daily health goals!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Keep Walking",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "milestone_dismiss_button"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
}
