package com.goodbp.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.data.Reading
import com.goodbp.rules.BpRange
import com.goodbp.ui.theme.Bp

private val SysColor = Color(0xFFB3261E)
private val DiaColor = Color(0xFF0B57D0)
private val PulseColor = Color(0xFF8A8A8A)

/**
 * Systolic and diastolic over time, with the normal range shaded.
 * Drawn by hand (no chart library) so it stays small and predictable.
 */
@Composable
fun BpChart(readings: List<Reading>, range: BpRange, modifier: Modifier = Modifier) {
    val pts = readings.sortedBy { it.takenAt }
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontSize = 11.sp, color = Color.Gray)

    Canvas(modifier.fillMaxWidth().height(240.dp)) {
        val left = 36.dp.toPx(); val right = 8.dp.toPx(); val top = 8.dp.toPx(); val bottom = 8.dp.toPx()
        val w = size.width - left - right; val h = size.height - top - bottom
        val yMin = minOf(40, pts.minOfOrNull { minOf(it.diastolic, it.pulse) } ?: 40) - 5
        val yMax = maxOf(180, pts.maxOfOrNull { it.systolic } ?: 180) + 5
        fun y(v: Int) = top + h * (1f - (v - yMin).toFloat() / (yMax - yMin))
        val t0 = pts.firstOrNull()?.takenAt ?: 0L
        val t1 = pts.lastOrNull()?.takenAt ?: 1L
        fun x(t: Long) = if (t1 == t0) left + w / 2 else left + w * (t - t0).toFloat() / (t1 - t0)

        // Normal bands (systolic and diastolic ranges)
        drawRect(Bp.Normal.copy(alpha = 0.10f), Offset(left, y(range.sysHigh)), Size(w, y(range.sysLow) - y(range.sysHigh)))
        drawRect(Bp.Normal.copy(alpha = 0.10f), Offset(left, y(range.diaHigh)), Size(w, y(range.diaLow) - y(range.diaHigh)))

        // Grid + y labels every 20
        var g = ((yMin + 19) / 20) * 20
        while (g <= yMax) {
            drawLine(Color.LightGray.copy(alpha = 0.6f), Offset(left, y(g)), Offset(left + w, y(g)), 1f)
            drawText(measurer, "$g", Offset(0f, y(g) - 7.sp.toPx()), label)
            g += 20
        }

        fun series(color: Color, v: (Reading) -> Int, dot: Float) {
            if (pts.isEmpty()) return
            val path = Path()
            pts.forEachIndexed { i, r ->
                val o = Offset(x(r.takenAt), y(v(r)))
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
            pts.forEach { drawCircle(color, dot, Offset(x(it.takenAt), y(v(it)))) }
        }
        series(PulseColor.copy(alpha = 0.5f), { it.pulse }, 2.dp.toPx())
        series(DiaColor, { it.diastolic }, 4.dp.toPx())
        series(SysColor, { it.systolic }, 4.dp.toPx())
    }
}

@Composable
fun ChartLegend(sysLabel: String, diaLabel: String, pulseLabel: String) {
    Row {
        listOf(SysColor to sysLabel, DiaColor to diaLabel, PulseColor to pulseLabel).forEach { (c, t) ->
            Canvas(Modifier.width(14.dp).height(14.dp)) { drawCircle(c) }
            Spacer(Modifier.width(4.dp))
            Text(t, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp))
            Spacer(Modifier.width(16.dp))
        }
    }
}
