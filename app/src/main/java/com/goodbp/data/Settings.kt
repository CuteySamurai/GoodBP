package com.goodbp.data

import android.content.Context
import com.goodbp.rules.BpRange
import com.goodbp.rules.BpRules

/** Family-set configuration. SharedPreferences for the skeleton; move to DataStore later. */
class Settings(context: Context) {
    private val app: Context = context.applicationContext ?: context // null inside Application.attachBaseContext
    private val p = context.getSharedPreferences("goodbp", Context.MODE_PRIVATE)

    /**
     * App language. Chosen once from the phone's language on a fresh install, then
     * only changed in Settings. Existing installs that never picked one stay Mandarin.
     */
    var language: AppLanguage
        get() {
            p.getString("lang", null)?.let { saved ->
                return runCatching { AppLanguage.valueOf(saved) }.getOrDefault(AppLanguage.ZH)
            }
            val firstRun = p.all.isEmpty() && isFreshInstall()
            val picked = if (firstRun) AppLanguage.fromPhone(phoneLanguage()) else AppLanguage.ZH
            p.edit().putString("lang", picked.name).apply()
            return picked
        }
        set(v) = p.edit().putString("lang", v.name).apply()

    /** True on a new install, false after an app update (so current users keep Mandarin). */
    private fun isFreshInstall(): Boolean = runCatching {
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        info.firstInstallTime == info.lastUpdateTime
    }.getOrDefault(false)

    private fun phoneLanguage(): String =
        android.content.res.Resources.getSystem().configuration.locales[0]?.language ?: ""

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

    /** Height in cm; 0 = not set. */
    var heightCm: Int
        get() = p.getInt("height", 0)
        set(v) = p.edit().putInt("height", v).apply()

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
    ID("in", "id-ID");

    companion object {
        /** Phone language code -> app language. Unknown languages fall back to Mandarin. */
        fun fromPhone(code: String): AppLanguage = when (code.lowercase()) {
            "zh" -> ZH
            "id", "in" -> ID
            "en" -> EN
            else -> ZH
        }
    }
}
