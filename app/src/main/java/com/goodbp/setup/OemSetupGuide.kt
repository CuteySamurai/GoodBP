package com.goodbp.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Family-run setup step. Oppo/ColorOS (and Realme/OnePlus, same base) kill background
 * apps, which silences reminders. We open the right settings pages and the family
 * switches on auto-launch and turns off battery optimisation.
 * Component names vary by ColorOS version; each is tried and the first that opens wins.
 */
object OemSetupGuide {

    val isOppoFamily: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("oppo", "realme", "oneplus")

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun requestIgnoreBatteryOptimizations(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private val oppoAutoStart = listOf(
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
    )

    /** Returns false if no known page opened; then show the manual steps instead. */
    fun openAutoStartSettings(context: Context): Boolean {
        for (c in oppoAutoStart) {
            val i = Intent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(i) }.isSuccess) return true
        }
        // Fallback: app details page, where ColorOS shows "Allow auto launch".
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return false
    }

    /** Manual steps for the family (shown in the setup screen). */
    const val MANUAL_STEPS_ZH = """
1. 设置 → 应用管理 → 量血压 → 允许自动启动
2. 设置 → 电池 → 量血压 → 不优化 / 允许后台运行
3. 最近任务里把量血压“锁住”（下拉或点锁图标）
4. 通知：允许全部通知、锁屏显示、悬浮通知
"""
}
