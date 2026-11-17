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
    val Normal = Color(0xFF1B7F3B)
    val High = Color(0xFFB3261E)
    val Low = Color(0xFF8A4B00)
    val Primary = Color(0xFF0B57D0)
}

private val BigType = Typography(
    displayLarge = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 72.sp),
    headlineLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    bodyLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
)

@Composable
fun GoodBpTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(primary = Bp.Primary, background = Color.White, surface = Color.White),
    typography = BigType,
    content = content,
)
