package com.goodbp.reminders

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.goodbp.R
import com.goodbp.data.Settings
import com.goodbp.speech.Speaker
import com.goodbp.ui.home.BigButtonScreen
import com.goodbp.ui.theme.GoodBpTheme
import kotlinx.coroutines.delay

/**
 * Over the lock screen. Repeats the spoken reminder until she taps the big button.
 * TODO (family alerts step): if no response after N repeats, alert family.
 * TODO (meds step): MED kind -> 吃了 voice confirm + optional pill photo.
 */
class ReminderActivity : ComponentActivity() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.goodbp.AppLocale.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isBp = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) != ReminderScheduler.Kind.MED.name
        val speaker = Speaker(this, Settings(this).language)
        setContent {
          com.goodbp.AppLanguage {
            GoodBpTheme {
                val prompt = stringResource(if (isBp) R.string.say_reminder_bp else R.string.say_reminder_med)
                val key = if (isBp) "say_reminder_bp" else "say_reminder_med"
                LaunchedEffect(Unit) {
                    repeat(REPEATS) { speaker.sayPrompt(key, prompt); delay(REPEAT_GAP_MS) }
                }
                DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
                BigButtonScreen(
                    label = stringResource(if (isBp) R.string.home_button else R.string.took_medicine),
                    onPress = {
                        getSystemService(NotificationManager::class.java).cancel(ReminderReceiver.NOTIFICATION_ID)
                        if (isBp) startActivity(
                            android.content.Intent(this, com.goodbp.MainActivity::class.java)
                                .putExtra(com.goodbp.MainActivity.EXTRA_START_READING, true)
                        )
                        finish()
                    },
                )
            }
          }
        }
    }

    companion object {
        const val REPEATS = 10
        const val REPEAT_GAP_MS = 30_000L
    }
}
