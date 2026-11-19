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
    const val EXTRA_TEST = "test"
    private const val TEST_REQUEST = 999_999

    fun scheduleBp(context: Context, minutesOfDay: List<Int>) =
        minutesOfDay.forEach { schedule(context, Kind.BP, it, medId = 0) }

    /** Family changed the times: cancel the old alarms, set the new ones. */
    fun replaceBp(context: Context, old: List<Int>, new: List<Int>) {
        (old - new.toSet()).forEach { cancel(context, Kind.BP, it, medId = 0) }
        scheduleBp(context, new)
    }

    fun cancel(context: Context, kind: Kind, minutesOfDay: Int, medId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pendingIntent(context, kind, minutesOfDay, medId))
    }

    /** One-off test alarm, so the family can check it fires with the screen off. */
    fun scheduleTest(context: Context, inSeconds: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, Kind.BP.name)
            .putExtra(EXTRA_TEST, true)
        val pi = PendingIntent.getBroadcast(context, TEST_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + inSeconds * 1000L, pi)
    }

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
        val requestCode = requestCode(kind, minutes, medId)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** One code per (kind, medicine, time), so alarms and their screens never overwrite each other. */
    fun requestCode(kind: Kind, minutes: Int, medId: Long): Int = kind.ordinal * 10_000_000 + medId.toInt() * 1440 + minutes

    enum class Kind { BP, MED }
}
