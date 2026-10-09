package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val WalkCrewShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),      // Cards: 28dp corner radius
    extraLarge = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp) // Bottom sheets: 32dp
)

// Specific Named Shapes
val CardCornerRadius = 28.dp
val CardShape = RoundedCornerShape(CardCornerRadius)
val ButtonShape = RoundedCornerShape(999.dp) // Fully rounded pill
val ChipShape = RoundedCornerShape(999.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val FloatingNavPillShape = RoundedCornerShape(999.dp)
val ActiveTabPillShape = RoundedCornerShape(999.dp)

// Standard spacing scale
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val screenHorizontal = 20.dp
    val lg = 24.dp
    val xl = 32.dp
}
