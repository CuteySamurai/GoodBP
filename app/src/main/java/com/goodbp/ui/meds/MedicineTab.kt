package com.goodbp.ui.meds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.MedConfirmation
import com.goodbp.data.Medicine
import com.goodbp.data.Settings
import com.goodbp.meds.MedSchedule
import com.goodbp.speech.Speaker
import com.goodbp.ui.common.startOfDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private data class Dose(val med: Medicine, val minutes: Int, val dueAt: Long)

/** A dose can be confirmed from an hour before its time. */
private const val EARLY_MS = 60 * 60_000L
/** After two hours without a press it shows as not confirmed (she can still press). */
private const val LATE_MS = 2 * 60 * 60_000L

/**
 * Medicine tab. Top half is for her: today's doses with the pill photo and one big
 * button. Bottom half is for the family: add or change medicines and times.
 * No medicine advice anywhere; we only record that she pressed the button.
 */
@Composable
fun MedicineTab() {
    val context = LocalContext.current
    val dao = remember { GoodBpDatabase.get(context).medicines() }
    val settings = remember { Settings(context) }
    val speaker = remember { Speaker(context, settings.language) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    val scope = rememberCoroutineScope()

    val meds by remember { dao.all() }.collectAsState(emptyList())
    val weekStart = remember { startOfDay(Calendar.getInstance()) - 6 * 86_400_000L }
    val confs by remember { dao.confirmationsSince(weekStart) }.collectAsState(emptyList())
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    var editing by remember { mutableStateOf<Medicine?>(null) }

    val sConfirmed = stringResource(R.string.say_med_confirmed)
    val today = meds.flatMap { m -> MedSchedule.parse(m.timesOfDay).map { Dose(m, it, MedSchedule.todayAt(it)) } }
        .sortedWith(compareBy({ it.minutes }, { it.med.id }))

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.meds_today), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))
        if (today.isEmpty()) Text(stringResource(R.string.med_none), fontSize = 20.sp)
        today.forEach { d ->
            val conf = confs.firstOrNull { it.medicineId == d.med.id && it.dueAt == d.dueAt }
            DoseCard(d, conf, now,
                onSpeak = { speaker.say(listOf(d.med.name, d.med.spokenDescription).filter { it.isNotBlank() }.joinToString(". ")) },
                onConfirm = { scope.launch { MedSchedule.confirm(context, d.med.id, d.dueAt); speaker.say(sConfirmed) } })
        }

        if (meds.isNotEmpty()) WeekSummary(meds, confs, now)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.med_list), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        meds.forEach { m ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedPhoto(m.pillPhotoPath, 56.dp)
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(MedSchedule.parse(m.timesOfDay).joinToString("  ") { MedSchedule.hhmm(it) }, fontSize = 17.sp)
                    }
                    OutlinedButton({ editing = m }) { Text(stringResource(R.string.med_edit)) }
                }
            }
        }
        Button({ editing = Medicine(name = "", spokenDescription = "", pillPhotoPath = null, timesOfDay = "08:00") },
            Modifier.fillMaxWidth().height(56.dp)) { Text("＋ " + stringResource(R.string.med_add), fontSize = 20.sp) }
    }

    editing?.let { m -> MedEditor(m, onClose = { editing = null }) }
}

@Composable
private fun DoseCard(d: Dose, conf: MedConfirmation?, now: Long, onSpeak: () -> Unit, onConfirm: () -> Unit) {
    val due = now >= d.dueAt - EARLY_MS
    val late = conf == null && now > d.dueAt + LATE_MS
    val bg = when {
        conf != null -> Color(0xFFE8F5E9)
        late -> Color(0xFFFFEBEE)
        due -> Color(0xFFFFF3E0)
        else -> Color(0xFFF5F5F5)
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onSpeak), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MedPhoto(d.med.pillPhotoPath, 84.dp)
                Column(Modifier.weight(1f)) {
                    Text(MedSchedule.hhmm(d.minutes), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(d.med.name, fontSize = 22.sp)
                    if (d.med.spokenDescription.isNotBlank()) Text(d.med.spokenDescription, fontSize = 16.sp, color = Color(0xFF616161))
                }
                Text("🔊", fontSize = 26.sp)
            }
            when {
                conf != null -> Text("✓ " + stringResource(R.string.med_confirmed_at, MedSchedule.hhmm(minuteOf(conf.confirmedAt ?: conf.dueAt))),
                    fontSize = 22.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                due -> {
                    if (late) Text(stringResource(R.string.med_not_confirmed), fontSize = 18.sp, color = Color(0xFFC62828))
                    Button(onConfirm, Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))) {
                        Text("💊 " + stringResource(R.string.took_medicine), fontSize = 28.sp)
                    }
                }
                else -> Text(stringResource(R.string.med_upcoming), fontSize = 18.sp, color = Color(0xFF757575))
            }
        }
    }
}

/** Last 7 days: how many doses were confirmed out of how many were due. */
@Composable
private fun WeekSummary(meds: List<Medicine>, confs: List<MedConfirmation>, now: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.med_week), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val todayStart = startOfDay(Calendar.getInstance())
            for (back in 6 downTo 0) {
                val dayStart = todayStart - back * 86_400_000L
                val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
                // Only doses whose time has come count (today's later ones don't).
                val due = meds.sumOf { m -> MedSchedule.parse(m.timesOfDay).count { dayStart + it * 60_000L <= now } }
                val done = confs.count { c -> c.dueAt in dayStart until dayStart + 86_400_000L && meds.any { it.id == c.medicineId } }
                if (due == 0 && done == 0) continue
                val full = done >= due
                Row(Modifier.fillMaxWidth()) {
                    Text("%d/%d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1), fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text((if (full) "✓ " else "• ") + stringResource(R.string.med_week_line, done, due), fontSize = 18.sp,
                        color = if (full) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            }
        }
    }
}

private fun minuteOf(t: Long): Int = Calendar.getInstance().apply { timeInMillis = t }.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
