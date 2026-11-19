package com.goodbp

import android.app.Application
import com.goodbp.data.Settings
import com.goodbp.family.FamilyAlerts
import com.goodbp.family.NoOpFamilyAlerts
import com.goodbp.meds.MedSchedule
import com.goodbp.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoodBpApp : Application() {
    /** Swapped for the Firebase implementation in the family-alerts step. */
    val alerts: FamilyAlerts = NoOpFamilyAlerts

    override fun onCreate() {
        super.onCreate()
        Settings(this).let { if (it.remindersOn) ReminderScheduler.scheduleBp(this, it.bpTimes) }
        CoroutineScope(Dispatchers.IO).launch { runCatching { MedSchedule.scheduleAll(this@GoodBpApp) } }
    }
}
