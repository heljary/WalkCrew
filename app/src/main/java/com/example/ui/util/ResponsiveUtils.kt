package com.example.ui.util

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * CompositionLocal holding current WindowSizeClass if available
 */
val LocalAppWindowSizeClass = compositionLocalOf<WindowSizeClass?> { null }

/**
 * Responsive layout information computed for the active screen
 */
data class ResponsiveLayoutInfo(
    val windowSizeClass: WindowSizeClass?,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isCompactWidth: Boolean,
    val isMediumWidth: Boolean,
    val isExpandedWidth: Boolean,
    val isCompactHeight: Boolean,
    val isSmallHeight: Boolean, // < 640dp
    val isLandscape: Boolean,
    val scaleFactor: Float // Clamped between 0.85f and 1.25f based on 360dp width baseline
) {
    /**
     * Scale a dimension (Dp) according to the width scale factor
     */
    fun scale(base: Dp): Dp = base * scaleFactor

    /**
     * Scale text size, guaranteeing minimum readability limits (11sp for labels, 14sp for body)
     */
    fun scaleText(base: TextUnit, minSp: TextUnit = 11.sp): TextUnit {
        val scaled = base.value * scaleFactor
        return max(scaled, minSp.value).sp
    }
}

/**
 * Single helper function to compute responsive layout parameters across all screens.
 * Uses Material3 WindowSizeClass when available, and adapts according to orientation and dimensions.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberResponsiveLayoutInfo(): ResponsiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = context as? Activity

    val windowSizeClass = LocalAppWindowSizeClass.current ?: activity?.let {
        runCatching { calculateWindowSizeClass(it) }.getOrNull()
    }

    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp > configuration.screenHeightDp

    val isCompactWidth = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact ||
            configuration.screenWidthDp < 600
    val isMediumWidth = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium ||
            (configuration.screenWidthDp in 600..839)
    val isExpandedWidth = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded ||
            configuration.screenWidthDp >= 840

    val isCompactHeight = windowSizeClass?.heightSizeClass == WindowHeightSizeClass.Compact ||
            configuration.screenHeightDp < 480
    val isSmallHeight = configuration.screenHeightDp < 640

    // In portrait, scale relative to 360dp baseline; in landscape, clamp safely
    val effectiveWidth = if (isLandscape) {
        // In landscape, base scale on portrait proportion or clamped dimension
        (configuration.screenHeightDp * 1.3f).coerceAtMost(configuration.screenWidthDp.toFloat())
    } else {
        configuration.screenWidthDp.toFloat()
    }

    val rawScale = effectiveWidth / 360f
    val scaleFactor = rawScale.coerceIn(0.85f, 1.25f)

    return remember(
        windowSizeClass,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.orientation,
        scaleFactor
    ) {
        ResponsiveLayoutInfo(
            windowSizeClass = windowSizeClass,
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight,
            isCompactWidth = isCompactWidth,
            isMediumWidth = isMediumWidth,
            isExpandedWidth = isExpandedWidth,
            isCompactHeight = isCompactHeight,
            isSmallHeight = isSmallHeight,
            isLandscape = isLandscape,
            scaleFactor = scaleFactor
        )
    }
}

/**
 * Preview annotations covering:
 * - Small phone: 320x568dp
 * - Normal phone: 360x800dp
 * - Large phone: 412x915dp
 * - Foldable: 673x841dp
 * - Tablet: 800x1280dp
 * - Font scale 1.0 and 1.3
 * - Light and Dark modes
 */
@Preview(name = "1. Small Phone (320x568)", widthDp = 320, heightDp = 568, showBackground = true)
@Preview(name = "2. Normal Phone (360x800)", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "3. Large Phone (412x915)", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "4. Foldable (673x841)", widthDp = 673, heightDp = 841, showBackground = true)
@Preview(name = "5. Tablet (800x1280)", widthDp = 800, heightDp = 1280, showBackground = true)
annotation class DevicePreviews

@Preview(name = "Font Scale 1.0", fontScale = 1.0f, widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Font Scale 1.3", fontScale = 1.3f, widthDp = 360, heightDp = 800, showBackground = true)
annotation class FontScalePreviews

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 800, showBackground = true)
annotation class ThemePreviews
