package com.goodbp.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.rules.BpCategory

/** Colours follow the family's reference chart; Low added in blue. */
fun categoryColor(c: BpCategory): Color = when (c) {
    BpCategory.LOW -> Color(0xFF42A5F5)
    BpCategory.NORMAL -> Color(0xFF66BB6A)
    BpCategory.PRE -> Color(0xFFC0CA33)
    BpCategory.STAGE1 -> Color(0xFFFFA726)
    BpCategory.STAGE2 -> Color(0xFFEF5350)
    BpCategory.CRISIS -> Color(0xFFC62828)
}

fun categoryLabel(c: BpCategory): Int = when (c) {
    BpCategory.LOW -> R.string.cat_low
    BpCategory.NORMAL -> R.string.cat_normal
    BpCategory.PRE -> R.string.cat_pre
    BpCategory.STAGE1 -> R.string.cat_stage1
    BpCategory.STAGE2 -> R.string.cat_stage2
    BpCategory.CRISIS -> R.string.cat_crisis
}

/**
 * Six-band bar (Low · Normal · Pre · Stage 1 · Stage 2 · Crisis) with a marker
 * on the latest reading. Reference chart only; no advice.
 */
@Composable
fun BpIndicator(sys: Int, dia: Int) {
    val cat = BpCategory.of(sys, dia)
    val cats = BpCategory.entries
    val pos = (cat.ordinal + BpCategory.fraction(sys, dia)) / cats.size
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(categoryLabel(cat)), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = categoryColor(cat))
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val barTop = 14.dp.toPx(); val barH = 16.dp.toPx(); val w = size.width
            val seg = w / cats.size; val gap = 2.dp.toPx()
            cats.forEachIndexed { i, c ->
                drawRoundRect(categoryColor(c), Offset(i * seg + gap / 2, barTop), Size(seg - gap, barH), CornerRadius(4.dp.toPx()))
            }
            val x = (w * pos).coerceIn(8f, w - 8f)
            val tri = Path().apply {
                moveTo(x - 9.dp.toPx(), 0f); lineTo(x + 9.dp.toPx(), 0f); lineTo(x, barTop - 1.dp.toPx()); close()
            }
            drawPath(tri, Color(0xFF212121))
            drawLine(Color(0xFF212121), Offset(x, barTop - 2.dp.toPx()), Offset(x, barTop + barH + 4.dp.toPx()), 4.dp.toPx())
        }
        Row(Modifier.fillMaxWidth()) {
            cats.forEach { c ->
                Text(stringResource(categoryLabel(c)), Modifier.weight(1f), fontSize = 11.sp, lineHeight = 13.sp,
                    color = if (c == cat) Color.Black else Color.Gray, textAlign = TextAlign.Center,
                    fontWeight = if (c == cat) FontWeight.Bold else FontWeight.Normal)
            }
        }
        Text(stringResource(R.string.reference_note), fontSize = 12.sp, color = Color.Gray)
    }
}
