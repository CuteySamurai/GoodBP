package com.goodbp.ui.trend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Reading
import com.goodbp.data.Settings
import com.goodbp.speech.Speaker
import com.goodbp.ui.common.ReadingCard
import com.goodbp.ui.common.spoken
import kotlinx.coroutines.launch
import java.util.Calendar

/** Period choices: label, length in days, how points are grouped. */
private enum class Period(val days: Int, val group: Group) {
    D7(7, Group.EACH), W4(28, Group.EACH), M3(91, Group.DAY), Y1(365, Group.WEEK)
}
private enum class Group { EACH, DAY, WEEK }

@Composable
fun TrendTab() {
    val context = LocalContext.current
    val db = remember { GoodBpDatabase.get(context) }
    val all by remember { db.readings().all() }.collectAsState(emptyList())
    val settings = remember { Settings(context) }
    val range = remember { settings.range() }
    val speaker = remember { Speaker(context, settings.language) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    val scope = rememberCoroutineScope()

    var period by remember { mutableStateOf(Period.D7) }
    var showSys by remember { mutableStateOf(true) }
    var showDia by remember { mutableStateOf(true) }
    var showPulse by remember { mutableStateOf(true) }
    var toDelete by remember { mutableStateOf<Reading?>(null) }

    val now = System.currentTimeMillis()
    val cut = now - period.days * 86_400_000L
    val shown = all.filter { it.takenAt >= cut }
    val previous = all.filter { it.takenAt in (cut - period.days * 86_400_000L) until cut }
    val points = remember(shown, period) { toPoints(shown.sortedBy { it.takenAt }, period.group) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(stringResource(R.string.tab_trend), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Period.D7 to R.string.p_7d, Period.W4 to R.string.p_4w, Period.M3 to R.string.p_3m, Period.Y1 to R.string.p_1y).forEach { (p, l) ->
                    FilterChip(period == p, { period = p }, label = { Text(stringResource(l), fontSize = 18.sp) })
                }
            }
        }
        item {
            if (points.isEmpty()) Text(stringResource(R.string.no_readings), fontSize = 20.sp, modifier = Modifier.padding(24.dp))
            else BpChart(points, range, showSys, showDia, showPulse)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Toggle(stringResource(R.string.sys), SysColor, showSys, Modifier.weight(1f)) { showSys = !showSys }
                Toggle(stringResource(R.string.dia), DiaColor, showDia, Modifier.weight(1f)) { showDia = !showDia }
                Toggle(stringResource(R.string.pulse), PulseColor, showPulse, Modifier.weight(1f)) { showPulse = !showPulse }
            }
        }
        if (shown.isNotEmpty()) {
            item { TrendLine(shown, previous) }
            item { SummaryTable(shown) }
        }
        item { Text(stringResource(R.string.all_readings), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { Text(stringResource(R.string.hold_to_delete), fontSize = 14.sp, color = Color.Gray) }
        items(shown, key = { it.id }) { r ->
            ReadingCard(r, onTap = { speaker.say(spoken(context, r)) }, big = 30.sp, onLongPress = { toDelete = r })
        }
        item { Box(Modifier.height(24.dp)) }
    }

    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_reading_q)) },
            text = { Text("${r.systolic}/${r.diastolic}  ♥${r.pulse}", fontSize = 24.sp) },
            confirmButton = { TextButton({ scope.launch { db.readings().delete(r.id) }; toDelete = null }) { Text(stringResource(R.string.remove)) } },
            dismissButton = { TextButton({ toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Toggle(text: String, color: Color, on: Boolean, m: Modifier, onClick: () -> Unit) = Button(
    onClick, m.height(56.dp), shape = RoundedCornerShape(10.dp),
    colors = ButtonDefaults.buttonColors(containerColor = if (on) color else Color(0xFFE0E0E0), contentColor = if (on) Color.White else Color.DarkGray),
) { Text(text, fontSize = 17.sp, maxLines = 1) }

/** "平均 128/80 ♥70 · 比上一段 高压 ↑3 低压 ↓1" */
@Composable
private fun TrendLine(now: List<Reading>, prev: List<Reading>) {
    val sys = now.map { it.systolic }.average().toInt()
    val dia = now.map { it.diastolic }.average().toInt()
    val pulse = now.map { it.pulse }.average().toInt()
    fun arrow(d: Int) = when { d > 0 -> "↑$d"; d < 0 -> "↓${-d}"; else -> "→0" }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.avg_line, now.size, sys, dia, pulse), fontSize = 20.sp)
            if (prev.isNotEmpty()) {
                val ds = sys - prev.map { it.systolic }.average().toInt()
                val dd = dia - prev.map { it.diastolic }.average().toInt()
                Text(stringResource(R.string.vs_previous, arrow(ds), arrow(dd)), fontSize = 18.sp, color = Color.DarkGray)
            }
        }
    }
}

/** Max / Min / Avg, overall and by time of day (like a paper BP diary). */
@Composable
private fun SummaryTable(rs: List<Reading>) {
    fun hour(r: Reading) = Calendar.getInstance().apply { timeInMillis = r.takenAt }.get(Calendar.HOUR_OF_DAY)
    val groups = listOf(
        R.string.overall to rs,
        R.string.morning to rs.filter { hour(it) < 12 },
        R.string.afternoon to rs.filter { hour(it) in 12..17 },
        R.string.evening to rs.filter { hour(it) >= 18 },
    ).filter { it.second.isNotEmpty() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            groups.forEach { (title, g) ->
                Text("${stringResource(title)} (${g.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Row { Cell(""); Cell(stringResource(R.string.max)); Cell(stringResource(R.string.min)); Cell(stringResource(R.string.avg)) }
                listOf(R.string.sys to g.map { it.systolic }, R.string.dia to g.map { it.diastolic }, R.string.pulse to g.map { it.pulse })
                    .forEach { (l, v) ->
                        Row { Cell(stringResource(l)); Cell("${v.max()}", true); Cell("${v.min()}", true); Cell("%.0f".format(v.average()), true) }
                    }
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(t: String, bold: Boolean = false) =
    Text(t, Modifier.weight(1f), fontSize = 17.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

private fun toPoints(rs: List<Reading>, group: Group): List<ChartPoint> {
    fun label(t: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = t }
        return "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
    }
    fun avg(t: Long, g: List<Reading>) = ChartPoint(
        t, g.map { it.systolic }.average().toInt(), g.map { it.diastolic }.average().toInt(),
        g.map { it.pulse }.average().toInt(), label(t))
    return when (group) {
        Group.EACH -> rs.map { ChartPoint(it.takenAt, it.systolic, it.diastolic, it.pulse, label(it.takenAt)) }
        Group.DAY -> rs.groupBy { localDay(it.takenAt) }.values.map { avg(it.first().takenAt, it) }
        Group.WEEK -> rs.groupBy { localDay(it.takenAt) / 7 }.values.map { avg(it.first().takenAt, it) }
    }
}

/** Day number in the phone's time zone (so 1am Singapore isn't counted as yesterday). */
private fun localDay(t: Long): Long = (t + java.util.TimeZone.getDefault().getOffset(t)) / 86_400_000L
