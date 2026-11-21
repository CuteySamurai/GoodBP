package com.goodbp.ui.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.rules.BpRange

val SysColor = Color(0xFF2E7D32)
val DiaColor = Color(0xFF1565C0)
val PulseColor = Color(0xFFC62828)

val MorningColor = Color(0xFFC9A800) // palette yellow, deepened to show on white
val AfternoonColor = Color(0xFF8E9C2A) // palette lime, deepened
val EveningColor = com.goodbp.ui.theme.Palette.BlueDeep

/** 0 = morning (<12), 1 = afternoon (12-17), 2 = evening (18+). */
fun partOf(hour: Int) = when { hour < 12 -> 0; hour < 18 -> 1; else -> 2 }
fun partColor(part: Int) = when (part) { 0 -> MorningColor; 1 -> AfternoonColor; else -> EveningColor }

/** One point on the chart: a single reading (with its time of day), or an average for a day/week (part = null). */
data class ChartPoint(val t: Long, val sys: Int, val dia: Int, val pulse: Int, val label: String, val part: Int? = null)

/**
 * Line chart (hand-drawn, no library). Points are evenly spaced so gaps between
 * days don't squash the line; x labels come from each point's label.
 * Dashed lines mark the high limits from her range.
 */
@Composable
fun BpChart(
    points: List<ChartPoint>,
    range: BpRange,
    showSys: Boolean = true,
    showDia: Boolean = true,
    showPulse: Boolean = true,
    height: Dp = 280.dp,
) {
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontSize = 12.sp, color = Color.Gray)
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val left = 34.dp.toPx(); val right = 10.dp.toPx(); val top = 8.dp.toPx(); val bottom = 22.dp.toPx()
        val w = size.width - left - right; val h = size.height - top - bottom
        val values = points.flatMap { listOfNotNull(it.sys.takeIf { showSys }, it.dia.takeIf { showDia }, it.pulse.takeIf { showPulse }) }
        val yMin = (minOf(values.minOrNull() ?: 50, range.diaLow) - 10) / 10 * 10
        val yMax = (maxOf(values.maxOrNull() ?: 160, range.sysHigh) + 19) / 10 * 10
        fun y(v: Int) = top + h * (1f - (v - yMin).toFloat() / (yMax - yMin))
        val n = points.size
        fun x(i: Int) = if (n <= 1) left + w / 2 else left + w * i / (n - 1)

        // Horizontal grid + labels
        val step = if (yMax - yMin > 120) 20 else 10
        var g = yMin
        while (g <= yMax) {
            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(left, y(g)), Offset(left + w, y(g)), 1f)
            drawText(measurer, "$g", Offset(0f, y(g) - 8.sp.toPx()), label)
            g += step
        }
        // Limit lines from her range
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        if (showSys) drawLine(SysColor.copy(alpha = 0.6f), Offset(left, y(range.sysHigh)), Offset(left + w, y(range.sysHigh)), 3f, pathEffect = dash)
        if (showDia) drawLine(DiaColor.copy(alpha = 0.6f), Offset(left, y(range.diaHigh)), Offset(left + w, y(range.diaHigh)), 3f, pathEffect = dash)

        // X labels: up to ~6, evenly picked
        if (n > 0) {
            val every = maxOf(1, (n + 5) / 6)
            for (i in 0 until n step every) {
                val lay = measurer.measure(points[i].label, label)
                val lx = (x(i) - lay.size.width / 2).coerceIn(left - 4, size.width - lay.size.width.toFloat())
                drawText(lay, topLeft = Offset(lx, top + h + 4.dp.toPx()))
            }
        }

        fun series(color: Color, v: (ChartPoint) -> Int) {
            if (n == 0) return
            val path = Path()
            points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(i), y(v(p))) else path.lineTo(x(i), y(v(p))) }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
            val r = if (n > 40) 3.dp.toPx() else 6.dp.toPx()
            // Dot = time-of-day colour (morning/afternoon/evening), ringed in the series colour.
            points.forEachIndexed { i, p ->
                val o = Offset(x(i), y(v(p)))
                drawCircle(color, r, o)
                p.part?.let { drawCircle(partColor(it), r * 0.6f, o) }
            }
        }
        if (showPulse) series(PulseColor) { it.pulse }
        if (showDia) series(DiaColor) { it.dia }
        if (showSys) series(SysColor) { it.sys }
    }
}
