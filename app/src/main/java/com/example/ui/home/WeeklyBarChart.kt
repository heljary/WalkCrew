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
    // Primary accent lime (#C6FF00) for days with recorded steps
    val accentLime = Color(0xFFC6FF00)
    // Subtle background track structure for empty/zero-step days (#1E281E)
    val trackBgColor = if (darkTheme) Color(0xFF1E281E) else Color(0xFFE2E9E2)
    // Dotted goal threshold line clearly visible across the chart columns
    val goalLineColor = if (darkTheme) Color(0xFFB0B0B0).copy(alpha = 0.65f) else Color(0xFF5B6B62).copy(alpha = 0.65f)
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
                        color = Color(0xFFB0B0B0)
                    )
                    val totalSteps = weeklyData.sumOf { it.steps }
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalSteps) + " steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WalkCrewTheme.extendedColors.textPrimary
                    )
                }

                // Goal badge with high contrast
                Box(
                    modifier = Modifier
                        .background(
                            color = if (darkTheme) Color(0xFF1E281E) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CardShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (darkTheme) Color(0xFF2A3D2E) else Color.Transparent,
                            shape = CardShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Goal: ${goalSteps / 1000}k / day",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0B0B0),
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

                    val chartHeight = canvasHeight - 20.dp.toPx()
                    val barCount = if (weeklyData.isEmpty()) 7 else weeklyData.size
                    val slotWidth = canvasWidth / barCount

                    // Daily bar thickness: 12.dp with RoundedCornerShape
                    val barWidth = 12.dp.toPx().coerceAtMost(slotWidth * 0.45f)
                    val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                    // 1. Subtle background track structure for all days (empty/zero-step days retain this)
                    weeklyData.forEachIndexed { index, _ ->
                        val centerX = slotWidth * index + slotWidth / 2f
                        val left = centerX - barWidth / 2f

                        drawRoundRect(
                            color = trackBgColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 2. Dotted goal threshold line clearly visible across the chart columns
                    val goalY = chartHeight - (goalSteps.toFloat() / maxSteps * chartHeight)
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(canvasWidth, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = pathEffect
                    )

                    // 3. Any day with recorded steps fills upward with primary accent lime (#C6FF00)
                    weeklyData.forEachIndexed { index, data ->
                        if (data.steps > 0) {
                            val centerX = slotWidth * index + slotWidth / 2f
                            val left = centerX - barWidth / 2f

                            val barHeightFraction = (data.steps.toFloat() / maxSteps).coerceIn(0f, 1f)
                            val animatedBarHeight = (barHeightFraction * chartHeight * animProgress.value)
                                .coerceAtLeast(barWidth)

                            val barTop = chartHeight - animatedBarHeight
                            drawRoundRect(
                                color = accentLime,
                                topLeft = Offset(left, barTop),
                                size = Size(barWidth, animatedBarHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Initials Labels Row with high-contrast text
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
                            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (day.isToday) {
                                Color(0xFFC6FF00)
                            } else {
                                Color(0xFF9E9E9E)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
