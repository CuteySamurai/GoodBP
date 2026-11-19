package com.goodbp.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.goodbp.R

/** Alarm fired: show a full-screen reminder that speaks, then schedule tomorrow's. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(raw: Context, intent: Intent) {
        val context = com.goodbp.AppLocale.wrap(raw)
        val kind = ReminderScheduler.Kind.valueOf(intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: "BP")
        val minutes = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, 480)
        val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, 0)

        val isTest = intent.getBooleanExtra(ReminderScheduler.EXTRA_TEST, false)
        if (!isTest) ReminderScheduler.schedule(context, kind, minutes, medId) // tomorrow

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH)
        )
        val code = if (isTest) 999_999 else ReminderScheduler.requestCode(kind, minutes, medId)
        val full = PendingIntent.getActivity(
            context, code,
            Intent(context, ReminderActivity::class.java)
                .putExtras(intent)
                .putExtra(EXTRA_NOTIFICATION_ID, code)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = context.getString(if (kind == ReminderScheduler.Kind.BP) R.string.say_reminder_bp else R.string.say_reminder_med)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(full, true)
            .setOngoing(true)
            .build()
        nm.notify(code, n)
    }

    companion object {
        const val CHANNEL = "reminders"
        const val NOTIFICATION_ID = 1
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
