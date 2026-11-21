package com.goodbp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** High contrast, very large text. Colour is never the only signal: every state is also spoken. */
object Bp {
    // Result colours keep their meaning (green / red / brown), not the theme.
    val Normal = Color(0xFF1B7F3B)
    val High = Color(0xFFB3261E)
    val Low = Color(0xFF8A4B00)
    val Primary = Palette.TealDeep
}

/**
 * App theme from the family's palette: #5AA7A7 #96D7C6 #BAC94A #E2D36B #6C8CBF.
 * Buttons with white text use deeper shades of teal and blue so text stays readable
 * (contrast 5.5:1). The light colours are for backgrounds and buttons with dark text.
 * Medical colours (BP category bar, result words, chart lines) stay as they were.
 */
object Palette {
    val Teal = Color(0xFF5AA7A7)
    val Mint = Color(0xFF96D7C6)
    val Lime = Color(0xFFBAC94A)
    val Yellow = Color(0xFFE2D36B)
    val Blue = Color(0xFF6C8CBF)
    val TealDeep = Color(0xFF2A7373)
    val BlueDeep = Color(0xFF4A6A9E)
    val MintTint = Color(0xFFEAF6F2)
    val YellowTint = Color(0xFFFBF6DA)
    val Ink = Color(0xFF1F2A2A)
}

private val BigType = Typography(
    displayLarge = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 72.sp),
    headlineLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    bodyLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
)

@Composable
fun GoodBpTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Palette.TealDeep, onPrimary = Color.White,
        primaryContainer = Palette.Mint, onPrimaryContainer = Palette.Ink,
        secondary = Palette.BlueDeep, onSecondary = Color.White,
        secondaryContainer = Palette.Blue.copy(alpha = 0.25f), onSecondaryContainer = Palette.Ink,
        tertiary = Palette.Lime, onTertiary = Palette.Ink,
        background = Color.White, surface = Color.White,
        surfaceVariant = Palette.MintTint, onSurfaceVariant = Palette.Ink,
        surfaceContainerHighest = Palette.MintTint, surfaceContainerHigh = Palette.MintTint,
        surfaceContainer = Palette.MintTint, surfaceContainerLow = Palette.MintTint,
        outline = Palette.Teal,
    ),
    typography = BigType,
    content = content,
)
