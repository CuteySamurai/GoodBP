package com.goodbp

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.goodbp.data.Settings
import java.util.Locale

/**
 * The app language is chosen in Settings, independent of the phone's language.
 * Some phones (and Compose itself) hand the system configuration back to the UI,
 * which undid the old attachBaseContext-only override, so we also pin it in Compose.
 */
object AppLocale {
    fun locale(context: Context): Locale = Locale.forLanguageTag(Settings(context).language.tag)

    fun configFor(context: Context): Configuration =
        Configuration(context.resources.configuration).apply { setLocale(locale(context)) }

    /** For attachBaseContext of the Application and Activities (notifications, receivers). */
    fun wrap(base: Context): Context {
        val loc = locale(base)
        Locale.setDefault(loc)
        return base.createConfigurationContext(configFor(base))
    }
}

/** Keeps the activity (for launchers, services) but answers with resources in the app language. */
private class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = res
}

@Composable
fun AppLanguage(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val tag = Settings(ctx).language.tag
    val config = remember(tag) { AppLocale.configFor(ctx) }
    val localized = remember(tag) { LocalizedContext(ctx, ctx.createConfigurationContext(config).resources) }
    CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) { content() }
}
