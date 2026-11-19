package com.goodbp

import com.goodbp.meds.MedSchedule
import org.junit.Assert.assertEquals
import org.junit.Test

class MedScheduleTest {
    @Test fun parseAndFormat() {
        assertEquals(listOf(480, 1200), MedSchedule.parse("20:00, 08:00,08:00,bad,25:00"))
        assertEquals("08:00,20:30", MedSchedule.format(listOf(1230, 480)))
        assertEquals("07:05", MedSchedule.hhmm(425))
    }
}
