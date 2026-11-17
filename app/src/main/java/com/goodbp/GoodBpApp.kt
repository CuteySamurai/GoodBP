package com.goodbp

import android.app.Application
import com.goodbp.data.Settings
import com.goodbp.family.FamilyAlerts
import com.goodbp.family.NoOpFamilyAlerts
import com.goodbp.reminders.ReminderScheduler

class GoodBpApp : Application() {
    /** Swapped for the Firebase implementation in the family-alerts step. */
    val alerts: FamilyAlerts = NoOpFamilyAlerts

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.scheduleBp(this, Settings(this).bpTimes)
    }
}
