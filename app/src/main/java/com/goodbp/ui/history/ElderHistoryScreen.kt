package com.goodbp.ui.history

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.Reading
import com.goodbp.data.Settings
import com.goodbp.speech.Speaker
import com.goodbp.ui.theme.Bp
import java.util.Calendar

/**
 * History for her: no reading needed. Big cards coloured and marked by result
 * (✓ normal, ↑ high, ↓ low); tap a card and it is spoken. One big button reads
 * out the latest readings. The detailed chart/report stays in family settings.
 */
@Composable
fun ElderHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val speaker = remember { Speaker(context, settings.language) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    val readings by remember { GoodBpDatabase.get(context).readings().all() }.collectAsState(emptyList())
    val recent = readings.take(14)
    val noHistory = stringResource(R.string.say_no_history)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onBack, Modifier.weight(1f).height(96.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F6368))) {
                Text(stringResource(R.string.back), style = MaterialTheme.typography.headlineLarge)
            }
            Button({
                if (recent.isEmpty()) speaker.say(noHistory)
                else speaker.say(recent.take(3).joinToString(" ") { spoken(context, it) })
            }, Modifier.weight(1.4f).height(96.dp)) {
                Text(stringResource(R.string.listen), style = MaterialTheme.typography.headlineLarge)
            }
        }
        if (recent.size >= 2) BpChart(recent.take(7), settings.range())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recent, key = { it.id }) { r -> ReadingCard(r) { speaker.say(spoken(context, r)) } }
        }
    }
}

@Composable
private fun ReadingCard(r: Reading, onTap: () -> Unit) {
    val (color, mark) = when (r.result) {
        "HIGH" -> Bp.High to "↑"
        "LOW" -> Bp.Low to "↓"
        else -> Bp.Normal to "✓"
    }
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(mark, fontSize = 56.sp, color = color, modifier = Modifier.width(64.dp))
        Column(Modifier.weight(1f)) {
            Text(whenLabel(context, r.takenAt), fontSize = 22.sp, color = Color.DarkGray)
            Text("${r.systolic} / ${r.diastolic}", fontSize = 44.sp, color = Color.Black)
        }
        Spacer(Modifier.width(8.dp))
        Text("🔊", fontSize = 36.sp)
    }
}

/** "今天早上" / "昨天晚上" / "11月16日 中午" */
private fun whenLabel(context: Context, t: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val today = Calendar.getInstance()
    val days = ((startOfDay(today) - startOfDay(c)) / 86_400_000L).toInt()
    val day = when (days) {
        0 -> context.getString(R.string.today)
        1 -> context.getString(R.string.yesterday)
        else -> context.getString(R.string.date_md, c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
    val h = c.get(Calendar.HOUR_OF_DAY)
    val part = context.getString(when {
        h < 11 -> R.string.morning
        h < 14 -> R.string.noon
        h < 18 -> R.string.afternoon
        else -> R.string.evening
    })
    return context.getString(R.string.day_part, day, part)
}

private fun startOfDay(c: Calendar): Long = (c.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun spoken(context: Context, r: Reading): String {
    val word = context.getString(when (r.result) {
        "HIGH" -> R.string.result_high
        "LOW" -> R.string.result_low
        else -> R.string.result_normal
    })
    return context.getString(R.string.say_history_item, whenLabel(context, r.takenAt), r.systolic, r.diastolic, word)
}
