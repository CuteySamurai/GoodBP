package com.goodbp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.setup.SettingsTab
import com.goodbp.ui.home.HomeTab
import com.goodbp.ui.info.InfoTab
import com.goodbp.ui.trend.TrendTab

/** Four big bottom tabs: Home · Trend · Info · Settings. */
@Composable
fun MainScaffold(onPhoto: () -> Unit, onManual: () -> Unit, onLanguageChanged: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        "🏠" to R.string.tab_home, "📈" to R.string.tab_trend,
        "ℹ️" to R.string.tab_info, "⚙️" to R.string.tab_settings,
    )
    Scaffold(bottomBar = {
        NavigationBar(Modifier.height(96.dp)) {
            tabs.forEachIndexed { i, (icon, label) ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Text(icon, fontSize = 28.sp) },
                    label = { Text(stringResource(label), fontSize = 16.sp) },
                )
            }
        }
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeTab(onPhoto, onManual)
                1 -> TrendTab()
                2 -> InfoTab()
                else -> SettingsTab(onLanguageChanged)
            }
        }
    }
}
