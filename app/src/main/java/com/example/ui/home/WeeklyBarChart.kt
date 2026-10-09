package com.example.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyStepData
import com.example.ui.theme.CardShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.WalkCrewTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WeeklyBarChart(
    weeklyData: List<DailyStepData>,
    goalSteps: Long = 8000L,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val todayBarColor = if (darkTheme) LimeAccent else DeepForest
    val otherBarColor = MaterialTheme.colorScheme.surfaceVariant
    val goalLineColor = WalkCrewTheme.extendedColors.textSecondary.copy(alpha = 0.5f)
    val dividerColor = WalkCrewTheme.extendedColors.divider

    // Animation progress from 0f to 1f
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(weeklyData) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Determine max steps for scale (at least 10,000 or 1.25 * max)
    val maxSteps = remember(weeklyData, goalSteps) {
        val peak = weeklyData.maxOfOrNull { it.steps } ?: goalSteps
        (maxOf(peak, goalSteps) * 1.2f).coerceAtLeast(10000f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, dividerColor, CardShape)
            .testTag("weekly_bar_chart_card"),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header stats for the week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                    val totalSteps = weeklyData.sumOf { it.steps }
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalSteps) + " steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                }

                // Goal badge
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CardShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Goal: ${goalSteps / 1000}k / day",
                        style = MaterialTheme.typography.labelSmall,
                        color = WalkCrewTheme.extendedColors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Reserve bottom 24dp for x-axis labels
                    val chartHeight = canvasHeight - 28.dp.toPx()
                    val barCount = if (weeklyData.isEmpty()) 7 else weeklyData.size
                    val slotWidth = canvasWidth / barCount
                    val barWidth = (slotWidth * 0.45f).coerceAtMost(28.dp.toPx())

                    // Draw dashed goal line
                    val goalY = chartHeight - (goalSteps.toFloat() / maxSteps * chartHeight)
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(canvasWidth, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = pathEffect
                    )

                    // Draw 7 rounded bars
                    weeklyData.forEachIndexed { index, data ->
                        val centerX = slotWidth * index + slotWidth / 2f
                        val barHeightFraction = (data.steps.toFloat() / maxSteps).coerceIn(0.04f, 1f)
                        val animatedBarHeight = barHeightFraction * chartHeight * animProgress.value
                        val barTop = chartHeight - animatedBarHeight

                        val barColor = if (data.isToday) todayBarColor else otherBarColor

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(centerX - barWidth / 2f, barTop),
                            size = Size(barWidth, animatedBarHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            // Day Initials Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayInitial,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) {
                                WalkCrewTheme.extendedColors.textPrimary
                            } else {
                                WalkCrewTheme.extendedColors.textSecondary
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
