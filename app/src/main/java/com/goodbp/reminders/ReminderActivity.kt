package com.goodbp.reminders

import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.goodbp.R
import com.goodbp.data.Medicine
import com.goodbp.data.Settings
import com.goodbp.meds.MedSchedule
import com.goodbp.speech.Speaker
import com.goodbp.ui.home.BigButtonScreen
import com.goodbp.ui.meds.MedPhoto
import com.goodbp.ui.theme.GoodBpTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Over the lock screen. Repeats the spoken reminder (on the alarm volume) until she taps the big button.
 * BP: opens the reading screen. MED: shows every medicine due at this time on one screen,
 * says their names, and records that she confirmed (never that she "took" it).
 * A reminder that arrives while one is showing waits its turn and shows next.
 * TODO (family alerts step): if no response after N repeats, alert family.
 */
class ReminderActivity : ComponentActivity() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.goodbp.AppLocale.wrap(base))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val same = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) == this.intent.getStringExtra(ReminderScheduler.EXTRA_KIND) &&
            intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, -1) == this.intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, -1)
        // Same kind and time: already on this screen (medicines due together are shown together).
        if (!same) queued.addLast(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loudEnough()
        val isBp = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) != ReminderScheduler.Kind.MED.name
        val minutes = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, 0)
        val dueAt = MedSchedule.doseAt(minutes)
        val notificationId = intent.getIntExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, ReminderReceiver.NOTIFICATION_ID)
        val speaker = Speaker(this, Settings(this).language, alarm = true)
        fun dismiss(meds: List<Medicine> = emptyList()) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.cancel(notificationId)
            meds.forEach { nm.cancel(ReminderScheduler.requestCode(ReminderScheduler.Kind.MED, minutes, it.id)) }
            val next = queued.removeFirstOrNull()
            if (next != null) { setIntent(next); recreate() } else finish()
        }
        setContent {
          com.goodbp.AppLanguage {
            GoodBpTheme {
                DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
                if (isBp) {
                    val prompt = stringResource(R.string.say_reminder_bp)
                    LaunchedEffect(Unit) { repeat(REPEATS) { speaker.sayPrompt("say_reminder_bp", prompt); delay(REPEAT_GAP_MS) } }
                    BigButtonScreen(label = stringResource(R.string.home_button), onPress = {
                        startActivity(
                            Intent(this, com.goodbp.MainActivity::class.java)
                                .putExtra(com.goodbp.MainActivity.EXTRA_START_READING, true)
                        )
                        dismiss()
                    })
                } else {
                    var meds by remember { mutableStateOf<List<Medicine>?>(null) }
                    LaunchedEffect(Unit) { meds = MedSchedule.dueAt(this@ReminderActivity, minutes) }
                    val list = meds.orEmpty()
                    val generic = stringResource(R.string.say_reminder_med)
                    val prompt = (listOf(generic.trimEnd('.', '。', ' ')) + list.flatMap { listOf(it.name, it.spokenDescription) })
                        .filter { it.isNotBlank() }.joinToString(". ")
                    val thanks = stringResource(R.string.say_med_confirmed)
                    LaunchedEffect(prompt) { if (meds != null) repeat(REPEATS) { speaker.say(prompt); delay(REPEAT_GAP_MS) } }
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(MedSchedule.hhmm(minutes), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                            if (list.size <= 1) {
                                val m = list.firstOrNull()
                                MedPhoto(m?.pillPhotoPath, 200.dp)
                                if (m != null) MedText(m, 32)
                            } else list.forEach { m ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    MedPhoto(m.pillPhotoPath, 110.dp)
                                    Column(Modifier.weight(1f)) { MedText(m, 26) }
                                }
                            }
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        list.forEach { MedSchedule.confirm(this@ReminderActivity, it.id, dueAt) }
                                        speaker.say(thanks)
                                        delay(1500)
                                        dismiss(list)
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = com.goodbp.ui.theme.Palette.Yellow, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth(if (list.size <= 1) 0.8f else 0.6f).aspectRatio(1f),
                            ) { Text("💊\n" + stringResource(R.string.took_medicine), fontSize = 40.sp, textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
          }
        }
    }

    /** Raise the alarm volume to at least 70% while the reminder shows; put it back afterwards. */
    private fun loudEnough() {
        runCatching {
            val am = getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val now = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val want = (max * 0.7f).toInt().coerceAtLeast(1)
            if (now < want) {
                if (restoreAlarmVolume == null) restoreAlarmVolume = now
                am.setStreamVolume(AudioManager.STREAM_ALARM, want, 0)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing && queued.isEmpty()) restoreAlarmVolume?.let { v ->
            runCatching { getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_ALARM, v, 0) }
            restoreAlarmVolume = null
        }
        super.onDestroy()
    }

    companion object {
        /** Reminders that fired while one was on screen (e.g. BP and medicine both at 08:00). Kept across recreate(). */
        private val queued = ArrayDeque<Intent>()
        private var restoreAlarmVolume: Int? = null
        const val REPEATS = 10
        const val REPEAT_GAP_MS = 30_000L
    }
}

@androidx.compose.runtime.Composable
private fun MedText(m: Medicine, size: Int) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(m.name, fontSize = size.sp, textAlign = TextAlign.Center)
    if (m.spokenDescription.isNotBlank())
        Text(m.spokenDescription, fontSize = (size * 0.62f).sp, textAlign = TextAlign.Center, color = Color(0xFF616161))
}
