package com.goodbp.meds

import android.content.Context
import com.goodbp.data.GoodBpDatabase
import com.goodbp.data.MedConfirmation
import com.goodbp.data.Medicine
import com.goodbp.reminders.ReminderScheduler
import java.util.Calendar

/**
 * Medicine times and alarms. Wording rule from the brief: we store that she
 * CONFIRMED (pressed the button), never that the medicine was taken.
 */
object MedSchedule {
    const val CONFIRMED = "CONFIRMED"

    /** "08:00,20:00" -> [480, 1200] */
    fun parse(times: String): List<Int> = times.split(",").mapNotNull { t ->
        val p = t.trim().split(":")
        if (p.size != 2) null else {
            val h = p[0].toIntOrNull(); val m = p[1].toIntOrNull()
            if (h == null || m == null || h !in 0..23 || m !in 0..59) null else h * 60 + m
        }
    }.distinct().sorted()

    fun format(minutes: List<Int>): String = minutes.distinct().sorted().joinToString(",") { "%02d:%02d".format(it / 60, it % 60) }

    fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    fun schedule(context: Context, med: Medicine) =
        parse(med.timesOfDay).forEach { ReminderScheduler.schedule(context, ReminderScheduler.Kind.MED, it, med.id) }

    fun cancel(context: Context, med: Medicine) =
        parse(med.timesOfDay).forEach { ReminderScheduler.cancel(context, ReminderScheduler.Kind.MED, it, med.id) }

    /** After boot / app start / app update: set every medicine alarm again. */
    suspend fun scheduleAll(context: Context) =
        GoodBpDatabase.get(context).medicines().allOnce().forEach { schedule(context, it) }

    /** Today's date at this minute of day. */
    fun todayAt(minutes: Int, now: Calendar = Calendar.getInstance()): Long = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * The dose a reminder belongs to: the latest time at this minute that is not more
     * than an hour in the future. A 23:30 reminder pressed at 00:10 stays yesterday's dose.
     */
    fun doseAt(minutes: Int, now: Calendar = Calendar.getInstance()): Long {
        val t = todayAt(minutes, now)
        if (t <= now.timeInMillis + 60 * 60_000L) return t
        return (now.clone() as Calendar).apply {
            timeInMillis = t; add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
    }

    /** All medicines due at this minute of the day (several can share a time). */
    suspend fun dueAt(context: Context, minutes: Int): List<Medicine> =
        GoodBpDatabase.get(context).medicines().allOnce().filter { minutes in parse(it.timesOfDay) }

    /** She pressed "I took it" for this dose. Recorded once per dose. */
    suspend fun confirm(context: Context, medId: Long, dueAt: Long) {
        val dao = GoodBpDatabase.get(context).medicines()
        if (dao.countFor(medId, dueAt) == 0)
            dao.insertConfirmation(MedConfirmation(medicineId = medId, dueAt = dueAt, confirmedAt = System.currentTimeMillis(), kind = CONFIRMED))
    }
}
