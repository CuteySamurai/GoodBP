package com.goodbp

import com.goodbp.meds.MedSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MedScheduleTest {
    @Test fun parseAndFormat() {
        assertEquals(listOf(480, 1200), MedSchedule.parse("20:00, 08:00,08:00,bad,25:00"))
        assertEquals("08:00,20:30", MedSchedule.format(listOf(1230, 480)))
        assertEquals("07:05", MedSchedule.hhmm(425))
    }

    @Test fun lateNightDoseStaysOnItsDay() {
        val now = Calendar.getInstance().apply { set(2026, 10, 21, 0, 10, 0); set(Calendar.MILLISECOND, 0) }
        val due = Calendar.getInstance().apply { timeInMillis = MedSchedule.doseAt(23 * 60 + 30, now) }
        assertEquals(20, due.get(Calendar.DAY_OF_MONTH))
        val morning = Calendar.getInstance().apply { timeInMillis = MedSchedule.doseAt(8 * 60, now) }
        assertEquals(20, morning.get(Calendar.DAY_OF_MONTH)) // 08:00 today is still 8h away
        val early = Calendar.getInstance().apply { set(2026, 10, 21, 7, 30, 0) }
        assertEquals(21, Calendar.getInstance().apply { timeInMillis = MedSchedule.doseAt(8 * 60, early) }.get(Calendar.DAY_OF_MONTH))
    }
}
