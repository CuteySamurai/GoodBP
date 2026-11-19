package com.goodbp.reminders

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import com.goodbp.data.GoodBpDatabase
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
 * Over the lock screen. Repeats the spoken reminder until she taps the big button.
 * BP: opens the reading screen. MED: shows the pill photo, says the medicine's name,
 * and records that she confirmed (never that she "took" it).
 * TODO (family alerts step): if no response after N repeats, alert family.
 */
class ReminderActivity : ComponentActivity() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.goodbp.AppLocale.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isBp = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) != ReminderScheduler.Kind.MED.name
        val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, 0)
        val minutes = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, 0)
        val notificationId = intent.getIntExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, ReminderReceiver.NOTIFICATION_ID)
        val speaker = Speaker(this, Settings(this).language)
        fun dismiss() {
            getSystemService(NotificationManager::class.java).cancel(notificationId)
            finish()
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
                            android.content.Intent(this, com.goodbp.MainActivity::class.java)
                                .putExtra(com.goodbp.MainActivity.EXTRA_START_READING, true)
                        )
                        dismiss()
                    })
                } else {
                    var med by remember { mutableStateOf<Medicine?>(null) }
                    LaunchedEffect(Unit) { med = GoodBpDatabase.get(this@ReminderActivity).medicines().get(medId) }
                    val generic = stringResource(R.string.say_reminder_med)
                    val m = med
                    val prompt = if (m == null) generic else stringResource(R.string.say_med_named, m.name, m.spokenDescription)
                    val thanks = stringResource(R.string.say_med_confirmed)
                    LaunchedEffect(prompt) { repeat(REPEATS) { speaker.say(prompt); delay(REPEAT_GAP_MS) } }
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MedPhoto(m?.pillPhotoPath, 200.dp)
                            Text(MedSchedule.hhmm(minutes), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                            if (m != null) Text(m.name, fontSize = 32.sp, textAlign = TextAlign.Center)
                            if (m != null && m.spokenDescription.isNotBlank())
                                Text(m.spokenDescription, fontSize = 20.sp, textAlign = TextAlign.Center, color = Color(0xFF616161))
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        if (m != null) MedSchedule.confirm(this@ReminderActivity, m.id, MedSchedule.todayAt(minutes))
                                        speaker.say(thanks)
                                        delay(1500)
                                        dismiss()
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                                modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f),
                            ) { Text("💊\n" + stringResource(R.string.took_medicine), fontSize = 40.sp, textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
          }
        }
    }

    companion object {
        const val REPEATS = 10
        const val REPEAT_GAP_MS = 30_000L
    }
}
