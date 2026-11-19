package com.goodbp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodbp.R
import com.goodbp.setup.SettingsTab
import com.goodbp.ui.home.HomeTab
import com.goodbp.ui.info.InfoTab
import com.goodbp.ui.trend.TrendTab

/**
 * Four big bottom tabs: Home · Trend · Info · Settings.
 * Custom bar (not NavigationBar) so it can be tall with large icons and labels,
 * and it sits above the phone's own gesture/nav bar instead of under it.
 */
@Composable
fun MainScaffold(onPhoto: () -> Unit, onManual: () -> Unit, onLanguageChanged: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        Icons.Filled.Home to R.string.tab_home, Icons.Filled.Timeline to R.string.tab_trend,
        Icons.Filled.Info to R.string.tab_info, Icons.Filled.Settings to R.string.tab_settings,
    )
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> HomeTab(onPhoto, onManual)
                1 -> TrendTab()
                2 -> InfoTab()
                else -> SettingsTab(onLanguageChanged)
            }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF7F9FC)).navigationBarsPadding().height(92.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEachIndexed { i, (icon, label) ->
                TabItem(icon, stringResource(label), tab == i, Modifier.weight(1f)) { tab = i }
            }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, m: Modifier, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF5F6368)
    Column(
        m.fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(34.dp))
        Text(label, fontSize = 16.sp, color = color, maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
