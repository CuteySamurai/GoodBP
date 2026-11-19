package com.goodbp.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goodbp.data.Settings

/** Alarms are cleared on reboot and app update, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Settings(context).let { if (it.remindersOn) ReminderScheduler.scheduleBp(context, it.bpTimes) }
        // TODO (meds step): reschedule medicine reminders too.
    }
}
