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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.rules.BpRange
import com.goodbp.rules.BpResult
import com.goodbp.rules.BpRules
import com.goodbp.ui.theme.Bp

/**
 * Three-band bar (low · normal · high) with a marker for the latest reading.
 * Bands come only from her range (the fixed rules); no other categories, no advice.
 * Whichever of SYS/DIA is furthest outside its range sets the marker, so the
 * marker always lands in the band that matches the spoken result.
 */
@Composable
fun BpIndicator(sys: Int, dia: Int, range: BpRange) {
    val pos = markerPosition(sys, dia, range)
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val barTop = 14.dp.toPx(); val barH = 14.dp.toPx(); val w = size.width
            val r = CornerRadius(barH / 2)
            drawRoundRect(Bp.Low, Offset(0f, barTop), Size(w * LOW_END, barH), r)
            drawRoundRect(Bp.High, Offset(w * HIGH_START, barTop), Size(w * (1 - HIGH_START), barH), r)
            drawRect(Bp.Normal, Offset(w * LOW_END - barH / 2, barTop), Size(w * (HIGH_START - LOW_END) + barH, barH))
            // Marker: a dark triangle above the bar + a line through it
            val x = (w * pos).coerceIn(6f, w - 6f)
            val tri = Path().apply {
                moveTo(x - 9.dp.toPx(), 0f); lineTo(x + 9.dp.toPx(), 0f); lineTo(x, barTop - 1.dp.toPx()); close()
            }
            drawPath(tri, Color(0xFF212121))
            drawLine(Color(0xFF212121), Offset(x, barTop - 2.dp.toPx()), Offset(x, barTop + barH + 4.dp.toPx()), 4.dp.toPx())
        }
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.result_low), Modifier.weight(LOW_END), fontSize = 15.sp, color = Bp.Low, textAlign = TextAlign.Center)
            Text(stringResource(R.string.result_normal), Modifier.weight(HIGH_START - LOW_END), fontSize = 15.sp, color = Bp.Normal, textAlign = TextAlign.Center)
            Text(stringResource(R.string.result_high), Modifier.weight(1 - HIGH_START), fontSize = 15.sp, color = Bp.High, textAlign = TextAlign.Center)
        }
    }
}

private const val LOW_END = 0.25f
private const val HIGH_START = 0.75f

/** 0..1 along the bar. Low band 0-0.25, normal 0.25-0.75, high 0.75-1. */
internal fun markerPosition(sys: Int, dia: Int, range: BpRange): Float {
    fun place(v: Int, lo: Int, hi: Int): Float = when {
        v < lo -> LOW_END - LOW_END * ((lo - v) / 30f).coerceIn(0.05f, 1f)
        v > hi -> HIGH_START + (1 - HIGH_START) * ((v - hi) / 40f).coerceIn(0.05f, 1f)
        else -> LOW_END + (HIGH_START - LOW_END) * (v - lo).toFloat() / (hi - lo).coerceAtLeast(1)
    }
    val s = place(sys, range.sysLow, range.sysHigh)
    val d = place(dia, range.diaLow, range.diaHigh)
    return when (BpRules.classify(sys, dia, range)) {
        BpResult.LOW -> minOf(s, d)
        BpResult.HIGH -> maxOf(s, d)
        BpResult.NORMAL -> if (kotlin.math.abs(s - 0.5f) >= kotlin.math.abs(d - 0.5f)) s else d
    }
}
