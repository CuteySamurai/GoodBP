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
import com.goodbp.setup.FamilySetupScreen
import com.goodbp.ui.flow.ReadingScreen
import com.goodbp.ui.history.HistoryScreen
import com.goodbp.ui.home.HomeScreen
import com.goodbp.ui.theme.GoodBpTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

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
        val startReading = intent.getBooleanExtra(EXTRA_START_READING, false)
        setContent {
            GoodBpTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        LaunchedEffect(Unit) { if (startReading) nav.navigate("reading") }
                        HomeScreen(onStart = { nav.navigate("reading") }, onFamilyLongPress = { nav.navigate("family") })
                    }
                    composable("reading") { ReadingScreen(alerts, onFinished = { nav.popBackStack("home", false) }) }
                    composable("family") { FamilySetupScreen(onHistory = { nav.navigate("history") }) }
                    composable("history") { HistoryScreen() }
                }
            }
        }
    }

    companion object { const val EXTRA_START_READING = "start_reading" }
}
