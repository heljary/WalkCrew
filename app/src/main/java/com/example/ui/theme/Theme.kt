package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val WalkCrewLightColorScheme = lightColorScheme(
    primary = DeepForest,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = DeepForest,
    secondary = LimeAccent,
    onSecondary = DeepForest,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = DeepForest,
    tertiary = CoralHighlight,
    onTertiary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDivider,
    outlineVariant = LightDivider
)

private val WalkCrewDarkColorScheme = darkColorScheme(
    primary = DarkPrimary, // Lime #C8F23C
    onPrimary = DarkOnPrimary, // Deep forest #0E3B2C
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkPrimary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkPrimary,
    tertiary = CoralHighlight,
    onTertiary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    outlineVariant = DarkDivider
)

private val LightExtendedColors = WalkCrewExtendedColors(
    accent = LimeAccent,
    onAccent = DeepForest,
    highlight = CoralHighlight,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    divider = LightDivider,
    rankGold = RankGold,
    rankSilver = RankSilver,
    rankBronze = RankBronze,
    ringGradientStart = RingGradientLightStart,
    ringGradientEnd = RingGradientLightEnd
)

private val DarkExtendedColors = WalkCrewExtendedColors(
    accent = DarkPrimary,
    onAccent = DarkOnPrimary,
    highlight = CoralHighlight,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    divider = DarkDivider,
    rankGold = RankGold,
    rankSilver = RankSilver,
    rankBronze = RankBronze,
    ringGradientStart = RingGradientDarkStart,
    ringGradientEnd = RingGradientDarkEnd
)

object WalkCrewTheme {
    val extendedColors: WalkCrewExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWalkCrewExtendedColors.current
}

@Composable
fun WalkCrewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is explicitly turned off as per design system requirements
    val colorScheme: ColorScheme = if (darkTheme) WalkCrewDarkColorScheme else WalkCrewLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalWalkCrewExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = WalkCrewShapes,
            content = content
        )
    }
}
