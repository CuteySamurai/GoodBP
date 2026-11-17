package com.goodbp.meds

import android.content.Context
import com.goodbp.data.Medicine
import com.goodbp.reminders.ReminderScheduler

/**
 * Medicine reminders (simple form). Family sets name, pill photo, spoken description, times.
 * She confirms by voice (吃了); the reminder stops on confirm to avoid a double dose.
 * Records say "confirmed by voice" / "photo taken", never "medicine taken".
 * No pill identification and no dose advice: the family judges the photo.
 * TODO (meds step): family setup screen, 吃了 recogniser, pill photo capture.
 */
object MedicineReminders {
    fun schedule(context: Context, med: Medicine) {
        med.timesOfDay.split(",").mapNotNull { parseHhMm(it) }.forEach {
            ReminderScheduler.schedule(context, ReminderScheduler.Kind.MED, it, med.id)
        }
    }

    fun parseHhMm(s: String): Int? {
        val (h, m) = s.trim().split(":").takeIf { it.size == 2 } ?: return null
        val hh = h.toIntOrNull() ?: return null
        val mm = m.toIntOrNull() ?: return null
        return if (hh in 0..23 && mm in 0..59) hh * 60 + mm else null
    }
}
