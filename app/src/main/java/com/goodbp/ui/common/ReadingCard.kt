package com.goodbp.ui.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.Reading
import com.goodbp.ui.theme.Bp
import java.util.Calendar

/** Colour + mark for a result: ✓ normal, ↑ high, ↓ low (never colour alone). */
fun resultStyle(result: String): Pair<Color, String> = when (result) {
    "HIGH" -> Bp.High to "↑"
    "LOW" -> Bp.Low to "↓"
    else -> Bp.Normal to "✓"
}

fun resultWord(context: Context, result: String): String = context.getString(when (result) {
    "HIGH" -> R.string.result_high
    "LOW" -> R.string.result_low
    else -> R.string.result_normal
})

/** Compact card: tap to hear it. Used for "last 3 readings" and history lists. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReadingCard(r: Reading, onTap: () -> Unit, big: TextUnit = 36.sp, onLongPress: (() -> Unit)? = null) {
    val (color, mark) = resultStyle(r.result)
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(mark, fontSize = 40.sp, color = color, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(whenLabel(context, r.takenAt), fontSize = 18.sp, color = Color.DarkGray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${r.systolic}/${r.diastolic}", fontSize = big, color = Color.Black)
                Spacer(Modifier.width(12.dp))
                Text("♥ ${r.pulse}", fontSize = 24.sp, color = Color.DarkGray)
            }
        }
        Text("🔊", fontSize = 28.sp)
    }
}

/** "今天早上 8:05" / "昨天晚上 20:10" / "11月16日 中午 12:30" */
fun whenLabel(context: Context, t: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val days = ((startOfDay(Calendar.getInstance()) - startOfDay(c)) / 86_400_000L).toInt()
    val day = when (days) {
        0 -> context.getString(R.string.today)
        1 -> context.getString(R.string.yesterday)
        else -> context.getString(R.string.date_md, c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
    val h = c.get(Calendar.HOUR_OF_DAY)
    val part = context.getString(partOfDay(h))
    return context.getString(R.string.day_part, day, part) + " %d:%02d".format(h, c.get(Calendar.MINUTE))
}

fun partOfDay(hour: Int): Int = when {
    hour < 11 -> R.string.morning
    hour < 14 -> R.string.noon
    hour < 18 -> R.string.afternoon
    else -> R.string.evening
}

fun startOfDay(c: Calendar): Long = (c.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** What gets spoken when a reading is tapped. */
fun spoken(context: Context, r: Reading): String = spokenSummary(context, r.takenAt, r.systolic, r.diastolic, r.pulse)

/** "When. Top x, bottom y, pulse z. Your blood pressure is high." (reference chart word, no advice) */
fun spokenSummary(context: Context, takenAt: Long, sys: Int, dia: Int, pulse: Int): String {
    val word = context.getString(when (com.goodbp.rules.BpCategory.of(sys, dia)) {
        com.goodbp.rules.BpCategory.LOW -> R.string.say_cat_low
        com.goodbp.rules.BpCategory.NORMAL -> R.string.say_cat_normal
        com.goodbp.rules.BpCategory.PRE -> R.string.say_cat_pre
        com.goodbp.rules.BpCategory.STAGE1, com.goodbp.rules.BpCategory.STAGE2 -> R.string.say_cat_high
        com.goodbp.rules.BpCategory.CRISIS -> R.string.say_cat_crisis
    })
    return context.getString(R.string.say_summary, whenLabel(context, takenAt), sys, dia, pulse, word)
}
