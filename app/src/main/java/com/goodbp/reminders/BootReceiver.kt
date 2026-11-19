package com.goodbp.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goodbp.data.Settings
import com.goodbp.meds.MedSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are cleared on reboot and app update, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Settings(context).let { if (it.remindersOn) ReminderScheduler.scheduleBp(context, it.bpTimes) }
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try { MedSchedule.scheduleAll(app) } finally { pending.finish() }
        }
    }
}
