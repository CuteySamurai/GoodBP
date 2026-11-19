package com.goodbp.data

import android.content.Context
import com.goodbp.rules.BpRange
import com.goodbp.rules.BpRules

/** Family-set configuration. SharedPreferences for the skeleton; move to DataStore later. */
class Settings(context: Context) {
    private val p = context.getSharedPreferences("goodbp", Context.MODE_PRIVATE)

    var language: AppLanguage
        get() = AppLanguage.valueOf(p.getString("lang", AppLanguage.ZH.name)!!)
        set(v) = p.edit().putString("lang", v.name).apply()

    var age: Int
        get() = p.getInt("age", 75)
        set(v) = p.edit().putInt("age", v).apply()

    var name: String
        get() = p.getString("name", "")!!
        set(v) = p.edit().putString("name", v).apply()

    /** Body weight in kg; 0 = not set. */
    var weightKg: Float
        get() = p.getFloat("weight", 0f)
        set(v) = p.edit().putFloat("weight", v).apply()

    var remindersOn: Boolean
        get() = p.getBoolean("reminders_on", true)
        set(v) = p.edit().putBoolean("reminders_on", v).apply()

    /** Reminder times as minutes after midnight, e.g. 8:00 -> 480. */
    var bpTimes: List<Int>
        get() = p.getString("bp_times", "480,780,1200")!!.split(",").mapNotNull { it.toIntOrNull() }
        set(v) = p.edit().putString("bp_times", v.joinToString(",")).apply()

    /** Doctor's range overrides the age default once entered. */
    fun range(): BpRange {
        if (!p.contains("doc_sys_low")) return BpRules.defaultRangeFor(age)
        return BpRange(
            p.getInt("doc_sys_low", 0), p.getInt("doc_sys_high", 0),
            p.getInt("doc_dia_low", 0), p.getInt("doc_dia_high", 0),
            BpRange.Source.DOCTOR,
        )
    }

    fun setDoctorRange(sysLow: Int, sysHigh: Int, diaLow: Int, diaHigh: Int) = p.edit()
        .putInt("doc_sys_low", sysLow).putInt("doc_sys_high", sysHigh)
        .putInt("doc_dia_low", diaLow).putInt("doc_dia_high", diaHigh).apply()
}

/** Mandarin first. Add a language by adding an entry, a values-xx folder and its clips. */
enum class AppLanguage(val tag: String, val speechTag: String) {
    ZH("zh", "zh-CN"),
    EN("en", "en-US"),
    ID("in", "id-ID"),
}
