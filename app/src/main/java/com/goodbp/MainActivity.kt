package com.goodbp

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.goodbp.data.Settings
import com.goodbp.ui.flow.ReadingScreen
import com.goodbp.ui.main.MainScaffold
import com.goodbp.ui.theme.GoodBpTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Opened from a reminder: go straight to a reading, once (not again on returning home). */
    private var pendingStart = false

    /** App language is set by the family, independent of the phone's language. */
    override fun attachBaseContext(base: Context) {
        val tag = Settings(base).language.tag
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val alerts = (application as GoodBpApp).alerts
        pendingStart = savedInstanceState == null && intent.getBooleanExtra(EXTRA_START_READING, false)
        setContent {
            GoodBpTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "main") {
                    composable("main") {
                        LaunchedEffect(Unit) { if (pendingStart) { pendingStart = false; nav.navigate("reading/photo") } }
                        MainScaffold(
                            onPhoto = { nav.navigate("reading/photo") },
                            onManual = { nav.navigate("reading/manual") },
                            onLanguageChanged = { recreate() },
                        )
                    }
                    composable("reading/{mode}") { entry ->
                        ReadingScreen(alerts, startManual = entry.arguments?.getString("mode") == "manual",
                            onFinished = { nav.popBackStack("main", false) })
                    }
                }
            }
        }
    }

    companion object { const val EXTRA_START_READING = "start_reading" }
}
