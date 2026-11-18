package com.goodbp.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Reading
import com.goodbp.data.Settings
import com.goodbp.report.DoctorReport
import com.goodbp.ui.theme.Bp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Family-facing: chart over time + every confirmed reading, newest first. */
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val all by remember { GoodBpDatabase.get(context).readings().all() }.collectAsState(emptyList())
    val range = remember { Settings(context).range() }
    var days by remember { mutableIntStateOf(30) }
    val since = System.currentTimeMillis() - days * 86_400_000L
    val shown = if (days == 0) all else all.filter { it.takenAt >= since }

    val fmtDay = remember { SimpleDateFormat("yyyy-MM-dd  EEE", Locale.getDefault()) }
    val fmtTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val small = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90, 0).forEach { d ->
                    FilterChip(selected = days == d, onClick = { days = d },
                        label = { Text(if (d == 0) stringResource(R.string.all) else stringResource(R.string.n_days, d)) })
                }
            }
        }
        item {
            if (shown.isEmpty()) Text(stringResource(R.string.no_readings), style = small)
            else {
                BpChart(shown, range)
                ChartLegend(stringResource(R.string.sys), stringResource(R.string.dia), stringResource(R.string.pulse))
                Text(stringResource(R.string.summary, shown.size, shown.map { it.systolic }.average().toInt(),
                    shown.map { it.diastolic }.average().toInt()), style = small)
            }
        }
        item {
            OutlinedButton(onClick = { DoctorReport.share(context, shown) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.share_report))
            }
        }
        var lastDay = ""
        shown.forEach { r ->
            val day = fmtDay.format(Date(r.takenAt))
            if (day != lastDay) {
                lastDay = day
                item(key = "d$day") { Text(day, style = small, modifier = Modifier.padding(top = 12.dp)); HorizontalDivider() }
            }
            item(key = r.id) { ReadingRow(r, fmtTime.format(Date(r.takenAt))) }
        }
    }
}

@Composable
private fun ReadingRow(r: Reading, time: String) {
    val (color, word) = when (r.result) {
        "HIGH" -> Bp.High to stringResource(R.string.result_high)
        "LOW" -> Bp.Low to stringResource(R.string.result_low)
        else -> Bp.Normal to stringResource(R.string.result_normal)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Canvas(Modifier.size(14.dp)) { drawCircle(color) }
        Spacer(Modifier.width(10.dp))
        Text(time, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(90.dp))
        Text("${r.systolic}/${r.diastolic}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(120.dp))
        Text("♥${r.pulse}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(80.dp))
        Text(word, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}
