package com.goodbp.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Exact alarms at the doctor's times. setExactAndAllowWhileIdle fires in Doze.
 * Each alarm schedules the next day's alarm when it fires (exact alarms don't repeat).
 * On Oppo/ColorOS this still needs auto-launch + no battery optimisation: see OemSetupGuide.
 */
object ReminderScheduler {

    const val EXTRA_KIND = "kind"          // BP or MED
    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_MED_ID = "med_id"

    fun scheduleBp(context: Context, minutesOfDay: List<Int>) =
        minutesOfDay.forEach { schedule(context, Kind.BP, it, medId = 0) }

    fun schedule(context: Context, kind: Kind, minutesOfDay: Int, medId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, kind, minutesOfDay, medId)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(minutesOfDay), pi)
    }

    fun nextTrigger(minutesOfDay: Int, now: Calendar = Calendar.getInstance()): Long {
        val c = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (!c.after(now)) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun pendingIntent(context: Context, kind: Kind, minutes: Int, medId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_MINUTES, minutes)
            .putExtra(EXTRA_MED_ID, medId)
        val requestCode = (kind.ordinal * 100_000 + medId.toInt() * 1440 + minutes)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    enum class Kind { BP, MED }
}
