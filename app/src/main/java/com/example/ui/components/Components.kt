package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActiveTabPillShape
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CardShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.FloatingNavPillShape
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.RingGradientDarkEnd
import com.example.ui.theme.RingGradientDarkStart
import com.example.ui.theme.RingGradientLightEnd
import com.example.ui.theme.RingGradientLightStart
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Primary action button:
 * - Lime background on dark theme, Deep Forest background on light theme
 * - Height: 56dp
 * - Fully rounded pill shape
 * - Flat, no elevation/shadow
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = WalkCrewTheme.extendedColors.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .height(56.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Secondary outlined button:
 * - Height: 56dp
 * - Fully rounded pill shape
 * - 1dp divider border, flat
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String = "secondary_button"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                listOf(WalkCrewTheme.extendedColors.divider, WalkCrewTheme.extendedColors.divider)
            ),
            width = 1.dp
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = WalkCrewTheme.extendedColors.textPrimary,
            disabledContentColor = WalkCrewTheme.extendedColors.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .height(56.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * StatCard:
 * - 28dp radius surface card
 * - 1dp divider border, flat, no shadows
 * - Equal width card for 3-up stat row
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    testTag: String = "stat_card"
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = WalkCrewTheme.extendedColors.divider,
                shape = CardShape
            )
            .testTag(testTag),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WalkCrewTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * SectionHeader:
 * - titleMedium header
 * - Optional trailing action / badge / chip
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WalkCrewTheme.extendedColors.textPrimary
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

/**
 * Avatar:
 * - Colored circle with initials
 * - 8 preset color options
 * - Clean contrast text
 */
@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    colorIndex: Int = 0,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "user_avatar"
) {
    val bgColor = AvatarColors.getOrElse(colorIndex % AvatarColors.size) { AvatarColors[0] }
    // Lime and Gold look best with dark forest text; darker colors with white
    val textColor = if (bgColor == LimeAccent || bgColor == AvatarColors[4]) {
        DeepForest
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = textColor),
                        onClick = onClick
                    )
                } else Modifier
            )
            .testTag(testTag)
            .semantics { contentDescription = "Avatar $initials" },
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = "Avatar photo $initials",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.take(2).uppercase(Locale.getDefault()),
                            color = textColor,
                            style = if (size < 40.dp) {
                                MaterialTheme.typography.labelSmall
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        } else {
            Text(
                text = initials.take(2).uppercase(Locale.getDefault()),
                color = textColor,
                style = if (size < 40.dp) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * StepRing:
 * - 240dp circular progress ring, 18dp stroke, round caps
 * - Track color: surfaceVariant
 * - Sweep gradient:
 *     Dark theme: #C8F23C to #7BD389
 *     Light theme: #0E3B2C to #2E7D5B
 * - Center: today's steps in displayLarge, below "of 8,000 steps" in textSecondary
 * - Spring animation on fill and count up animation on first load
 */
@Composable
fun StepRing(
    currentSteps: Long,
    goalSteps: Long = 8000L,
    modifier: Modifier = Modifier,
    customDiameter: Dp? = null,
    testTag: String = "step_ring"
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallHeight = configuration.screenHeightDp < 640

    val ringDiameter = customDiameter ?: if (isSmallHeight) {
        200.dp
    } else {
        val calcWidth = screenWidth * 0.62f
        if (calcWidth < 280.dp) calcWidth else 280.dp
    }
    val strokeWidthDp = ringDiameter * 0.075f

    val darkTheme = isSystemInDarkTheme()
    val gradientStart = if (darkTheme) RingGradientDarkStart else RingGradientLightStart
    val gradientEnd = if (darkTheme) RingGradientDarkEnd else RingGradientLightEnd
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val targetProgress = (currentSteps.toFloat() / goalSteps.toFloat()).coerceIn(0f, 1.5f)

    // Spring animation for ring progress
    val progressAnim = remember { Animatable(0f) }
    // Animated step counter
    val stepsAnim = remember { Animatable(0f) }

    LaunchedEffect(currentSteps, goalSteps) {
        // Animate ring fill with spring
        progressAnim.animateTo(
            targetValue = targetProgress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(currentSteps) {
        // Number counting up smoothly
        stepsAnim.animateTo(
            targetValue = currentSteps.toFloat(),
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    }

    val displaySteps = stepsAnim.value.toLong()
    val formattedSteps = remember(displaySteps) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(displaySteps)
    }
    val formattedGoal = remember(goalSteps) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(goalSteps)
    }

    Box(
        modifier = modifier
            .size(ringDiameter)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Draw background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Draw progress arc with sweep gradient
            val currentSweep = (progressAnim.value * 360f).coerceAtMost(360f)
            if (currentSweep > 0.5f) {
                // Brush with gradient
                val gradientBrush = Brush.sweepGradient(
                    colors = listOf(gradientStart, gradientEnd, gradientStart),
                    center = center
                )

                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        val numberFontSize = (ringDiameter.value * 0.16f).coerceIn(24f, 44f).sp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = ringDiameter * 0.08f)
        ) {
            Text(
                text = formattedSteps,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = numberFontSize),
                color = WalkCrewTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "of $formattedGoal steps",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = WalkCrewTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class NavigationTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    CREWS("Crews", Icons.Rounded.Group),
    PROFILE("Profile", Icons.Rounded.Person)
}

/**
 * FloatingPillNavBar:
 * - Rounded 999dp
 * - Forest Green (#0E3B2C) in light theme, Surface (#13201A) in dark theme
 * - 64dp tall
 * - 16dp margin from bottom
 * - 3 tabs: Home, Crews, Profile
 * - Selected tab shows a lime pill (#C8F23C) behind icon + label
 */
@Composable
fun FloatingPillNavBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val barBackground = if (darkTheme) MaterialTheme.colorScheme.surface else DeepForest
    val borderDivider = if (darkTheme) WalkCrewTheme.extendedColors.divider else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .border(
                    width = if (darkTheme) 1.dp else 0.dp,
                    color = borderDivider,
                    shape = FloatingNavPillShape
                )
                .testTag("floating_pill_nav_bar"),
            shape = FloatingNavPillShape,
            color = barBackground,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    FloatingNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        onClick = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    tab: NavigationTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activePillColor = LimeAccent
    val activeContentColor = DeepForest
    val inactiveContentColor = Color.White.copy(alpha = 0.65f)

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(ActiveTabPillShape)
            .then(
                if (isSelected) {
                    Modifier.background(activePillColor)
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = activeContentColor),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) activeContentColor else inactiveContentColor,
                modifier = Modifier.size(22.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.title,
                    color = activeContentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun ComponentsPreview() {
    WalkCrewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StepRing(
                    currentSteps = 6420L,
                    goalSteps = 8000L
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Distance",
                        value = "5.2",
                        unit = "km",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Calories",
                        value = "340",
                        unit = "kcal",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Active time",
                        value = "48",
                        unit = "mins",
                        modifier = Modifier.weight(1f)
                    )
                }

                FloatingPillNavBar(
                    selectedTab = NavigationTab.HOME,
                    onTabSelected = {}
                )
            }
        }
    }
}
