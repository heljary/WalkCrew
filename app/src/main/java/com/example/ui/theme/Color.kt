package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette
val LightBackground = Color(0xFFF6F7F2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFECEFE6)
val DeepForest = Color(0xFF0E3B2C)
val LightOnPrimary = Color(0xFFF6F7F2)
val LimeAccent = Color(0xFFC8F23C)
val CoralHighlight = Color(0xFFFF6B4A)
val LightTextPrimary = Color(0xFF0E1A14)
val LightTextSecondary = Color(0xFF5B6B62)
val LightDivider = Color(0xFFE2E6DB)

// Dark Palette
val DarkBackground = Color(0xFF09120D)
val DarkSurface = Color(0xFF13201A)
val DarkSurfaceVariant = Color(0xFF1B2B23)
val DarkPrimary = Color(0xFFC8F23C)
val DarkOnPrimary = Color(0xFF0E3B2C)
val DarkTextPrimary = Color(0xFFF1F5EC)
val DarkTextSecondary = Color(0xFFB0B0B0) // Elevated contrast lighter gray
val DarkTextTertiary = Color(0xFF9E9E9E)  // Subtle tertiary labels
val DarkDivider = Color(0xFF22342B)

// Ring Gradient Colors
val RingGradientDarkStart = Color(0xFFC8F23C)
val RingGradientDarkEnd = Color(0xFF7BD389)
val RingGradientLightStart = Color(0xFF0E3B2C)
val RingGradientLightEnd = Color(0xFF2E7D5B)

// Rank Colors
val RankGold = Color(0xFFF5C542)
val RankSilver = Color(0xFFB8C2CC)
val RankBronze = Color(0xFFD9915B)
val CoralAlert = Color(0xFFFF6B4A)
val GoldStar = Color(0xFFF5C542)

// Avatar Preset Colors (8 options)
val AvatarColors = listOf(
    Color(0xFF0E3B2C), // Deep Forest
    Color(0xFF2E7D5B), // Pine
    Color(0xFFC8F23C), // Lime
    Color(0xFFFF6B4A), // Coral
    Color(0xFFF5C542), // Gold
    Color(0xFF388E3C), // Emerald
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF7C3AED)  // Violet
)

@Immutable
data class WalkCrewExtendedColors(
    val accent: Color,
    val onAccent: Color,
    val highlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color = DarkTextTertiary,
    val divider: Color,
    val rankGold: Color = RankGold,
    val rankSilver: Color = RankSilver,
    val rankBronze: Color = RankBronze,
    val ringGradientStart: Color,
    val ringGradientEnd: Color
)

val LocalWalkCrewExtendedColors = staticCompositionLocalOf {
    WalkCrewExtendedColors(
        accent = LimeAccent,
        onAccent = DeepForest,
        highlight = CoralHighlight,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        divider = LightDivider,
        ringGradientStart = RingGradientLightStart,
        ringGradientEnd = RingGradientLightEnd
    )
}
